package com.redhat.cloud.notifications.routers;

import com.redhat.cloud.notifications.oapi.OApiFilter;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Serve the final OpenAPI documents.
 */
@com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/api")
public class OApiResource {

    @Inject
    OApiFilter oApiFilter;

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.GET)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/{what}/v1.0/openapi.json")
    @Produces(MediaType.APPLICATION_JSON)
    public String serveOpenAPI(@PathParam("what") String what) {
        return oApiFilter.serveOpenApi(what);
    }

}
