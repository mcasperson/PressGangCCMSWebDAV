package org.jboss.pressgang.ccms.restserver.webdav.resources.hierarchy.topics.topic;

import net.java.dev.webdav.jaxrs.xml.elements.MultiStatus;
import net.java.dev.webdav.jaxrs.xml.elements.Response;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.restserver.utils.EnversUtilities;
import org.jboss.pressgang.ccms.restserver.webdav.constants.WebDavConstants;
import org.jboss.pressgang.ccms.restserver.webdav.utils.WebDavUtils;
import org.jboss.pressgang.ccms.restserver.webdav.resources.InternalResource;
import org.jboss.pressgang.ccms.restserver.webdav.resources.MultiStatusReturnValue;
import org.jboss.pressgang.ccms.restserver.webdav.managers.DeleteManager;
import org.jboss.pressgang.ccms.restserver.webdav.managers.ResourceTypes;
import org.jboss.pressgang.ccms.restserver.webdav.resources.hierarchy.topics.topic.fields.InternalResourceTempTopicFile;
import org.jboss.pressgang.ccms.restserver.webdav.resources.hierarchy.topics.topic.fields.InternalResourceTopicContent;

import javax.persistence.EntityManager;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the available fields and temporary files associated with a topic.
 */
public class InternalResourceTopic extends InternalResource {
    public InternalResourceTopic(final UriInfo uriInfo, final Integer intId) {
        super(uriInfo, intId);
    }

    @Override
    public MultiStatusReturnValue propfind(final DeleteManager deleteManager, final int depth) {

        if (getUriInfo() == null) {
            throw new IllegalStateException("Can not perform propfind without uriInfo");
        }

        if (depth == 0) {
            /* A depth of zero means we are returning information about this item only */

            return new MultiStatusReturnValue(207, new MultiStatus(getFolderProperties(getUriInfo())));
        } else {
            /* Otherwise we are retuning info on the children in this collection */
            final EntityManager entityManager = WebDavUtils.getEntityManager(false);

            final Topic topic = entityManager.find(Topic.class, getIntId());

            final List<Response> responses = new ArrayList<Response>();

            /*
                List the field of the topic
             */
            if (topic != null) {
                /* Fix the last modified date */
                topic.setLastModifiedDate(EnversUtilities.getFixedLastModifiedDate(entityManager, topic));

                /* Don't list the contents if it is "deleted" */
                if (!deleteManager.isDeleted(ResourceTypes.TOPIC_CONTENTS, topic.getId())) {
                    responses.add(InternalResourceTopicContent.getProperties(getUriInfo(), topic, false));
                }
            }

            final File dir = new File(WebDavConstants.TEMP_LOCATION);
            final String tempFileNamePrefix = InternalResourceTempTopicFile.buildTempFileName(getUriInfo().getPath());
            if (dir.exists() && dir.isDirectory()) {
                for (final File child : dir.listFiles()) {
                    if (child.getPath().startsWith(tempFileNamePrefix)) {
                        responses.add(InternalResourceTempTopicFile.getProperties(getUriInfo(), child, false));
                    }
                }
            }

            final MultiStatus st = new MultiStatus(responses.toArray(new Response[responses.size()]));
            return new MultiStatusReturnValue(207, st);

        }
    }
}
