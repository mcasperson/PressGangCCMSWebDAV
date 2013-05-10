package org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields;

import net.java.dev.webdav.jaxrs.methods.COPY;
import net.java.dev.webdav.jaxrs.methods.MOVE;
import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.methods.PROPPATCH;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.properties.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavConstants;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavResource;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavUtils;
import org.jboss.pressgang.ccms.restserver.webdav.system.FixedCreationDate;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.io.*;
import java.lang.annotation.Annotation;
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

    @Context
    private UriInfo uriInfo;

    @GET
    @Produces("application/octet-stream")
    public javax.ws.rs.core.Response get() {
        LOGGER.info("ENTER WebDavTempTopicFile.get()");

        final String fileLocation = buildTempFileName(uriInfo);

        try {
            final FileInputStream inputStream = new FileInputStream(fileLocation);
            try {
                final String fileContents = IOUtils.toString(inputStream);
                return javax.ws.rs.core.Response.ok().entity(fileContents).build();

            } catch (final Exception ex) {

            } finally {
                try {
                    inputStream.close();
                } catch (final Exception ex) {

                }
            }
        } catch (final FileNotFoundException e) {

        }

        return javax.ws.rs.core.Response.serverError().build();
    }

    @PUT
    @Consumes("*/*")
    public javax.ws.rs.core.Response put(final InputStream entityStream)
            throws IOException, URISyntaxException {
        LOGGER.info("ENTER WebDavTempTopicFile.put()");

        final File directory = new File(WebDavConstants.TEMP_LOCATION);
        final String fileLocation = buildTempFileName(uriInfo);

        if (!directory.exists()) {
            directory.mkdirs();
        } else if (!directory.isDirectory()) {
            directory.delete();
            directory.mkdirs();
        }

        final File file = new File(fileLocation);

        if (!file.exists()) {
            file.createNewFile();
        }

        final StringWriter writer = new StringWriter();
        IOUtils.copy(entityStream, writer, "UTF-8");
        final String newContents = writer.toString();

        FileUtils.writeStringToFile(file, newContents);

        return javax.ws.rs.core.Response.serverError().build();
    }

    /**
     *  A lot of text editors try to move files around during their saving process. This simply tricks them into
     *  believing that they have successfully performed a move.
     */
    @Override
    @MOVE
    public javax.ws.rs.core.Response move(@Context final UriInfo uriInfo, @HeaderParam(OVERWRITE) final String overwriteStr, @HeaderParam(DESTINATION) final String destination) throws URISyntaxException {
        LOGGER.info("ENTER WebDavTempTopicFile.move()");
        LOGGER.info("Source " + uriInfo.getPath().toString());
        LOGGER.info("Destination " + destination);
        return javax.ws.rs.core.Response.ok().build();
    }

    @COPY
    @Override
    public javax.ws.rs.core.Response copy() {
        LOGGER.info("ENTER WebDavTempTopicFile.copy()");
        return javax.ws.rs.core.Response.ok().build();
    }

    /**
     *  A lot of text editors try to delete files around during their saving process. This simply tricks them into
     *  believing that they have successfully performed a delete.
     */
    @Override
    @DELETE
    public javax.ws.rs.core.Response delete() {
        LOGGER.info("ENTER WebDavTempTopicFile.delete()");
        LOGGER.info(uriInfo.getPath().toString());

        final String fileLocation = buildTempFileName(uriInfo);

        final File file = new File(fileLocation);
        if (file.exists()) {
            file.delete();
            return javax.ws.rs.core.Response.ok().build();
        }

        return javax.ws.rs.core.Response.status(404).build();
    }

    @Override
    @PROPFIND
    public javax.ws.rs.core.Response propfind(@Context final UriInfo uriInfo, @HeaderParam(DEPTH) int depth, final InputStream entityStream, @HeaderParam(CONTENT_LENGTH) final long contentLength,
                                              @Context final Providers providers, @Context final HttpHeaders httpHeaders) throws URISyntaxException, IOException {
        LOGGER.info("ENTER WebDavTopic.propfind()");

        try {
            final String fileLocation = buildTempFileName(uriInfo);

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
        final HRef hRef = local ? new HRef(uriInfo.getRequestUri()) : new HRef(uriInfo.getRequestUriBuilder().path(buildWebDavFileName(uriInfo, file)).build());
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

    public static String buildTempFileName(final UriInfo uriInfo) {
          return WebDavConstants.TEMP_LOCATION + "/" + (uriInfo.getPath().toString()).replace("/", "_");
    }

    public static String buildWebDavFileName(final UriInfo uriInfo, final File file) {
        return file.getName().replaceFirst((uriInfo.getPath().toString()).replace("/", "_"), "");
    }
}
