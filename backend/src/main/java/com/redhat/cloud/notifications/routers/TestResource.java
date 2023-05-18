package com.redhat.cloud.notifications.routers;

import com.redhat.cloud.notifications.auth.ConsoleIdentityProvider;
import com.redhat.cloud.notifications.db.Query;
import com.redhat.cloud.versioned.VersionedMethod;
import com.redhat.cloud.versioned.VersionedPath;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import java.util.HashMap;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@VersionedPath(path = "/api/notifications/$version/test-resource", sinceVersion = "1.0")
public class TestResource {

    @Produces(MediaType.APPLICATION_JSON)
    @VersionedPath(path = "/")
    @VersionedMethod(VersionedMethod.HttpMethod.POST)
    @RolesAllowed(ConsoleIdentityProvider.RBAC_READ_INTEGRATIONS_ENDPOINTS)
    @Operation(summary = "List endpoints", description = "Get a list of endpoints filtered down by the passed parameters.")
    @Parameters({
            @Parameter(
                    name = "limit",
                    in = ParameterIn.QUERY,
                    description = "Number of items per page. If the value is 0, it will return all elements",
                    schema = @Schema(type = SchemaType.INTEGER)
            ),
            @Parameter(
                    name = "pageNumber",
                    allowEmptyValue = true,
                    in = ParameterIn.QUERY,
                    description = "Page number. Starts at first page (0), if not specified starts at first page.",
                    schema = @Schema(type = SchemaType.INTEGER, maxLength = 200)
            )
    })
    public int sharedPost(@QueryParam("limit") int limit, @QueryParam("pageNumber") int pageNumber) {
        System.out.println("Shared post");
        return 1;
    }

    @Produces(MediaType.APPLICATION_JSON)
    @VersionedPath(path = "/", sinceVersion = "1.0")
    public int oldGet(@Context SecurityContext sec,
                       @BeanParam @Valid Query query,
                       @QueryParam("type") List<String> targetType,
                       @QueryParam("active") Boolean activeOnly,
                       @QueryParam("name") String name
    ) {
        System.out.println("Old get");
        return 1;
    }

    @Produces(MediaType.APPLICATION_JSON)
    @VersionedPath(path = "/", sinceVersion = "2.0")
    public int newGet() {
        System.out.println("New get");
        return 1;
    }

}
