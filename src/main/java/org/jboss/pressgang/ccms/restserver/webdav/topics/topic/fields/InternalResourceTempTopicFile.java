package org.jboss.pressgang.ccms.restserver.webdav.topics.topic.fields;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.restserver.utils.JNDIUtilities;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavConstants;
import org.jboss.pressgang.ccms.restserver.webdav.WebDavUtils;
import org.jboss.pressgang.ccms.restserver.webdav.internal.InternalResource;
import org.jboss.pressgang.ccms.restserver.webdav.internal.StringReturnValue;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: matthew
 * Date: 5/10/13
 * Time: 2:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class InternalResourceTempTopicFile extends InternalResource {

    public InternalResourceTempTopicFile(final String path) {
        super(path);
    }

    @Override
    public int write(final String contents) {
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
    public StringReturnValue get() {

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
    public int delete(){
        final String fileLocation = buildTempFileName(stringId);

        final File file = new File(fileLocation);
        if (file.exists()) {
            file.delete();
            return Response.Status.OK.getStatusCode();
        }

        return Response.Status.NOT_FOUND.getStatusCode();
    }

    public static String buildTempFileName(final String path) {
        return WebDavConstants.TEMP_LOCATION + "/" + path.replace("/", "_");
    }

    public static String buildWebDavFileName(final String path, final File file) {
        return file.getName().replaceFirst(path.replace("/", "_"), "");
    }
}
