/*
 * Copyright 2008, 2009 Daniel MANZKE
 *
 * This file is part of webdav-jaxrs.
 *
 * webdav-jaxrs is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * webdav-jaxrs is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with webdav-jaxrs.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jboss.pressgang.ccms.restserver.webdav;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.Response.Status.OK;
import static net.java.dev.webdav.jaxrs.Headers.*;
import static net.java.dev.webdav.jaxrs.xml.properties.ResourceType.COLLECTION;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import net.java.dev.webdav.jaxrs.methods.COPY;
import net.java.dev.webdav.jaxrs.methods.MKCOL;
import net.java.dev.webdav.jaxrs.methods.MOVE;
import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.methods.PROPPATCH;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.properties.CreationDate;
import net.java.dev.webdav.jaxrs.xml.properties.GetLastModified;
import org.jboss.pressgang.ccms.restserver.utils.JNDIUtilities;
import org.jboss.resteasy.spi.InternalServerErrorException;

public class WebDavResource {
    public static final String WEBDAV_COMPLIANCE_LEVEL = "1";

    private static final Logger LOGGER = Logger.getLogger(WebDavResource.class.getName());

    @GET
    @Produces("application/octet-stream")
    public javax.ws.rs.core.Response get() {
        LOGGER.info("ENTER WebDavResource.get()");
        return javax.ws.rs.core.Response.serverError().build();
    }

    @PUT
    @Consumes("*/*")
    public javax.ws.rs.core.Response put(@Context final UriInfo uriInfo, final InputStream entityStream, @HeaderParam(CONTENT_LENGTH) final long contentLength)
            throws IOException, URISyntaxException {
        LOGGER.info("ENTER WebDavResource.put()");
        return javax.ws.rs.core.Response.serverError().build();
    }

    @MKCOL
    public javax.ws.rs.core.Response mkcol() {
        LOGGER.info("ENTER WebDavResource.mkcol()");
        return javax.ws.rs.core.Response.serverError().build();
    }

    @Produces("application/xml")
    @PROPFIND
    public javax.ws.rs.core.Response propfind(@Context final UriInfo uriInfo, @HeaderParam(DEPTH) final int depth, final InputStream entityStream,
                                       @HeaderParam(CONTENT_LENGTH) final long contentLength, @Context final Providers providers,
                                       @Context final HttpHeaders httpHeaders) throws URISyntaxException, IOException {
        LOGGER.info("ENTER WebDavResource.propfind()");
        return javax.ws.rs.core.Response.serverError().build();
    }

    @PROPPATCH
    public javax.ws.rs.core.Response proppatch(@Context final UriInfo uriInfo, final InputStream body, @Context final Providers providers, @Context final HttpHeaders httpHeaders) throws IOException, URISyntaxException {
        LOGGER.info("ENTER WebDavResource.proppatch()");
        return javax.ws.rs.core.Response.serverError().build();
    }

    @COPY
    public javax.ws.rs.core.Response copy() {
        LOGGER.info("ENTER WebDavResource.copy()");
        return javax.ws.rs.core.Response.serverError().build();
    }

    /*
        201 (Created)	The resource was moved successfully and a new resource was created at the specified destination URI.
        204 (No Content)	The resource was moved successfully to a pre-existing destination URI.
        403 (Forbidden)	The source URI and the destination URI are the same.
        409 (Conflict)	A resource cannot be created at the destination URI until one or more intermediate collections are created.
        412 (Precondition Failed)	Either the Overwrite header is "F" and the state of the destination resource is not null, or the method was used in a Depth: 0 transaction.
        423 (Locked)	The destination resource is locked.
        502 (Bad Gateway)	The destination URI is located on a different server, which refuses to accept the resource.
     */
    @MOVE
    public javax.ws.rs.core.Response move(@Context final UriInfo uriInfo, @HeaderParam(OVERWRITE) final String overwriteStr, @HeaderParam(DESTINATION) final String destination) throws URISyntaxException {
        LOGGER.info("ENTER WebDavResource.move()");
        return javax.ws.rs.core.Response.serverError().build();
    }

    @DELETE
    public javax.ws.rs.core.Response delete() {
        LOGGER.info("ENTER WebDavResource.delete()");
        return javax.ws.rs.core.Response.serverError().build();
    }

    @OPTIONS
    public javax.ws.rs.core.Response options() {
        LOGGER.info("ENTER WebDavResource.options()");
        javax.ws.rs.core.Response.ResponseBuilder builder = javax.ws.rs.core.Response.ok();
        builder.header(DAV, WEBDAV_COMPLIANCE_LEVEL);

        return builder.build();
    }

    /**
     * Returning a child folder means returning a Respose that identifies a WebDAV collection.
     * This method populates the returned request with the information required to identify
     * a child folder.
     *
     * @param uriInfo The URI of the current request
     * @param resourceName The name of the child folder
     * @return The properties for a child folder
     */
    public static Response getFolderProperties(final UriInfo uriInfo, final String resourceName) {
        final URI uri = uriInfo.getRequestUriBuilder().path(resourceName).build();
        final HRef hRef = new HRef(uri);
        final Date lastModified = new Date();
        final CreationDate creationDate = new CreationDate(lastModified);
        final GetLastModified getLastModified = new GetLastModified(lastModified);
        final Status status = new Status((javax.ws.rs.core.Response.StatusType) OK);
        final Prop prop = new Prop(creationDate, getLastModified, COLLECTION);
        final PropStat propStat = new PropStat(prop, status);

        final Response folder = new Response(hRef, null, null, null, propStat);

        return folder;
    }

    /**
     *
     * @param uriInfo The URI of the current request
     * @return The properties for the current folder
     */
    public static Response getFolderProperties(final UriInfo uriInfo) {
        final URI uri = uriInfo.getRequestUri();
        final HRef hRef = new HRef(uri);
        final Date lastModified = new Date();
        final CreationDate creationDate = new CreationDate(lastModified);
        final GetLastModified getLastModified = new GetLastModified(lastModified);
        final Status status = new Status((javax.ws.rs.core.Response.StatusType) OK);
        final Prop prop = new Prop(creationDate, getLastModified, COLLECTION);
        final PropStat propStat = new PropStat(prop, status);

        final Response folder = new Response(hRef, null, null, null, propStat);

        return folder;
    }
}