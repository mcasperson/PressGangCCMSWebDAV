package org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields;

import net.java.dev.webdav.jaxrs.methods.*;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.elements.Response;
import net.java.dev.webdav.jaxrs.xml.properties.*;
import org.apache.commons.io.IOUtils;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.restserver.utils.JNDIUtilities;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavConstants;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavResource;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavUtils;
import org.jboss.pressgang.ccms.restserver.webdav.system.FixedCreationDate;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.Response.Status.OK;
import static net.java.dev.webdav.jaxrs.Headers.*;


import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import javax.ws.rs.*;
import javax.ws.rs.OPTIONS;
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
@Path("/TOPICS{var:(/\\d)*}/{topicId:TOPIC\\d*}/{topicId2:\\d+}.xml")
public class WebDavTopicContent extends WebDavResource {

    private static final Logger LOGGER = Logger.getLogger(WebDavTopicContent.class.getName());

    @PathParam("topicId") private String topicIdString;
    @PathParam("topicId2") private int topicId2;

    @Override
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public javax.ws.rs.core.Response get(@Context final UriInfo uriInfo) {
        final Integer topicId = Integer.parseInt(topicIdString.replaceFirst("TOPIC", ""));

        /*
            The regex will allow two different ids, so check for that here.
         */
        if (topicId != topicId2) {
            return javax.ws.rs.core.Response.status(404).build();
        }

        return super.get(uriInfo);
    }

    @Override
    @PUT
    @Consumes("*/*")
    public javax.ws.rs.core.Response put(@Context final UriInfo uriInfo, final InputStream entityStream)
            throws IOException, URISyntaxException {

        final Integer topicId = Integer.parseInt(topicIdString.replaceFirst("TOPIC", ""));

        /*
            The regex will allow two different ids, so check for that here.
         */
        if (topicId != topicId2) {
            return javax.ws.rs.core.Response.status(404).build();
        }

        return super.put(uriInfo, entityStream);
    }

    @Override
    @PROPFIND
    public javax.ws.rs.core.Response propfind(@Context final UriInfo uriInfo, @HeaderParam(DEPTH) int depth, final InputStream entityStream, @HeaderParam(CONTENT_LENGTH) final long contentLength,
                                              @Context final Providers providers, @Context final HttpHeaders httpHeaders) throws URISyntaxException, IOException {
        LOGGER.info("ENTER WebDavTopic.propfind()");

        try {
            final Integer topicId = Integer.parseInt(topicIdString.replaceFirst("TOPIC", ""));

            final EntityManager entityManager = WebDavUtils.getEntityManager(false);

            final Topic topic = entityManager.find(Topic.class, topicId);

            if (topic != null) {
                final Response response = getProperties(uriInfo, topic, true);
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

    /**
     *
     * @param uriInfo The uri that was used to access this resource
     * @param topic The topic that this content represents
     * @param local true if we are building the properties for the resource at the given uri, and false if we are building
     *              properties for a child resource.
     * @return
     */
    public static Response getProperties(final UriInfo uriInfo, final Topic topic, final boolean local) {
        final HRef hRef = local ? new HRef(uriInfo.getRequestUri()) : new HRef(uriInfo.getRequestUriBuilder().path(topic.getId() + ".xml").build());
        final FixedCreationDate creationDate = new FixedCreationDate(topic.getTopicTimeStamp() == null ? new Date() : topic.getTopicTimeStamp());
        final GetLastModified getLastModified = new GetLastModified(topic.getLastModifiedDate() == null ? new Date() : topic.getLastModifiedDate());
        final GetContentType getContentType = new GetContentType(MediaType.APPLICATION_OCTET_STREAM);
        final GetContentLength getContentLength = new GetContentLength(topic.getTopicXML() == null ? 0 : topic.getTopicXML().length());
        final DisplayName displayName = new DisplayName(topic.getId() + ".xml");
        final SupportedLock supportedLock = new SupportedLock();
        final LockDiscovery lockDiscovery = new LockDiscovery();
        final Prop prop = new Prop(creationDate, getLastModified, getContentType, getContentLength, displayName, supportedLock, lockDiscovery);
        final Status status = new Status((javax.ws.rs.core.Response.StatusType) OK);
        final PropStat propStat = new PropStat(prop, status);

        final Response davFile = new Response(hRef, null, null, null, propStat);

        return davFile;
    }
}
