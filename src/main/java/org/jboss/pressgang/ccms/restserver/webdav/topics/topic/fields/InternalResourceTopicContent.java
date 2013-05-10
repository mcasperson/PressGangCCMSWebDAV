package org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields;

import org.apache.commons.io.IOUtils;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.restserver.utils.JNDIUtilities;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavUtils;
import org.jboss.pressgang.ccms.restserver.webdav.internal.InternalResource;
import org.jboss.pressgang.ccms.restserver.webdav.internal.StringReturnValue;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import javax.ws.rs.core.Response;
import java.io.StringWriter;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: matthew
 * Date: 5/10/13
 * Time: 1:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class InternalResourceTopicContent extends InternalResource {

    private static final Logger LOGGER = Logger.getLogger(InternalResourceTopicContent.class.getName());

    public InternalResourceTopicContent(final Integer intId) {
        super(intId);
    }

    @Override
    public int write(final String contents) {

        EntityManager entityManager = null;
        TransactionManager transactionManager = null;

        try {
            LOGGER.info("ENTER WebDavTopicContent.put()");

            transactionManager = JNDIUtilities.lookupTransactionManager();
            transactionManager.begin();

            entityManager = WebDavUtils.getEntityManager(false);

            final Topic topic = entityManager.find(Topic.class, intId);

            if (topic != null) {

                topic.setTopicXML(contents);

                entityManager.persist(topic);
                entityManager.flush();
                transactionManager.commit();

                return Response.Status.CREATED.getStatusCode();
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
        }  finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

    }

    @Override
    public StringReturnValue get() {

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
    public int delete(){
        /* we pretend to delete these resources */
        return Response.Status.NO_CONTENT.getStatusCode();
    }
}
