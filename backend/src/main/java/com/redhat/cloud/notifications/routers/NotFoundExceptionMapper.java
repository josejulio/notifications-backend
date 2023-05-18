package com.redhat.cloud.notifications.routers;

import io.vertx.ext.web.RoutingContext;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Inject
    RoutingContext routingContext;

    @Inject
    ContainerRequestContext containerRequestContext;

    @Override
    public Response toResponse(NotFoundException exception) {
        // String path = routingContext.currentRoute().getPath();
        String path = containerRequestContext.getUriInfo().getPath();
        if (path.contains("/v2.0/")) {
            path = path.replace("/v2.0/", "/v1.0/");
            routingContext.redirect(path);
        }

        return Response
                .status(exception.getResponse().getStatus())
                .type(MediaType.TEXT_PLAIN)
                .entity(exception.getMessage())
                .build();
    }
}
