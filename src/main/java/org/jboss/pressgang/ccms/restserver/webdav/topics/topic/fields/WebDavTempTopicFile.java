package org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields;

import net.java.dev.webdav.jaxrs.methods.COPY;
import net.java.dev.webdav.jaxrs.methods.MOVE;
import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.methods.PROPPATCH;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.elements.Response;
import net.java.dev.webdav.jaxrs.xml.properties.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavConstants;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavResource;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavUtils;
import org.jboss.pressgang.ccms.restserver.webdav.internal.InternalResource;
import org.jboss.pressgang.ccms.restserver.webdav.internal.StringReturnValue;
import org.jboss.pressgang.ccms.restserver.webdav.system.FixedCreationDate;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Providers;
import java.io.*;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.logging.Logger;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.Response.Status.OK;
import static net.java.dev.webdav.jaxrs.Headers.DEPTH;
import static net.java.dev.webdav.jaxrs.Headers.DESTINATION;
import static net.java.dev.webdav.jaxrs.Headers.OVERWRITE;

/**

 */
@Path("/TOPICS{var:(/\\d)*}/{topicId:TOPIC\\d*}/{filename:.+}")
public class WebDavTempTopicFile extends WebDavResource {

    private static final Logger LOGGER = Logger.getLogger(WebDavTempTopicFile.class.getName());

    @PathParam("filename")
    private String filename;

    @Override
    @PROPFIND
    public javax.ws.rs.core.Response propfind(@Context final UriInfo uriInfo, @HeaderParam(DEPTH) int depth, final InputStream entityStream, @HeaderParam(CONTENT_LENGTH) final long contentLength,
                                              @Context final Providers providers, @Context final HttpHeaders httpHeaders) throws URISyntaxException, IOException {
        LOGGER.info("ENTER WebDavTopic.propfind()");

        try {
            final String fileLocation = InternalResourceTempTopicFile.buildTempFileName(uriInfo.getPath());

            final File file = new File(fileLocation);

            if (file.exists())  {
                final Response response = getProperties(uriInfo, file, true);
                final MultiStatus st = new MultiStatus(response);
                return javax.ws.rs.core.Response.status(207).entity(st).type(MediaType.TEXT_XML).build();
            }

        } catch (final NumberFormatException ex) {
            return javax.ws.rs.core.Response.status(404).build();
        }

        return javax.ws.rs.core.Response.status(404).build();
    }

    /**
     *
     * @param uriInfo The uri that was used to access this resource
     * @param file The file that this content represents
     * @param local true if we are building the properties for the resource at the given uri, and false if we are building
     *              properties for a child resource.
     * @return
     */
    public static Response getProperties(final UriInfo uriInfo, final File file, final boolean local) {
        final HRef hRef = local ? new HRef(uriInfo.getRequestUri()) : new HRef(uriInfo.getRequestUriBuilder().path(InternalResourceTempTopicFile.buildWebDavFileName(uriInfo.getPath(), file)).build());
        final GetLastModified getLastModified = new GetLastModified(new Date(file.lastModified()));
        final GetContentType getContentType = new GetContentType(MediaType.APPLICATION_OCTET_STREAM);
        final GetContentLength getContentLength = new GetContentLength(file.length());
        final DisplayName displayName = new DisplayName(file.getName());
        final SupportedLock supportedLock = new SupportedLock();
        final LockDiscovery lockDiscovery = new LockDiscovery();
        final Prop prop = new Prop(getLastModified, getContentType, getContentLength, displayName, supportedLock, lockDiscovery);
        final Status status = new Status((javax.ws.rs.core.Response.StatusType) OK);
        final PropStat propStat = new PropStat(prop, status);

        final Response davFile = new Response(hRef, null, null, null, propStat);

        return davFile;
    }


}
