package org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields;

import net.java.dev.webdav.jaxrs.methods.MOVE;
import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.methods.PROPPATCH;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.elements.Response;
import net.java.dev.webdav.jaxrs.xml.properties.*;
import org.apache.commons.io.IOUtils;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.restserver.utils.JNDIUtilities;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavConstants;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavResource;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavUtils;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static net.java.dev.webdav.jaxrs.Headers.DEPTH;
import static javax.ws.rs.core.Response.Status.OK;
import static net.java.dev.webdav.jaxrs.Headers.DESTINATION;
import static net.java.dev.webdav.jaxrs.Headers.OVERWRITE;


import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
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
@Path("/TOPICS{var:.*}/{topicId:\\d+}/{topicId2:\\d+}.xml")
public class WebDavTopicContent extends WebDavResource {

    private static final Logger LOGGER = Logger.getLogger(WebDavTopicContent.class.getName());

    @PathParam("topicId") private int topicId;
    @PathParam("topicId2") private int topicId2;

    @Context private UriInfo uriInfo;
    @HeaderParam(CONTENT_LENGTH) private long contentLength;

    @Override
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public javax.ws.rs.core.Response get() {
        LOGGER.info("ENTER WebDavTopicContent.get()");

        /*
            The regex will allow two different ids, so check for that here.
         */
        if (topicId != topicId2) {
            return javax.ws.rs.core.Response.status(404).build();
        }

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
    public javax.ws.rs.core.Response put(final InputStream entityStream)
            throws IOException, URISyntaxException {

        EntityManager entityManager = null;
        TransactionManager transactionManager = null;

        try {
            LOGGER.info("ENTER WebDavTopicContent.put()");

            transactionManager = JNDIUtilities.lookupTransactionManager();
            transactionManager.begin();

            entityManager = WebDavUtils.getEntityManager(false);

            if (contentLength == 0)
                return javax.ws.rs.core.Response.ok().build();

            final Topic topic = entityManager.find(Topic.class, topicId);

            if (topic != null) {

                final StringWriter writer = new StringWriter();
                IOUtils.copy(entityStream, writer, "UTF-8");
                final String newContents = writer.toString();

                LOGGER.info(newContents);

                topic.setTopicXML(newContents);

                entityManager.persist(topic);
                entityManager.flush();
                transactionManager.commit();

                return javax.ws.rs.core.Response.ok().build();
            }
        } catch (final Exception ex) {
            if (transactionManager != null) {
                try {
                    transactionManager.rollback();
                } catch (final Exception ex2) {
                    LOGGER.severe("There was an error rolling back the transaction " + ex2.toString());
                }
            }

            return javax.ws.rs.core.Response.status(404).build();
        }  finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        return javax.ws.rs.core.Response.status(404).build();
    }

    @Override
    @PROPFIND
    public javax.ws.rs.core.Response propfind(@Context final UriInfo uriInfo, @HeaderParam(DEPTH) int depth, final InputStream entityStream, @HeaderParam(CONTENT_LENGTH) final long contentLength,
                                              @Context final Providers providers, @Context final HttpHeaders httpHeaders) throws URISyntaxException, IOException {
        LOGGER.info("ENTER WebDavTopic.propfind()");

        try {
            final EntityManager entityManager = WebDavUtils.getEntityManager(false);

            final Topic topic = entityManager.find(Topic.class, topicId);

            if (topic != null) {
                final Response response = getProperties(uriInfo, topic);
                final MultiStatus st = new MultiStatus(response);
                return javax.ws.rs.core.Response.status(207).entity(st).type(MediaType.TEXT_XML).build();
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
        final HRef hRef = new HRef(uriInfo.getRequestUriBuilder().path(topic.getId() + ".xml").build());
        final CreationDate creationDate = new CreationDate(topic.getTopicTimeStamp() == null ? new Date() : topic.getTopicTimeStamp());
        final GetLastModified getLastModified = new GetLastModified(topic.getLastModifiedDate() == null ? new Date() : topic.getLastModifiedDate());
        final GetContentType getContentType = new GetContentType(MediaType.APPLICATION_OCTET_STREAM);
        final GetContentLength getContentLength = new GetContentLength(topic.getTopicXML() == null ? 0 : topic.getTopicXML().length());
        final DisplayName displayName = new DisplayName(topic.getId() + ".xml");
        final Prop prop = new Prop(creationDate, getLastModified, getContentType, getContentLength, displayName);
        final Status status = new Status((javax.ws.rs.core.Response.StatusType) OK);
        final PropStat propStat = new PropStat(prop, status);

        final Response davFile = new Response(hRef, null, null, null, propStat);

        return davFile;
    }

    /**
     *  A lot of text editors try to move files around during their saving process. This simply tricks them into
     *  believing that they have successfully performed a move.
     */
    @Override
    @MOVE
    public javax.ws.rs.core.Response move(@Context final UriInfo uriInfo, @HeaderParam(OVERWRITE) final String overwriteStr, @HeaderParam(DESTINATION) final String destination) throws URISyntaxException {
        LOGGER.info("ENTER WebDavResource.move()");
        return javax.ws.rs.core.Response.ok().build();
    }

    /**
     *  A lot of text editors try to delete files around during their saving process. This simply tricks them into
     *  believing that they have successfully performed a delete.
     */
    @Override
    @DELETE
    public javax.ws.rs.core.Response delete() {
        LOGGER.info("ENTER WebDavResource.delete()");
        return javax.ws.rs.core.Response.ok().build();
    }
}
