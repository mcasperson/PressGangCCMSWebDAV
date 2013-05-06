package org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields;

import net.java.dev.webdav.jaxrs.methods.PROPPATCH;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.elements.Response;
import net.java.dev.webdav.jaxrs.xml.properties.*;
import org.apache.commons.io.IOUtils;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavConstants;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavResource;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavUtils;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static net.java.dev.webdav.jaxrs.Headers.DEPTH;
import static javax.ws.rs.core.Response.Status.OK;


import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Providers;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.logging.Logger;

/**
    A WebDAV file that holds the topics contents.
 */
@Path("{var:.*}TOPICS{var:.*}/{topicId:\\d+}/CONTENT")
public class WebDavTopicContent extends WebDavResource {

    private static final String RESOURCE_NAME = "CONTENT";
    private static final Logger LOGGER = Logger.getLogger(WebDavTopicContent.class.getName());

    @PathParam("topicId") int topicId;

    @Override
    @GET
    @Produces(WebDavConstants.OCTET_STREAM_MIME)
    public javax.ws.rs.core.Response get() {
        LOGGER.info("ENTER WebDavTopicContent.get()");

        try {
            final EntityManager entityManager = WebDavUtils.getEntityManager(false);

            final Topic topic = entityManager.find(Topic.class, topicId);

            if (topic != null) {
                return javax.ws.rs.core.Response.ok().entity(topic.getTopicXML()).build();
            }

        } catch (final Exception ex) {
            return javax.ws.rs.core.Response.status(404).build();
        }

        return javax.ws.rs.core.Response.status(404).build();
    }

    @Override
    @PUT
    @Consumes("*/*")
    public javax.ws.rs.core.Response put(@Context final UriInfo uriInfo, final InputStream entityStream, @HeaderParam(CONTENT_LENGTH) final long contentLength,
                                         @HeaderParam("Expect") final String expect)
            throws IOException, URISyntaxException {

        try {
            if(expect != null){
                return javax.ws.rs.core.Response.status(417).build();
            }

            if (contentLength == 0)
                return javax.ws.rs.core.Response.ok().build();

            final EntityManager entityManager = WebDavUtils.getEntityManager(false);
            final Topic topic = entityManager.find(Topic.class, topicId);

            if (topic != null) {

                StringWriter writer = new StringWriter();
                IOUtils.copy(entityStream, writer, "UTF-8");

                topic.setTopicXML(writer.toString());
                entityManager.persist(topic);
            }

        } catch (final Exception ex) {
            return javax.ws.rs.core.Response.status(404).build();
        }

        return javax.ws.rs.core.Response.status(404).build();
    }

    @Override
    public javax.ws.rs.core.Response propfind(@Context UriInfo uriInfo, @HeaderParam(DEPTH) int depth, InputStream entityStream, @HeaderParam(CONTENT_LENGTH) long contentLength, @Context Providers providers, @Context HttpHeaders httpHeaders) throws URISyntaxException, IOException {
        LOGGER.info("ENTER WebDavTopic.propfind()");

        try {
            final EntityManager entityManager = WebDavUtils.getEntityManager(false);

            final Topic topic = entityManager.find(Topic.class, topicId);

            if (topic != null) {
                final Response response = getProperties(uriInfo, topic);
                final MultiStatus st = new MultiStatus(response);
                return javax.ws.rs.core.Response.status(207).entity(st).type(WebDavConstants.XML_MIME).build();
            }

        } catch (final NumberFormatException ex) {
            return javax.ws.rs.core.Response.status(404).build();
        }

        return javax.ws.rs.core.Response.status(404).build();
    }

    @Override
    @PROPPATCH
    public javax.ws.rs.core.Response proppatch(@Context final UriInfo uriInfo, final InputStream body, @Context final Providers providers, @Context final HttpHeaders httpHeaders)
            throws IOException, URISyntaxException {

        final PropertyUpdate propertyUpdate =
                providers.getMessageBodyReader(PropertyUpdate.class, PropertyUpdate.class, new Annotation[0],MediaType.APPLICATION_XML_TYPE).
                    readFrom(PropertyUpdate.class, PropertyUpdate.class, new Annotation[0], MediaType.APPLICATION_XML_TYPE,httpHeaders.getRequestHeaders(), body);

        LOGGER.info("PATCH PROPERTIES: " + propertyUpdate.list());

        return propfind(uriInfo, 0, body, 0, providers, httpHeaders);
    }

    public static Response getProperties(final UriInfo uriInfo, final Topic topic) {
        final HRef hRef = new HRef(uriInfo.getRequestUriBuilder().path(RESOURCE_NAME).build());
        final CreationDate creationDate = new CreationDate(topic.getTopicTimeStamp() == null ? new Date() : topic.getTopicTimeStamp());
        final GetLastModified getLastModified = new GetLastModified(topic.getLastModifiedDate() == null ? new Date() : topic.getLastModifiedDate());
        final GetContentType getContentType = new GetContentType(WebDavConstants.OCTET_STREAM_MIME);
        final GetContentLength getContentLength = new GetContentLength(topic.getTopicXML() == null ? 0 : topic.getTopicXML().length());
        final DisplayName displayName = new DisplayName(RESOURCE_NAME);
        final Prop prop = new Prop(creationDate, getLastModified, getContentType, getContentLength, displayName);
        final Status status = new Status((javax.ws.rs.core.Response.StatusType) OK);
        final PropStat propStat = new PropStat(prop, status);

        final Response davFile = new Response(hRef, null, null, null, propStat);

        return davFile;
    }
}
