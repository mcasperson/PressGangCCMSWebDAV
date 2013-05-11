package org.jboss.pressgang.ccms.restserver.webdav.internal;

import net.java.dev.webdav.jaxrs.xml.elements.HRef;
import org.apache.commons.io.IOUtils;
import org.jboss.pressgang.ccms.restserver.webdav.managers.DeleteManager;
import org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields.InternalResourceTempTopicFile;
import org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields.InternalResourceTopicContent;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The WebDAV server exposes resources from multiple locations. Some resources are found in a database, and some
 * are saved as files.
 * <p/>
 * All resources can potentially be written to, read and deleted. Copying and moving are just combinations of this
 * basic functionality.
 * <p/>
 * Instances of the InternalResource class wrap the functionality needed to read, write and delete.
 * <p/>
 * The InternalResource class is also a factory, matching url paths to the InternalResource instances that manage
 * them. This provides a simple way for the JAX-RS interface to pass off the actual implementation of these underlying
 * methods.
 * <p/>
 * This means that the WebDavResource class can defer functionality to InternalResource.
 */
public abstract class InternalResource {
    private static final Logger LOGGER = Logger.getLogger(InternalResource.class.getName());

    private static final Pattern TOPIC_CONTENTS_RE = Pattern.compile("/TOPICS(/\\d)*/TOPIC\\d+/(?<TopicID>\\d+).xml");
    private static final Pattern TOPIC_TEMP_FILE_RE = Pattern.compile("/TOPICS(/\\d)*/TOPIC\\d+/.*");


    protected final Integer intId;
    protected final String stringId;

    protected InternalResource(final Integer intId) {
        this.intId = intId;
        this.stringId = null;
    }

    protected InternalResource(final String stringId) {
        this.intId = null;
        this.stringId = stringId;
    }

    public int write(final DeleteManager deleteManager, final String contents) {
        throw new UnsupportedOperationException();
    }

    public int delete(final DeleteManager deleteManager) {
        throw new UnsupportedOperationException();
    }

    public StringReturnValue get(final DeleteManager deleteManager) {
        throw new UnsupportedOperationException();
    }

    public static javax.ws.rs.core.Response copy(final DeleteManager deleteManager, final UriInfo uriInfo, final String overwriteStr, final String destination) {
        LOGGER.info("ENTER InternalResourceTopicContent.copy() " + uriInfo.getPath() + " " + destination);

        try {
            final HRef destHRef = new HRef(destination);
            final URI destUriInfo = destHRef.getURI();

            final InternalResource destinationResource = InternalResource.getInternalResource(destUriInfo.getPath());
            final InternalResource sourceResource = InternalResource.getInternalResource(uriInfo.getPath());

            if (destinationResource != null && sourceResource != null) {
                final StringReturnValue stringReturnValue = sourceResource.get(deleteManager);

                if (stringReturnValue.getStatusCode() != javax.ws.rs.core.Response.Status.OK.getStatusCode()) {
                    return javax.ws.rs.core.Response.status(stringReturnValue.getStatusCode()).build();
                }

                int statusCode;
                if ((statusCode = destinationResource.write(deleteManager, stringReturnValue.getValue())) != javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode()) {
                    return javax.ws.rs.core.Response.status(statusCode).build();
                }

                return javax.ws.rs.core.Response.ok().build();

            }
        } catch (final URISyntaxException e) {

        }

        return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
    }

    public static javax.ws.rs.core.Response move(final DeleteManager deleteManager, final UriInfo uriInfo, final String overwriteStr, final String destination) {

        LOGGER.info("ENTER InternalResourceTopicContent.move() " + uriInfo.getPath() + " " + destination);

        /*
            We can't move outside of the filesystem
         */
        if (!destination.startsWith(uriInfo.getBaseUri().toString())) {
            return javax.ws.rs.core.Response.status(Response.Status.NOT_FOUND).build();
        }

        final InternalResource destinationResource = InternalResource.getInternalResource("/" + destination.replaceFirst(uriInfo.getBaseUri().toString(), ""));
        final InternalResource sourceResource = InternalResource.getInternalResource(uriInfo.getPath());

        if (destinationResource != null && sourceResource != null) {
            final StringReturnValue stringReturnValue = sourceResource.get(deleteManager);

            if (stringReturnValue.getStatusCode() != javax.ws.rs.core.Response.Status.OK.getStatusCode()) {
                return javax.ws.rs.core.Response.status(stringReturnValue.getStatusCode()).build();
            }

            int statusCode;
            if ((statusCode = destinationResource.write(deleteManager, stringReturnValue.getValue())) != javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode()) {
                return javax.ws.rs.core.Response.status(statusCode).build();
            }

            if ((statusCode = sourceResource.delete(deleteManager)) != javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode()) {
                return javax.ws.rs.core.Response.status(statusCode).build();
            }

            return javax.ws.rs.core.Response.ok().build();

        }

        return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
    }

    public static javax.ws.rs.core.Response delete(final DeleteManager deleteManager, final UriInfo uriInfo) {
        final InternalResource sourceResource = InternalResource.getInternalResource(uriInfo.getPath());

        if (sourceResource != null) {
            return javax.ws.rs.core.Response.status(sourceResource.delete(deleteManager)).build();
        }

        return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
    }

    public static StringReturnValue get(final DeleteManager deleteManager, final UriInfo uriInfo) {
        final InternalResource sourceResource = InternalResource.getInternalResource(uriInfo.getPath());

        if (sourceResource != null) {
            StringReturnValue statusCode;
            if ((statusCode = sourceResource.get(deleteManager)).getStatusCode() != javax.ws.rs.core.Response.Status.OK.getStatusCode()) {
                return statusCode;
            }

            return statusCode;
        }

        return new StringReturnValue(javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    public static javax.ws.rs.core.Response put(final DeleteManager deleteManager, final UriInfo uriInfo, final InputStream entityStream) {
        try {
            final InternalResource sourceResource = InternalResource.getInternalResource(uriInfo.getPath());

            if (sourceResource != null) {
                final StringWriter writer = new StringWriter();
                IOUtils.copy(entityStream, writer, "UTF-8");
                final String newContents = writer.toString();

                int statusCode = sourceResource.write(deleteManager, newContents);
                return javax.ws.rs.core.Response.status(statusCode).build();
            }

            return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
        } catch (final IOException e) {

        }

        return javax.ws.rs.core.Response.serverError().build();
    }

    public static InternalResource getInternalResource(final String path) {
        final Matcher topicContents = TOPIC_CONTENTS_RE.matcher(path);
        if (topicContents.matches()) {
            return new InternalResourceTopicContent(Integer.parseInt(topicContents.group("TopicID")));
        }

        final Matcher topicTemp = TOPIC_TEMP_FILE_RE.matcher(path);
        if (topicTemp.matches()) {
            return new InternalResourceTempTopicFile(path);
        }

        return null;
    }
}
