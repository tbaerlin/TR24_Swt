package tr24.utils.swt.apprunenv;

import tr24.utils.annotations.Nullable;
import tr24.utils.common.ILogger;
import tr24.utils.common.TaskQueue;
import tr24.utils.scheduler.SchedulerService;

import java.util.concurrent.ExecutorService;

/**
 * @param <T> - type of initObject
 */
public interface ISwtApp<T> {

    void initServices(T initObject, @Nullable TaskQueue taskQ, @Nullable ExecutorService pool, @Nullable SchedulerService scheduler, IAppCtx appCtx, ILogger logger);

    void initGui(IAppCtx appCtx, Tr24GuiCore core);

}
