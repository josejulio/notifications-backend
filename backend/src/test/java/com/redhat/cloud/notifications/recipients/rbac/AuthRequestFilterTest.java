package com.redhat.cloud.notifications.recipients.rbac;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

public class AuthRequestFilterTest {

    @BeforeEach
    public void clean() {
        System.setProperty("rbac.service-to-service.exceptional.auth.info", "");
    }

    @Test
    public void testServiceToServiceHeaders() {
        ClientRequestContext context = configureContext();

        AuthRequestFilter rbacAuthRequestFilter = new AuthRequestFilter();
        rbacAuthRequestFilter.application = "My nice app";
        rbacAuthRequestFilter.secret = "this-is-a-secret-token";

        try {
            rbacAuthRequestFilter.filter(context);
            MultivaluedMap<String, Object> map = context.getHeaders();
            Assertions.assertEquals("this-is-a-secret-token", context.getHeaderString("x-rh-rbac-psk"));
            Assertions.assertEquals("My nice app", context.getHeaderString("x-rh-rbac-client-id"));
            Assertions.assertNull(context.getHeaderString("Authorization"));
        } catch (IOException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    public void testDevServiceToServiceHeaders() {
        System.setProperty("rbac.service-to-service.exceptional.auth.info", "myuser:p4ssw0rd");
        ClientRequestContext context = configureContext();

        AuthRequestFilter rbacAuthRequestFilter = new AuthRequestFilter();
        rbacAuthRequestFilter.application = "My nice app";
        rbacAuthRequestFilter.secret = "this-is-a-secret-token";

        // Setting x-rh-rbac-account
        context.getHeaders().putSingle("x-rh-rbac-account", "the-account-id");

        try {
            rbacAuthRequestFilter.filter(context);
            Assertions.assertNull(context.getHeaderString("x-rh-rbac-psk"));
            Assertions.assertNull(context.getHeaderString("x-rh-rbac-client-id"));

            // Account is removed
            Assertions.assertNull(context.getHeaderString("x-rh-rbac-account"));
            Assertions.assertEquals(
                    "Basic " + Base64.getEncoder().encodeToString("myuser:p4ssw0rd".getBytes()),
                    context.getHeaderString("Authorization")
            );
        } catch (IOException ex) {
            Assertions.fail(ex);
        }
    }

    private ClientRequestContext configureContext() {
        ClientRequestContext context = Mockito.mock(ClientRequestContext.class);
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        Mockito.when(context.getHeaders()).thenReturn(map);
        Mockito.when(context.getHeaderString(Mockito.anyString())).then(invocationOnMock -> {
            Object o = map.get(invocationOnMock.getArgument(0));
            if (o instanceof List) {
                return ((List) o).get(0);
            }

            return o;
        });
        return context;
    }
}
