package org.jboss.pressgang.ccms.restserver.webdav.internal;

import net.java.dev.webdav.jaxrs.xml.elements.MultiStatus;

import javax.annotation.Nullable;

/**
 * Created with IntelliJ IDEA.
 * User: matthew
 * Date: 11/05/13
 * Time: 5:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class MultiStatusReturnValue {
    private final int statusCode;
    @Nullable
    private final MultiStatus value;

    public MultiStatusReturnValue(final int statusCode, @Nullable final MultiStatus value) {
        this.statusCode = statusCode;
        this.value = value;
    }
    public MultiStatusReturnValue(final int statusCode) {
        this.statusCode = statusCode;
        this.value = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Nullable
    public MultiStatus getValue() {
        return value;
    }
}
