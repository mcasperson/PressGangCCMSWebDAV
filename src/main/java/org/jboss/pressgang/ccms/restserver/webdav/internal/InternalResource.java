package org.jboss.pressgang.ccms.restserver.webdav.internal;

import net.java.dev.webdav.jaxrs.xml.elements.HRef;
import org.apache.commons.io.IOUtils;
import org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields.InternalResourceTempTopicFile;
import org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields.InternalResourceTopicContent;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.java.dev.webdav.jaxrs.Headers.DESTINATION;
import static net.java.dev.webdav.jaxrs.Headers.OVERWRITE;

/**
 * Created with IntelliJ IDEA.
 * User: matthew
 * Date: 5/10/13
 * Time: 12:59 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class InternalResource {
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

    public int write(final String contents) {
        throw new NotImplementedException();
    }

    public int delete(){
        throw new NotImplementedException();
    }

    public StringReturnValue get(){
        throw new NotImplementedException();
    }

    public static javax.ws.rs.core.Response copy(final UriInfo uriInfo, final String overwriteStr, final String destination) {
        try {
            final HRef destHRef = new HRef(destination);
            final URI destUriInfo = destHRef.getURI();

            final InternalResource destinationResource = InternalResource.getInternalResource(destUriInfo.getPath());
            final InternalResource sourceResource = InternalResource.getInternalResource(uriInfo.getPath());

            if (destinationResource != null && sourceResource != null) {
                final StringReturnValue stringReturnValue = sourceResource.get();

                if (stringReturnValue.getStatusCode() != javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode()) {
                    return javax.ws.rs.core.Response.status(stringReturnValue.getStatusCode()).build();
                }

                int statusCode;
                if ((statusCode = destinationResource.write(stringReturnValue.getValue())) != javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode()) {
                    return javax.ws.rs.core.Response.status(statusCode).build();
                }

                return javax.ws.rs.core.Response.ok().build();

            }
        } catch (final URISyntaxException e) {

        }

        return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
    }

    public static javax.ws.rs.core.Response move(final UriInfo uriInfo, final String overwriteStr, final String destination) {

        /*
            We can't move outside of the filesystem
         */
        if (!destination.startsWith(uriInfo.getBaseUri().toString())) {
            return javax.ws.rs.core.Response.status(Response.Status.NOT_FOUND).build();
        }

        final InternalResource destinationResource = InternalResource.getInternalResource(destination.replaceFirst(uriInfo.getBaseUri().toString(), ""));
        final InternalResource sourceResource = InternalResource.getInternalResource(uriInfo.getPath());

        if (destinationResource != null && sourceResource != null) {
            final StringReturnValue stringReturnValue = sourceResource.get();

            if (stringReturnValue.getStatusCode() != javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode()) {
                return javax.ws.rs.core.Response.status(stringReturnValue.getStatusCode()).build();
            }

            int statusCode;
            if ((statusCode = destinationResource.write(stringReturnValue.getValue())) != javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode()) {
                return javax.ws.rs.core.Response.status(statusCode).build();
            }

            if ((statusCode = sourceResource.delete()) != javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode()) {
                return javax.ws.rs.core.Response.status(statusCode).build();
            }

            return javax.ws.rs.core.Response.ok().build();

        }

        return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
    }

    public static javax.ws.rs.core.Response delete(final UriInfo uriInfo) {
        final InternalResource sourceResource = InternalResource.getInternalResource(uriInfo.getPath());

        if (sourceResource != null) {
            int statusCode;
            if ((statusCode = sourceResource.delete()) != javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode()) {
                return javax.ws.rs.core.Response.status(statusCode).build();
            }

            return javax.ws.rs.core.Response.ok().build();
        }

        return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
    }

    public static StringReturnValue get(final UriInfo uriInfo) {
        final InternalResource sourceResource = InternalResource.getInternalResource(uriInfo.getPath());

        if (sourceResource != null) {
            StringReturnValue statusCode;
            if ((statusCode = sourceResource.get()).getStatusCode() != javax.ws.rs.core.Response.Status.OK.getStatusCode()) {
                return statusCode;
            }

            return statusCode;
        }

        return new StringReturnValue(javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    public static javax.ws.rs.core.Response put(final UriInfo uriInfo, final InputStream entityStream) {
        try {
            final InternalResource sourceResource = InternalResource.getInternalResource(uriInfo.getPath());

            if (sourceResource != null) {
                final StringWriter writer = new StringWriter();
                IOUtils.copy(entityStream, writer, "UTF-8");
                final String newContents = writer.toString();

                int statusCode = sourceResource.write(newContents);
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
