package org.jboss.pressgang.ccms.restserver.webdav;

import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.elements.Response;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static net.java.dev.webdav.jaxrs.Headers.DEPTH;

import javax.ws.rs.core.*;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Providers;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jboss.pressgang.ccms.restserver.webdav.topics.TopicGroupedVirtualFolder;

/**
    The root of the WebDAV server.
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
            LOGGER.info("ENTER WebDavRoot.propfind()");

            if (depth == 0) {
                LOGGER.info("Depth == 0");
                /* A depth of zero means we are returning information about this item only */
                final Response folder = getFolderProperties(uriInfo);

                return javax.ws.rs.core.Response.status(207).entity(new MultiStatus(folder)).type(WebDavConstants.XML_MIME).build();
            } else {
                LOGGER.info("Depth != 0");
                /* Otherwise we are retuning info on the children in this collection */
                final List<Response> responses = new ArrayList<Response>();

                /* The topic collection */
                responses.add(getFolderProperties(uriInfo, TopicGroupedVirtualFolder.RESOURCE_NAME));


                final MultiStatus st = new MultiStatus(responses.toArray(new Response[responses.size()]));
                return javax.ws.rs.core.Response.status(207).entity(st).type(WebDavConstants.XML_MIME).build();
            }

        } catch (final Exception ex) {
            LOGGER.severe(ex.toString());
            ex.printStackTrace();
            return javax.ws.rs.core.Response.status(500).build();
        }
    }
}
