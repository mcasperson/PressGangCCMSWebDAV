package org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields;

import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.properties.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavConstants;
import org.jboss.pressgang.ccms.restserver.webdav.internal.InternalResource;
import org.jboss.pressgang.ccms.restserver.webdav.internal.MultiStatusReturnValue;
import org.jboss.pressgang.ccms.restserver.webdav.internal.StringReturnValue;
import org.jboss.pressgang.ccms.restserver.webdav.managers.DeleteManager;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.OK;

/**
 * Handles access to temporary files.
 */
public class InternalResourceTempTopicFile extends InternalResource {

    private static final Logger LOGGER = Logger.getLogger(InternalResourceTempTopicFile.class.getName());

    public InternalResourceTempTopicFile(final UriBuilder requestUriBuilder, final String path) {
        super(requestUriBuilder, path);
    }

    @Override
    public int write(final DeleteManager deleteManager, final String contents) {
        LOGGER.info("ENTER InternalResourceTempTopicFile.write() " + stringId);
        LOGGER.info(contents);

        try {
            final File directory = new java.io.File(WebDavConstants.TEMP_LOCATION);
            final String fileLocation = buildTempFileName(stringId);

            if (!directory.exists()) {
                directory.mkdirs();
            } else if (!directory.isDirectory()) {
                directory.delete();
                directory.mkdirs();
            }

            final File file = new File(fileLocation);

            if (!file.exists()) {
                file.createNewFile();
            }

            FileUtils.writeStringToFile(file, contents);

            return Response.Status.NO_CONTENT.getStatusCode();
        } catch (final IOException e) {

        }

        return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }

    @Override
    public StringReturnValue get(final DeleteManager deleteManager) {

        LOGGER.info("ENTER InternalResourceTempTopicFile.get() " + stringId);

        final String fileLocation = buildTempFileName(stringId);

        try {
            final FileInputStream inputStream = new FileInputStream(fileLocation);
            try {
                final String fileContents = IOUtils.toString(inputStream);
                return new StringReturnValue(Response.Status.OK.getStatusCode(), fileContents);

            } catch (final Exception ex) {

            } finally {
                try {
                    inputStream.close();
                } catch (final Exception ex) {

                }
            }
        } catch (final FileNotFoundException e) {
            return new StringReturnValue(Response.Status.NOT_FOUND.getStatusCode(), null);
        }

        return new StringReturnValue(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), null);
    }

    @Override
    public int delete(final DeleteManager deleteManager) {
        LOGGER.info("ENTER InternalResourceTempTopicFile.delete() " + stringId);

        final String fileLocation = buildTempFileName(stringId);

        final File file = new File(fileLocation);
        if (file.exists()) {
            file.delete();
            return Response.Status.OK.getStatusCode();
        }

        return Response.Status.NOT_FOUND.getStatusCode();
    }

    @Override
    public MultiStatusReturnValue propfind(final DeleteManager deleteManager, final int depth) {
        try {
            final String fileLocation = InternalResourceTempTopicFile.buildTempFileName(uriInfo.build().getPath());

            final File file = new File(fileLocation);

            if (file.exists()) {
                final net.java.dev.webdav.jaxrs.xml.elements.Response response = getProperties(uriInfo, file, true);
                final MultiStatus st = new MultiStatus(response);
                return new MultiStatusReturnValue(207, st);
            }

        } catch (final NumberFormatException ex) {
            return new MultiStatusReturnValue(404);
        }

        return new MultiStatusReturnValue(404);
    }

    public static String buildTempFileName(final String path) {
        return WebDavConstants.TEMP_LOCATION + "/" + path.replace("/", "_");
    }

    public static String buildWebDavFileName(final String path, final File file) {
        return file.getName().replaceFirst(path.replace("/", "_"), "");
    }

    /**
     * @param uriRequestBuilder The uri that was used to access this resource
     * @param file    The file that this content represents
     * @param local   true if we are building the properties for the resource at the given uri, and false if we are building
     *                properties for a child resource.
     * @return
     */
    public static net.java.dev.webdav.jaxrs.xml.elements.Response getProperties(final UriBuilder uriRequestBuilder, final File file, final boolean local) {
        final HRef hRef = local ? new HRef(uriRequestBuilder.build()) : new HRef(uriRequestBuilder.path(InternalResourceTempTopicFile.buildWebDavFileName(uriRequestBuilder.build().getPath(), file)).build());
        final GetLastModified getLastModified = new GetLastModified(new Date(file.lastModified()));
        final GetContentType getContentType = new GetContentType(MediaType.APPLICATION_OCTET_STREAM);
        final GetContentLength getContentLength = new GetContentLength(file.length());
        final DisplayName displayName = new DisplayName(file.getName());
        final SupportedLock supportedLock = new SupportedLock();
        final LockDiscovery lockDiscovery = new LockDiscovery();
        final Prop prop = new Prop(getLastModified, getContentType, getContentLength, displayName, supportedLock, lockDiscovery);
        final Status status = new Status((javax.ws.rs.core.Response.StatusType) OK);
        final PropStat propStat = new PropStat(prop, status);

        final net.java.dev.webdav.jaxrs.xml.elements.Response davFile = new net.java.dev.webdav.jaxrs.xml.elements.Response(hRef, null, null, null, propStat);

        return davFile;
    }
}
