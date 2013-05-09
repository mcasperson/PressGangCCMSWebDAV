package org.jboss.pressgang.ccms.restserver.webdav.topics;

import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.xml.elements.MultiStatus;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.restserver.webdav.MathUtils;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavConstants;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavResource;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavUtils;

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
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static net.java.dev.webdav.jaxrs.Headers.DEPTH;

/**

 */
@Path("/TOPICS{var:(/\\d)*}")
public class TopicVirtualFolder extends WebDavResource {

    public static final String RESOURCE_NAME = "TOPICS";
    private static final Logger LOGGER = Logger.getLogger(TopicVirtualFolder.class.getName());
    @PathParam("var") private String var;

    @Override
    @Produces(MediaType.APPLICATION_XML)
    @PROPFIND
    public javax.ws.rs.core.Response propfind(@Context final UriInfo uriInfo, @HeaderParam(DEPTH) final int depth, final InputStream entityStream,
                                              @HeaderParam(CONTENT_LENGTH) final long contentLength, @Context final Providers providers,
                                              @Context final HttpHeaders httpHeaders) throws URISyntaxException, IOException {
        try {
            LOGGER.info("ENTER TopicVirtualFolder.propfind()");

            if (depth == 0) {
                /* A depth of zero means we are returning information about this item only */
                return javax.ws.rs.core.Response.status(207).entity(new MultiStatus(getFolderProperties(uriInfo))).type(MediaType.TEXT_XML).build();
            } else {
                /* Otherwise we are retuning info on the children in this collection */

                EntityManager entityManager = null;
                try {

                    entityManager = WebDavUtils.getEntityManager(false);

                    final Integer minId = entityManager.createQuery("SELECT MIN(topic.topicId) FROM Topic topic", Integer.class).getSingleResult();
                    final Integer maxId = entityManager.createQuery("SELECT MAX(topic.topicId) FROM Topic topic", Integer.class).getSingleResult();

                    int maxScale = Math.abs(minId) > maxId ? Math.abs(minId) : maxId;

                    /* find out how large is the largest (or smallest) topic id, logarithmicaly speaking */
                    final int zeros = MathUtils.getScale(maxScale);

                    Integer lastPath = null;

                    if (!(var == null || var.isEmpty())) {
                        final String[] varElements = var.split("/");
                        StringBuilder path = new StringBuilder();
                        for (final String varElement : varElements) {
                            path.append(varElement);
                        }
                        lastPath = Integer.parseInt(path.toString());
                    }

                    final int thisPathZeros = lastPath == null ? 0 : MathUtils.getScale(lastPath);

                    /* we've gone too deep */
                    if (thisPathZeros > zeros)  {
                        return javax.ws.rs.core.Response.status(404).build();
                    }

                    /* the response collection */
                    final List<net.java.dev.webdav.jaxrs.xml.elements.Response> responses = new ArrayList<net.java.dev.webdav.jaxrs.xml.elements.Response>();

                    /* The only purpose of the directory /TOPICS/0 is to list TOPIC0 */
                    if (lastPath == null || lastPath != 0) {
                        for (int i = 0; i < 10; ++i) {
                            responses.add(getFolderProperties(uriInfo, i + ""));
                        }
                    }

                    if (lastPath != null) {
                        responses.add(getFolderProperties(uriInfo, "TOPIC" + lastPath.toString()));
                    }

                    final MultiStatus st = new MultiStatus(responses.toArray(new net.java.dev.webdav.jaxrs.xml.elements.Response[responses.size()]));

                    return javax.ws.rs.core.Response.status(207).entity(st).type(MediaType.TEXT_XML).build();
                } finally {
                    if (entityManager != null) {
                        entityManager.close();
                    }
                }
            }

        } catch (final Exception ex) {
            LOGGER.severe(ex.toString());
            ex.printStackTrace();
            return javax.ws.rs.core.Response.status(500).build();
        }
    }

}
