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
package org.jboss.pressgang.ccms.restserver.webdav.jaxrs;

import net.java.dev.webdav.jaxrs.methods.*;
import org.jboss.pressgang.ccms.restserver.webdav.resources.InternalResource;
import org.jboss.pressgang.ccms.restserver.webdav.resources.ByteArrayReturnValue;
import org.jboss.pressgang.ccms.restserver.webdav.managers.DeleteManager;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import static net.java.dev.webdav.jaxrs.Headers.*;

/**
 * This JAX-RS endpoint captures all requests. It then relays them to the InternalResource class,
 * which will build the objects that will actually respond to the requests.
 */
@RequestScoped
@Path("{var:.*}")
public class WebDavResource {
    public static final String WEBDAV_COMPLIANCE_LEVEL = "1";

    private static final Logger LOGGER = Logger.getLogger(WebDavResource.class.getName());

    @Inject
    protected DeleteManager deleteManager;

    @GET
    @Produces("application/octet-stream")
    public javax.ws.rs.core.Response get(@Context final UriInfo uriInfo) {
        final ByteArrayReturnValue stringValueReturn = InternalResource.get(deleteManager, uriInfo);
        if (stringValueReturn.getStatusCode() != javax.ws.rs.core.Response.Status.OK.getStatusCode()) {
            return javax.ws.rs.core.Response.status(stringValueReturn.getStatusCode()).build();
        }

        return javax.ws.rs.core.Response.ok().entity(stringValueReturn.getValue()).build();
    }

    @PUT
    @Consumes("*/*")
    public javax.ws.rs.core.Response put(@Context final UriInfo uriInfo, final InputStream entityStream) throws IOException, URISyntaxException {
        return InternalResource.put(deleteManager, uriInfo, entityStream);
    }

    @MKCOL
    public javax.ws.rs.core.Response mkcol() {
        LOGGER.info("ENTER WebDavResource.mkcol()");
        return javax.ws.rs.core.Response.serverError().build();
    }

    @Produces("application/xml")
    @PROPFIND
    public javax.ws.rs.core.Response propfind(@Context final UriInfo uriInfo, @HeaderParam(DEPTH) final int depth) throws URISyntaxException, IOException {
        LOGGER.info("ENTER WebDavResource.propfind()");
        return InternalResource.propfind(deleteManager, uriInfo, depth);
    }

    @PROPPATCH
    public javax.ws.rs.core.Response proppatch(@Context final UriInfo uriInfo, final InputStream body, @Context final Providers providers, @Context final HttpHeaders httpHeaders) throws IOException, URISyntaxException {
        LOGGER.info("ENTER WebDavResource.proppatch()");
        return javax.ws.rs.core.Response.serverError().build();
    }

    @COPY
    public javax.ws.rs.core.Response copy(@Context final UriInfo uriInfo, @HeaderParam(OVERWRITE) final String overwriteStr, @HeaderParam(DESTINATION) final String destination) {
        return InternalResource.copy(deleteManager, uriInfo, overwriteStr, destination);
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
        return InternalResource.move(deleteManager, uriInfo, overwriteStr, destination);
    }

    @DELETE
    public javax.ws.rs.core.Response delete(@Context final UriInfo uriInfo) {
        return InternalResource.delete(deleteManager, uriInfo);
    }

    @OPTIONS
    public javax.ws.rs.core.Response options() {
        LOGGER.info("ENTER WebDavResource.options()");
        javax.ws.rs.core.Response.ResponseBuilder builder = javax.ws.rs.core.Response.ok();
        builder.header(DAV, WEBDAV_COMPLIANCE_LEVEL);

        return builder.build();
    }
}