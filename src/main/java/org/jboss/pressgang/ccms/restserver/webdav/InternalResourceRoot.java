package org.jboss.pressgang.ccms.restserver.webdav;

import net.java.dev.webdav.jaxrs.xml.elements.MultiStatus;
import net.java.dev.webdav.jaxrs.xml.elements.Response;
import org.jboss.pressgang.ccms.restserver.webdav.internal.InternalResource;
import org.jboss.pressgang.ccms.restserver.webdav.internal.MultiStatusReturnValue;
import org.jboss.pressgang.ccms.restserver.webdav.managers.DeleteManager;
import org.jboss.pressgang.ccms.restserver.webdav.topics.InternalResourceTopicVirtualFolder;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: matthew
 * Date: 11/05/13
 * Time: 5:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class InternalResourceRoot extends InternalResource {
    public InternalResourceRoot(final UriBuilder requestUriBuilder, final String stringId) {
        super(requestUriBuilder, stringId);
    }

    @Override
    public MultiStatusReturnValue propfind(final DeleteManager deleteManager, final int depth) {
        if (depth == 0) {
            /* A depth of zero means we are returning information about this item only */
            final Response folder = getFolderProperties(uriInfo);

            return new MultiStatusReturnValue(207, new MultiStatus(folder));
        } else {
            /* Otherwise we are retuning info on the children in this collection */
            final List<Response> responses = new ArrayList<Response>();

            /* The topic collection */
            responses.add(getFolderProperties(uriInfo, InternalResourceTopicVirtualFolder.RESOURCE_NAME));

            final MultiStatus st = new MultiStatus(responses.toArray(new Response[responses.size()]));
            return new MultiStatusReturnValue(207, st);
        }

    }
}
