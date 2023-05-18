package com.redhat.cloud.notifications.routers.internal;

import com.redhat.cloud.notifications.db.repositories.TemplateRepository;
import com.redhat.cloud.notifications.models.AggregationEmailTemplate;
import com.redhat.cloud.notifications.models.InstantEmailTemplate;
import com.redhat.cloud.notifications.models.Template;
import com.redhat.cloud.notifications.routers.models.RenderEmailTemplateRequest;
import com.redhat.cloud.notifications.routers.models.RenderEmailTemplateResponse;
import com.redhat.cloud.notifications.templates.TemplateEngineClient;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestPath;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

import static com.redhat.cloud.notifications.Constants.API_INTERNAL;
import static com.redhat.cloud.notifications.auth.ConsoleIdentityProvider.RBAC_INTERNAL_ADMIN;
import static com.redhat.cloud.notifications.auth.ConsoleIdentityProvider.RBAC_INTERNAL_USER;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path =API_INTERNAL + "/templates")
@RolesAllowed(RBAC_INTERNAL_ADMIN)
public class TemplateResource {

    @Inject
    TemplateRepository templateRepository;

    @Inject
    @RestClient
    TemplateEngineClient templateEngineClient;

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.POST)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Transactional
    @RolesAllowed(RBAC_INTERNAL_ADMIN)
    public Template createTemplate(@NotNull @Valid Template template) {
        return templateRepository.createTemplate(template);
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.GET)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(RBAC_INTERNAL_USER)
    public List<Template> getAllTemplates() {
        return templateRepository.findAllTemplates();
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.GET)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/{templateId}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed(RBAC_INTERNAL_USER)
    public Template getTemplate(@RestPath UUID templateId) {
        return templateRepository.findTemplateById(templateId);
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.PUT)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/{templateId}")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    @Transactional
    @RolesAllowed(RBAC_INTERNAL_ADMIN)
    public Response updateTemplate(@RestPath UUID templateId, Template template) {
        boolean updated = templateRepository.updateTemplate(templateId, template);
        if (updated) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.DELETE)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/{templateId}")
    @Transactional
    @RolesAllowed(RBAC_INTERNAL_ADMIN)
    public boolean deleteTemplate(@RestPath UUID templateId) {
        return templateRepository.deleteTemplate(templateId);
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.POST)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/email/instant")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Transactional
    @RolesAllowed(RBAC_INTERNAL_ADMIN)
    public InstantEmailTemplate createInstantEmailTemplate(@NotNull @Valid InstantEmailTemplate template) {
        return templateRepository.createInstantEmailTemplate(template);
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.GET)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/email/instant")
    @Produces(APPLICATION_JSON)
    @RolesAllowed(RBAC_INTERNAL_USER)
    public List<InstantEmailTemplate> getAllInstantEmailTemplates(@QueryParam("applicationId") UUID applicationId) {
        return templateRepository.findAllInstantEmailTemplates(applicationId);
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.GET)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/email/instant/eventType/{eventTypeId}")
    @RolesAllowed(RBAC_INTERNAL_USER)
    @APIResponses(value = {
        @APIResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = InstantEmailTemplate.class))),
        @APIResponse(responseCode = "404", description = "No instant email found for the event type", content = @Content(mediaType = TEXT_PLAIN, schema = @Schema(type = SchemaType.STRING)))
    })
    public InstantEmailTemplate getInstantEmailTemplateByEventType(@RestPath UUID eventTypeId) {
        return templateRepository.findInstantEmailTemplateByEventType(eventTypeId);
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.GET)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/email/instant/{templateId}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed(RBAC_INTERNAL_USER)
    public InstantEmailTemplate getInstantEmailTemplate(@RestPath UUID templateId) {
        return templateRepository.findInstantEmailTemplateById(templateId);
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.PUT)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/email/instant/{templateId}")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    @Transactional
    @RolesAllowed(RBAC_INTERNAL_ADMIN)
    public Response updateInstantEmailTemplate(@RestPath UUID templateId, @NotNull @Valid InstantEmailTemplate template) {
        boolean updated = templateRepository.updateInstantEmailTemplate(templateId, template);
        if (updated) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.DELETE)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/email/instant/{templateId}")
    @Transactional
    @RolesAllowed(RBAC_INTERNAL_ADMIN)
    public boolean deleteInstantEmailTemplate(@RestPath UUID templateId) {
        return templateRepository.deleteInstantEmailTemplate(templateId);
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.POST)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/email/aggregation")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Transactional
    @RolesAllowed(RBAC_INTERNAL_ADMIN)
    public AggregationEmailTemplate createAggregationEmailTemplate(@NotNull @Valid AggregationEmailTemplate template) {
        return templateRepository.createAggregationEmailTemplate(template);
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.GET)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/email/aggregation")
    @Produces(APPLICATION_JSON)
    @RolesAllowed(RBAC_INTERNAL_USER)
    public List<AggregationEmailTemplate> getAllAggregationEmailTemplates() {
        return templateRepository.findAllAggregationEmailTemplates();
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.GET)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/email/aggregation/application/{appId}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed(RBAC_INTERNAL_USER)
    public List<AggregationEmailTemplate> getAggregationEmailTemplatesByApplication(@RestPath UUID appId) {
        return templateRepository.findAggregationEmailTemplatesByApplication(appId);
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.GET)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/email/aggregation/{templateId}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed(RBAC_INTERNAL_USER)
    public AggregationEmailTemplate getAggregationemailTemplate(@RestPath UUID templateId) {
        return templateRepository.findAggregationEmailTemplateById(templateId);
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.PUT)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/email/aggregation/{templateId}")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    @Transactional
    @RolesAllowed(RBAC_INTERNAL_ADMIN)
    public Response updateAggregationEmailTemplate(@RestPath UUID templateId, @NotNull @Valid AggregationEmailTemplate template) {
        boolean updated = templateRepository.updateAggregationEmailTemplate(templateId, template);
        if (updated) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.DELETE)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/email/aggregation/{templateId}")
    @Transactional
    @RolesAllowed(RBAC_INTERNAL_ADMIN)
    public boolean deleteAggregationEmailTemplate(@RestPath UUID templateId) {
        return templateRepository.deleteAggregationEmailTemplate(templateId);
    }

    @com.redhat.cloud.versioned.VersionedMethod(com.redhat.cloud.versioned.VersionedMethod.HttpMethod.POST)
    @com.redhat.cloud.versioned.VersionedPath(sinceVersion= "1.0", path ="/email/render")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @APIResponses(value = {
        @APIResponse(responseCode = "200", content = {
            @Content(schema = @Schema(title = "RenderEmailTemplateResponseSuccess", implementation = RenderEmailTemplateResponse.Success.class))
        }),
        @APIResponse(responseCode = "400", content = {
            @Content(schema = @Schema(title = "RenderEmailTemplateResponseError", implementation = RenderEmailTemplateResponse.Error.class))
        })
    })
    @RolesAllowed(RBAC_INTERNAL_USER)
    public Response renderEmailTemplate(@NotNull @Valid RenderEmailTemplateRequest renderEmailTemplateRequest) {
        try {
            return templateEngineClient.render(renderEmailTemplateRequest);
        } catch (BadRequestException e) {
            // The following line is required to forward the HTTP 400 error message.
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}
