package tr24.utils.swt;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import tr24.utils.common.ThreadUtil;
import tr24.utils.annotations.Nullable;
import tr24.utils.scheduler.IScheduleClient;
import tr24.utils.scheduler.IScheduleContext;
import tr24.utils.scheduler.SchedulerService;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Speed-Filter-ing for SWT-update calls
 *  Idea:
 *   - everybody can request updates at any time (and not in SWT-Threads)
 *     - all requests are collected by category
 *       and only the last one is remembered
 *   - an internal 2x/sec loop triggers SWT-update calls
 *     WHEN there is an update-request for this category
 *
 *   - SWT-paint-Handling works a little different...
 *
 */
public class SwtCentral {


    final Display display;
    private long lastSwtRun;

    /**
     * Example: a SWT-label that can be updated
     * @param <T> what type of payload, e.g. a String
     */
    public interface IUpdateableGuiElement<T> {
        /**
         * call IN SWT: e.g. Label calls setText(payload)
         */
        void runUpdate(T payload);
    }

    /**
     * for the two-way paint()-Handshake:
     *  - User code registers some code
     */
    public interface IReDrawDoneApi<T> {
        void onPaintDone(int ackNum);
    }


    public interface IReDrawableElement {
        /**
         * code must call e.g. Canvas.redraw()
         * and SEND BACK the #ackNum !
         */
        void runReDraw(int ackNum, IReDrawDoneApi callback);
    }

    /**
     * each #register() returns this
     */
    public interface ITriggerApi<T> {
        void triggerUpdate(T payload);
    }

    /** Ctor ----------------------------------------------- */
    public SwtCentral(SchedulerService scheduler, Display display) {
        this.display = display;
        scheduler.registerClient(new IScheduleClient() {
            @Override
            public void onSchedulePing(IScheduleContext ctx) {
                runSwtUpdates();
            }
        }, 250);
    }

    /* ==================== register stuff;  return is a int-handle, for calling swtCentral.update(handle, payload) ====== */
    /**
     * Register an SWT-text-label for update
     */
    public ITriggerApi<String> registerLabel(Label textLabel) {
        return new TriggerApiImpl<String>(null, textLabel, null);
    }

    /**
     * Register a code-snipplet that handles the SWT-update itself, e.g. a Button that has to update other stuff too
     */
    public <T> ITriggerApi<T> register(IUpdateableGuiElement<T> callback) {
        return new TriggerApiImpl<T>(callback, null, null);
    }

    /**
     * Register an Canvas-Element that reacts to {@link org.eclipse.swt.events.PaintListener#paintControl}
     */
    public ITriggerApi<Void>  registerRedrawableGuiElement(IReDrawableElement element) {
        return new TriggerApiImpl<Void>(null, null, element);
    }
/* ==================== register stuff;  return is a int-handle, for calling swtCentral.update(handle, payload) ====== */

    private final AtomicInteger nextId = new AtomicInteger(1);
    /**
     * maintain a list of elements that currently want to be updated, used SYNC'ed Hashtable!
     */
    private final Hashtable<Integer, TriggerApiImpl> activeUpdates = new Hashtable<>();

    /**
     * HERE, all client "update this, update that" calls arrive
     * - "filtering": last update-payload wins
     */
    class TriggerApiImpl<T> implements ITriggerApi<T>, IReDrawDoneApi {
        final int myId;
        @Nullable
        final IUpdateableGuiElement<T> callbackThisCodeInSWT;
        @Nullable final Label textLabel;
        @Nullable final IReDrawableElement canvasElement;
        int nextAckNum = -1;    // (-1) not set => a new trigger will "go through"; (>0) wait for ackn
        /**
         * all SWT-code IF this is set
         */
        T latestUpdateObj = null;

