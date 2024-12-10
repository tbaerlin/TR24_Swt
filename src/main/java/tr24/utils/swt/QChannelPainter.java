package tr24.utils.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import tr24.algoutils.trading.Contract;
import tr24.utils.swt.customcanvas.api.CustomChartPainter;
import tr24.utils.trademeasures.QChannelFinderV2;

/**
 * util (so I can separate {@link QChannelFinderV2} from SWT-stuff
 */
public class QChannelPainter {


    /**
     * praktisch: der Channel kann sich selbst in eine CCC einzeichnen
     *
     * @param gc
     * @param painter
     * @param dataLength - wieviele Punkte hat die Linie
     * @param xWidth     - pixel-Breite des Anzeige-Bereichs, idR pixBox.width
     */
    public static void paintChannel(GC gc, CustomChartPainter painter, QChannelFinderV2.QChannel q, int dataLength, int xWidth) {

        gc.setForeground(FARBE.RED_DARK_1);
        gc.setLineWidth(2);
        int n = dataLength; // -1;
        int xp1 = painter.x2pixel(0);
        int yp1 = painter.y2pixel(q.offsetObereLinie);
        int xp2 = painter.x2pixel(n);
        int yp2 = painter.y2pixel(q.ratio * n + q.offsetObereLinie);
        gc.drawLine(xp1, yp1, xp2, yp2);
        int yp3 = painter.y2pixel(q.offsetObereLinie - q.height);
        int yp4 = painter.y2pixel(q.ratio * n + q.offsetObereLinie - q.height);
        gc.drawLine(xp1, yp3, xp2, yp4);
        // zeige die Channel-Mittel-Line
        int yp5 = painter.y2pixel(q.offsetObereLinie - q.height / 2);
        int yp6 = painter.y2pixel(q.ratio * n + q.offsetObereLinie - q.height / 2);
        gc.setLineWidth(1);
        gc.setLineStyle(SWT.LINE_DOT);
        gc.drawLine(xp1, yp5, xp2, yp6);

        // gebe ein paar Daten aus
        String s = "Ch  r=" + Contract.print(q.ratio, 2)
                + "   q = ( cc:" + ((int) (q.ratio * dataLength)) + " / "
                + " h:" + ((int) q.height)
                + " ) = " + ((int) (q.quality * 100)) + "%" /*Contract.print(channel.quality, 2)*/;

    gc.drawString(s, xWidth/2-70, 2);
    }

}
