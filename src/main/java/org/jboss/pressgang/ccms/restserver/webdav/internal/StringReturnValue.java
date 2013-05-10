package org.jboss.pressgang.ccms.restserver.webdav.internal;

import javax.annotation.Nullable;

/**
 * Created with IntelliJ IDEA.
 * User: matthew
 * Date: 5/10/13
 * Time: 1:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class StringReturnValue {
    private final int statusCode;
    @Nullable private final String value;

    public StringReturnValue(final int statusCode, @Nullable final String value) {
        this.statusCode = statusCode;
        this.value = value;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Nullable
    public String getValue() {
        return value;
    }
}
