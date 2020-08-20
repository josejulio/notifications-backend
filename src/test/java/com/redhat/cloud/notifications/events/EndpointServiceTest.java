package com.redhat.cloud.notifications.events;

import com.redhat.cloud.notifications.MockServerConfig;
import com.redhat.cloud.notifications.MockServerClientConfig;
import com.redhat.cloud.notifications.TestHelpers;
import com.redhat.cloud.notifications.TestLifecycleManager;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
public class EndpointServiceTest {

    @MockServerConfig
    MockServerClientConfig mockServerConfig;

    @Test
    void testEndpointAdding() {
        String tenant = "empty";
        String userName = "user";
        String identityHeaderValue = TestHelpers.encodeIdentityInfo(tenant, userName);
        Header identityHeader = TestHelpers.createIdentityHeader(identityHeaderValue);

        mockServerConfig.addMockRbacAccess(identityHeaderValue, MockServerClientConfig.RbacAccess.FULL_ACCESS);

        // Test empty tenant
        given()
                // Set header to x-rh-identity
                .header(identityHeader)
                .when().get("/endpoints")
                .then()
                .statusCode(200)
                .body(is("[]"));

        // Add new endpoints

        // Fetch single endpoint and endpoints

        // Enable, fetch, disable, fetch

        // Delete, fetch
    }
}