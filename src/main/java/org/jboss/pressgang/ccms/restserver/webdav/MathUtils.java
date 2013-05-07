package org.jboss.pressgang.ccms.restserver.webdav;

/**
 * Created with IntelliJ IDEA.
 * User: matthew
 * Date: 5/8/13
 * Time: 6:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class MathUtils {
    public static int getScale(final int number) {
        int maxScale = Math.abs(number);

        /* find out how large is the largest (or smallest) topic id, logarithmicly speaking */
        int zeros = 0;
        while (maxScale > 0) {
            maxScale = maxScale / 10;
            ++zeros;
        }

        return zeros;
    }
}