        /**
         * generic ctor:
         *
         * @param textLabel - if (!=null) => call label.setText IN SWT, payload must be a STRING then
         */
        public TriggerApiImpl(IUpdateableGuiElement<T> callbackThisCodeInSWT, Label textLabel, IReDrawableElement canvasElement) {
            this.myId = nextId.getAndIncrement();
            this.callbackThisCodeInSWT = callbackThisCodeInSWT;
            this.textLabel = textLabel;
            this.canvasElement = canvasElement;
        }
        @Override
        public void triggerUpdate(T payload) {
            // switch: Canvas or "normal element" ?
            if (canvasElement!=null) {
                if (nextAckNum > 0) {
                    ThreadUtil.syserr(" ! filter canvas.redraw request !!");
                    return;
                }
                // prepare next call:
                nextAckNum = (-nextAckNum)+1;   // flip from (-1) => (+2), (-2) => (+3), ...
                this.latestUpdateObj = (T) Boolean.TRUE;    // set something
                activeUpdates.put(myId, this);    // make myself known
            } else {
                // Labels and user-Code: Speed-Filter'ing is done by the scheduler-loop
                this.latestUpdateObj = payload;   // no need to do anything else... Scheduler-Run will check for latest-payload
                activeUpdates.put(myId, this);    // make myself known
            }
        }
        /** IReDrawDoneApi */
        @Override
        public void onPaintDone(int ackNum) {
            // number should be the same, but how cares....
            // System.err.println("   --(2)-- redraw() ackn = " + ackNum + " vs this.curAckn="+nextAckNum);
            if (nextAckNum>0) {
                nextAckNum = -nextAckNum;      // reset; now the next #triggerUpdate() will go through
            }
        }
    }


    private List<TriggerApiImpl> buffer = new ArrayList<>(50);
    private List<TriggerApiImpl> callSwt = new ArrayList<>(50);

    private boolean isSwtRunning = false;

    /**
     * ping every 300ms:
     * - call SWT-code for all "clients" where there is a pending update-payload
     *
     * Contract:
     *  - Collect all "stuff" that wants/needs a SWT-update-call
     *  - then goto-SWT and call'em
     *    - UNTIL that CALL is done, do NOT call anything else
     *
     */
    private void runSwtUpdates() {

        // FILTER: do not call swt while swt is running !
        if (isSwtRunning) {
            // System.err.println("SC: skip swt calling");
            return;
        }

        buffer.clear();
        callSwt.clear();    // good: 'callSwt[]' is "locked" because method does not go here as long as is-swt-running
        buffer.addAll(activeUpdates.values());   // snapshot
        for (TriggerApiImpl tai : buffer) {
            // 1) update all elements like Text-Labels and update-by-Code
            //if (tai.callbackThisCodeInSWT!=null || tai.textLabel!=null) {              // just to make sure
            if (tai.latestUpdateObj!=null) {
                callSwt.add(tai);         // a) collect, then bulk-call
                activeUpdates.remove(tai.myId);   // processed for this round
            }
        }

        if (callSwt.size()>0) {
            isSwtRunning = true;      // "block"
            display.asyncExec(()-> {

                /*long now = System.currentTimeMillis();
                long diff = (lastSwtRun>0 ? (now-lastSwtRun) : 0);
                String info = "   --- run SWT, last = " + (diff>0 ? (diff+"ms") : "first") + " elems = " + callSwt.size();
                System.err.println(info);
                lastSwtRun = now; */

                for (TriggerApiImpl tai : callSwt) {
                    if (tai.textLabel!=null) {            // type 1
                        String temp = (String) tai.latestUpdateObj;
                        if (temp!=null) {   // sis
                            String old = tai.textLabel.getText();
                            tai.textLabel.setText(temp);
                            // System.err.println("   ----  update label " + old + " -> " + temp);
                        }
                    } else if (tai.callbackThisCodeInSWT!=null) {   // type 2
                        Object temp = tai.latestUpdateObj;
                        tai.callbackThisCodeInSWT.runUpdate(temp);
                        // System.err.println("   ----  update element " + tai.code);
                    } else if (tai.canvasElement!=null) {
                        // canvas -> redraw() : callback/user-code must trigger the redraw() and I will WAIT until the ackn-Num comes back before calling the next trigger
                        int ack = tai.nextAckNum;
                        if (ack>0) {   // sis
                            // System.err.println("   --(1)-- call canvas.repaing , " + ack);
                            tai.canvasElement.runReDraw(ack, tai);    // pass Impl as callback; all next triggers() are BLOCKed until the number is acknowledged
                        }
                    }
                    tai.latestUpdateObj = null;      // reset
                }
                // swt done
                isSwtRunning = false;
            });
        }


    }


}





























