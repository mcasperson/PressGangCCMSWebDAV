package org.jboss.pressgang.ccms.restserver.webdav;

import net.java.dev.webdav.jaxrs.xml.elements.MultiStatus;
import net.java.dev.webdav.jaxrs.xml.elements.Response;
import org.jboss.pressgang.ccms.restserver.webdav.internal.InternalResource;
import org.jboss.pressgang.ccms.restserver.webdav.internal.MultiStatusReturnValue;
import org.jboss.pressgang.ccms.restserver.webdav.managers.DeleteManager;
import org.jboss.pressgang.ccms.restserver.webdav.topics.InternalResourceTopicVirtualFolder;

import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * The root folder of the WebDAV hierarchy.
 */
public class InternalResourceRoot extends InternalResource {
    public InternalResourceRoot(final UriInfo uriInfo, final String stringId) {
        super(uriInfo, stringId);
    }

    @Override
    public MultiStatusReturnValue propfind(final DeleteManager deleteManager, final int depth) {

        if (getUriInfo() == null) {
            throw new IllegalStateException("Can not perform propfind without uriInfo");
        }

        if (depth == 0) {
            /* A depth of zero means we are returning information about this item only */
            final Response folder = getFolderProperties(getUriInfo());

            return new MultiStatusReturnValue(207, new MultiStatus(folder));
        } else {
            /* Otherwise we are retuning info on the children in this collection */
            final List<Response> responses = new ArrayList<Response>();

            /* The topic collection */
            responses.add(getFolderProperties(getUriInfo(), InternalResourceTopicVirtualFolder.RESOURCE_NAME));

            final MultiStatus st = new MultiStatus(responses.toArray(new Response[responses.size()]));
            return new MultiStatusReturnValue(207, st);
        }

    }
}
