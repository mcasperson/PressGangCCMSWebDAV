package org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields;

import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.properties.*;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.restserver.utils.JNDIUtilities;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavUtils;
import org.jboss.pressgang.ccms.restserver.webdav.internal.InternalResource;
import org.jboss.pressgang.ccms.restserver.webdav.internal.MultiStatusReturnValue;
import org.jboss.pressgang.ccms.restserver.webdav.internal.StringReturnValue;
import org.jboss.pressgang.ccms.restserver.webdav.managers.DeleteManager;
import org.jboss.pressgang.ccms.restserver.webdav.managers.ResourceTypes;
import org.jboss.pressgang.ccms.restserver.webdav.system.FixedCreationDate;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Date;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.OK;

/**
 * Handles access to topic's XML content.
 */
public class InternalResourceTopicContent extends InternalResource {

    private static final Logger LOGGER = Logger.getLogger(InternalResourceTopicContent.class.getName());

    public InternalResourceTopicContent(final UriInfo uriInfo, final Integer intId) {
        super(uriInfo, intId);
    }

    @Override
    public int write(final DeleteManager deleteManager, final String contents) {

        LOGGER.info("ENTER InternalResourceTopicContent.write() " + intId);
        LOGGER.info(contents);

        EntityManager entityManager = null;
        TransactionManager transactionManager = null;

        try {

            transactionManager = JNDIUtilities.lookupTransactionManager();
            transactionManager.begin();

            entityManager = WebDavUtils.getEntityManager(false);

            final Topic topic = entityManager.find(Topic.class, intId);

            if (topic != null) {

                topic.setTopicXML(contents);

                entityManager.persist(topic);
                entityManager.flush();
                transactionManager.commit();

                deleteManager.create(ResourceTypes.TOPIC_CONTENTS, intId);

                return Response.Status.NO_CONTENT.getStatusCode();
            }

            return Response.Status.NOT_FOUND.getStatusCode();
        } catch (final Exception ex) {
            if (transactionManager != null) {
                try {
                    transactionManager.rollback();
                } catch (final Exception ex2) {
                    LOGGER.severe("There was an error rolling back the transaction " + ex2.toString());
                }
            }

            return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

    }

    @Override
    public StringReturnValue get(final DeleteManager deleteManager) {

        LOGGER.info("ENTER InternalResourceTopicContent.get() " + intId);

        if (deleteManager.isDeleted(ResourceTypes.TOPIC_CONTENTS, intId)) {
            LOGGER.info("Deletion Manager says this file is deleted");
            return new StringReturnValue(Response.Status.NOT_FOUND.getStatusCode(), null);
        }

        EntityManager entityManager = null;

        try {
            entityManager = WebDavUtils.getEntityManager(false);

            final Topic topic = entityManager.find(Topic.class, intId);

            if (topic != null) {
                return new StringReturnValue(Response.Status.OK.getStatusCode(), topic.getTopicXML());
            }

        } catch (final Exception ex) {
            return new StringReturnValue(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null);
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        return new StringReturnValue(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    @Override
    public int delete(final DeleteManager deleteManager) {
        LOGGER.info("ENTER InternalResourceTopicContent.delete() " + intId);

        if (deleteManager.isDeleted(ResourceTypes.TOPIC_CONTENTS, intId)) {
            LOGGER.info("Deletion Manager says this file is already deleted");
            return Response.Status.NOT_FOUND.getStatusCode();
        }

        deleteManager.delete(ResourceTypes.TOPIC_CONTENTS, intId);

        /* pretend to be deleted */
        return Response.Status.NO_CONTENT.getStatusCode();
    }

    @Override
    public MultiStatusReturnValue propfind(final DeleteManager deleteManager, final int depth) {

        if (uriInfo == null) {
            throw new IllegalStateException("Can not perform propfind without uriInfo");
        }

        EntityManager entityManager = null;

        try {
            entityManager = WebDavUtils.getEntityManager(false);

            final Topic topic = entityManager.find(Topic.class, intId);

            if (topic != null) {
                final net.java.dev.webdav.jaxrs.xml.elements.Response response = getProperties(uriInfo, topic, true);
                final MultiStatus st = new MultiStatus(response);
                return new MultiStatusReturnValue(207, st);
            }

        } catch (final NumberFormatException ex) {
            return new MultiStatusReturnValue(Response.Status.NOT_FOUND.getStatusCode());
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        return new MultiStatusReturnValue(Response.Status.NOT_FOUND.getStatusCode());
    }

    /**
     * @param uriInfo The uri that was used to access this resource
     * @param topic   The topic that this content represents
     * @param local   true if we are building the properties for the resource at the given uri, and false if we are building
     *                properties for a child resource.
     * @return
     */
    public static net.java.dev.webdav.jaxrs.xml.elements.Response getProperties(final UriInfo uriInfo, final Topic topic, final boolean local) {
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

        final net.java.dev.webdav.jaxrs.xml.elements.Response davFile = new net.java.dev.webdav.jaxrs.xml.elements.Response(hRef, null, null, null, propStat);

        return davFile;
    }
}
