package com.redhat.cloud.notifications.routers;

import com.redhat.cloud.notifications.auth.RhIdPrincipal;
import com.redhat.cloud.notifications.models.Notification;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

@Path("/notifications")
public class NotificationService {

    @Inject
    Vertx vertx;

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Path("/updates")
    public Multi<Notification> getNotificationUpdates(@Context SecurityContext sec) {
        // TODO Check the Notification type if we want something else
        // TODO Process:
        //      - Fetch the last unread notifications (with some limit?)
        //         - If we're not previously subscribed, add the last n-days of notifications to our unread list?
        //          - Add subscription to our subscription base to receive future notifications
        //      - Subscribe to VertX eventBus listening for: notifications<tenantId><userId>
        return vertx.eventBus().consumer(getAddress(sec.getUserPrincipal()))
                .toMulti()
                // TODO Verify that toMulti subscribes to a hot stream, not cold!
                .onItem()
                .transform(m -> (Notification) m.body());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> markRead(@Context SecurityContext sec, Integer id) {
        // Mark the notification id for <tenantId><userId> 's subscription as read
    }

    // TODO Mark all as read?

    // TODO DB structure? <tenantId><userId><notificationId><read> ? Will we show old read-messages or not? Do we vacuum old items from the subscriptions?

    private String getAddress(Principal principal) {
        // TODO This should call some global point which is used by the Processor interface to push data to the same queue names
        RhIdPrincipal rhUser = (RhIdPrincipal) principal;
        return String.format("notifications-%s", rhUser.getAccount());
    }
}
