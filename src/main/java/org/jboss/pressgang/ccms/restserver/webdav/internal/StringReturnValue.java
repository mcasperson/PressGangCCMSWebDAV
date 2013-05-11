package org.jboss.pressgang.ccms.restserver.webdav.internal;

import javax.annotation.Nullable;

/**
 * A wrapper that contains the HTTP return code, and the returned string.
 */
public class StringReturnValue {
    private final int statusCode;
    @Nullable
    private final String value;

    public StringReturnValue(final int statusCode, @Nullable final String value) {
        this.statusCode = statusCode;
        this.value = value;
    }

    public StringReturnValue(final int statusCode) {
        this.statusCode = statusCode;
        this.value = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Nullable
    public String getValue() {
        return value;
    }
}
