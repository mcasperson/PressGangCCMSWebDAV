package org.jboss.pressgang.ccms.restserver.webdav.topics.topic;

import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.xml.elements.MultiStatus;
import net.java.dev.webdav.jaxrs.xml.elements.Response;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.restserver.utils.EnversUtilities;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavConstants;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavResource;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavUtils;
import org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields.InternalResourceTempTopicFile;
import org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields.WebDavTempTopicFile;
import org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields.WebDavTopicContent;

import javax.persistence.EntityManager;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static net.java.dev.webdav.jaxrs.Headers.DEPTH;

/**
 * The virtual folder that holds all the topic's details
 */
@Path("/TOPICS{var:(/\\d)*}/{topicId:TOPIC\\d*}")
public class WebDavTopic extends WebDavResource {

    private static final Logger LOGGER = Logger.getLogger(WebDavTopic.class.getName());

    @PathParam("topicId")
    String topicIdString;

    @Override
    @Produces(MediaType.APPLICATION_XML)
    @PROPFIND
    public javax.ws.rs.core.Response propfind(@Context final UriInfo uriInfo, @HeaderParam(DEPTH) final int depth, final InputStream entityStream,
                                              @HeaderParam(CONTENT_LENGTH) final long contentLength, @Context final Providers providers,
                                              @Context final HttpHeaders httpHeaders) throws URISyntaxException, IOException {
        try {

            if (depth == 0) {
                /* A depth of zero means we are returning information about this item only */

                return javax.ws.rs.core.Response.status(207).entity(new MultiStatus(getFolderProperties(uriInfo))).type(MediaType.TEXT_XML).build();
            } else {
                /* Otherwise we are retuning info on the children in this collection */
                final EntityManager entityManager = WebDavUtils.getEntityManager(false);

                final Integer topicId = Integer.parseInt(topicIdString.replaceFirst("TOPIC", ""));
                final Topic topic = entityManager.find(Topic.class, topicId);

                final List<Response> responses = new ArrayList<Response>();

                if (topic != null) {

                    /* Fix the last modified date */
                    topic.setLastModifiedDate(EnversUtilities.getFixedLastModifiedDate(entityManager, topic));
                    responses.add(WebDavTopicContent.getProperties(uriInfo, topic, false));
                }

                final File dir = new File(WebDavConstants.TEMP_LOCATION);
                final String tempFileNamePrefix = InternalResourceTempTopicFile.buildTempFileName(uriInfo.getPath());
                if (dir.exists() && dir.isDirectory()) {
                    for (final File child : dir.listFiles()) {
                        if (child.getPath().startsWith(tempFileNamePrefix)) {
                            responses.add(WebDavTempTopicFile.getProperties(uriInfo, child, false));
                        }
                    }
                }

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
