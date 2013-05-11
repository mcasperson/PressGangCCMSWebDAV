package org.jboss.pressgang.ccms.restserver.webdav;

import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.xml.elements.MultiStatus;
import net.java.dev.webdav.jaxrs.xml.elements.Response;
import org.jboss.pressgang.ccms.restserver.webdav.topics.TopicVirtualFolder;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static net.java.dev.webdav.jaxrs.Headers.DEPTH;

/**
 * The root of the WebDAV server.
 */
@Path("/")
public class WebDavRoot extends WebDavResource {

    private static final Logger LOGGER = Logger.getLogger(WebDavRoot.class.getName());

    @Override
    @Produces(MediaType.APPLICATION_XML)
    @PROPFIND
    public javax.ws.rs.core.Response propfind(@Context final UriInfo uriInfo, @HeaderParam(DEPTH) final int depth,
                                              final InputStream entityStream, @HeaderParam(CONTENT_LENGTH) final long contentLength,
                                              @Context final Providers providers, @Context final HttpHeaders httpHeaders) throws URISyntaxException, IOException {
        try {
            if (depth == 0) {
                /* A depth of zero means we are returning information about this item only */
                final Response folder = getFolderProperties(uriInfo);

                return javax.ws.rs.core.Response.status(207).entity(new MultiStatus(folder)).type(MediaType.TEXT_XML).build();
            } else {
                /* Otherwise we are retuning info on the children in this collection */
                final List<Response> responses = new ArrayList<Response>();

                /* The topic collection */
                responses.add(getFolderProperties(uriInfo, TopicVirtualFolder.RESOURCE_NAME));


                final MultiStatus st = new MultiStatus(responses.toArray(new Response[responses.size()]));
                return javax.ws.rs.core.Response.status(207).entity(st).type(MediaType.TEXT_XML).build();
            }

        } catch (final Exception ex) {
            LOGGER.severe(ex.toString());
            ex.printStackTrace();
            return javax.ws.rs.core.Response.status(500).build();
        }
    }
}
