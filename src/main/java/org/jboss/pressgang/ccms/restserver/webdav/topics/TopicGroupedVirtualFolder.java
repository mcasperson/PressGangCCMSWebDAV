package org.jboss.pressgang.ccms.restserver.webdav.topics;

import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavConstants;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavResource;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavUtils;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static net.java.dev.webdav.jaxrs.Headers.DEPTH;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.*;

import javax.persistence.EntityManager;
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

/**
    The virtual folder that holds all the topics within a given id range.
 */
@Path("/TOPICS/{start:\\d+}-{end:\\d+}")
public class TopicGroupedVirtualFolder extends WebDavResource {

    public static final String RESOURCE_NAME = "TOPICS";
    private static final Logger LOGGER = Logger.getLogger(TopicGroupedVirtualFolder.class.getName());

    @PathParam("start") int start;
    @PathParam("end") int end;

    @Override
    @Produces(MediaType.APPLICATION_XML)
    @PROPFIND
    public javax.ws.rs.core.Response propfind(@Context final UriInfo uriInfo, @HeaderParam(DEPTH) final int depth, final InputStream entityStream,
                                              @HeaderParam(CONTENT_LENGTH) final long contentLength, @Context final Providers providers,
                                              @Context final HttpHeaders httpHeaders) throws URISyntaxException, IOException {
        try {
            LOGGER.info("ENTER TopicGroupedVirtualFolder.propfind()");

            if (depth == 0) {
                LOGGER.info("Depth == 0");
                /* A depth of zero means we are returning information about this item only */
                return javax.ws.rs.core.Response.status(207).entity(new MultiStatus(getFolderProperties(uriInfo))).type(WebDavConstants.XML_MIME).build();
            } else {
                LOGGER.info("Getting children of the TOPICS virtual folder");
                /* Otherwise we are retuning info on the children in this collection */
                final EntityManager entityManager = WebDavUtils.getEntityManager(false);
                final List<Topic> topics = entityManager.createQuery("SELECT topic FROM Topic topic where topic.topicId >= " + start + " and topic.topicId <= " + end).getResultList();
                final List<net.java.dev.webdav.jaxrs.xml.elements.Response> responses = new ArrayList<net.java.dev.webdav.jaxrs.xml.elements.Response>();

                LOGGER.info(topics.size() + " topics found.");

                for (final Topic topic : topics) {
                    responses.add(getFolderProperties(uriInfo, topic.getTopicId().toString()));
                }

                final MultiStatus st = new MultiStatus(responses.toArray(new net.java.dev.webdav.jaxrs.xml.elements.Response[responses.size()]));

                return javax.ws.rs.core.Response.status(207).entity(st).type(WebDavConstants.XML_MIME).build();
            }

        } catch (final Exception ex) {
            LOGGER.severe(ex.toString());
            ex.printStackTrace();
            return javax.ws.rs.core.Response.status(500).build();
        }
    }

}
