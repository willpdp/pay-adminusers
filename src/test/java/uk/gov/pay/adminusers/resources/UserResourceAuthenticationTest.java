package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.pay.adminusers.fixtures.UserDbFixture;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.service.PasswordHasher;

import java.util.UUID;

import static com.jayway.restassured.http.ContentType.JSON;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.adminusers.fixtures.ServiceDbFixture.serviceDbFixture;

public class UserResourceAuthenticationTest extends IntegrationTest {

    @Test
    public void shouldAuthenticateUser_onAValidUsernamePasswordCombination() throws Exception {

        String[] gatewayAccountIds = new String[]{"1", "2"};
        Service service = serviceDbFixture(databaseHelper).withGatewayAccountIds(gatewayAccountIds).insertService();

        String username = createAValidUser(service);

        ImmutableMap<Object, Object> authPayload = ImmutableMap.builder()
                .put("username", username)
                .put("password", "password-" + username)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(authPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_AUTHENTICATE_URL)
                .then()
                .statusCode(200)
                .body("username", is(username))
                .body("email", is("user-" + username + "@example.com"))
                .body("service_roles", hasSize(1))
                .body("service_roles[0].service.external_id", is(notNullValue()))
                .body("service_roles[0].service.name", is(notNullValue()))
                .body("telephone_number", is("45334534634"))
                .body("otp_key", is("34f34"))
                .body("login_counter", is(0))
                .body("disabled", is(false))
                .body("_links", hasSize(1))
                .body("service_roles[0].role.name", is("admin"))
                .body("service_roles[0].role.permissions", hasSize(31)); //we could consider removing this assertion if the permissions constantly changing
    }

    @Test
    public void shouldAuthenticateUser_onAValidUsernamePasswordCombination_whenUserDoesNotBelongToAService() throws Exception {

        String username = randomAlphanumeric(10) + "example.com";
        String password = "password-" + username;
        String encryptedPassword = (new PasswordHasher()).hash(password);

        UserDbFixture.userDbFixture(databaseHelper)
                .withUsername(username)
                .withPassword(encryptedPassword).insertUser();

        ImmutableMap<Object, Object> authPayload = ImmutableMap.builder()
                .put("username", username)
                .put("password", password)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(authPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_AUTHENTICATE_URL)
                .then()
                .statusCode(200)
                .body("username", is(username))
                .body("gateway_account_ids", hasSize(0))
                .body("service_ids", hasSize(0))
                .body("services", hasSize(0))
                .body("_links", hasSize(1))
                .body("role", is(nullValue()))
                .body("permissions", hasSize(0)); //we could consider removing this assertion if the permissions constantly changing
    }

    @Test
    public void shouldAuthenticateFail_onAInvalidUsernamePasswordCombination() throws Exception {

        String[] gatewayAccountIds = new String[]{"3", "4"};
        Service service = serviceDbFixture(databaseHelper).withGatewayAccountIds(gatewayAccountIds).insertService();

        String username = createAValidUser(service);

        ImmutableMap<Object, Object> authPayload = ImmutableMap.builder()
                .put("username", username)
                .put("password", "invalid-password")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(authPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_AUTHENTICATE_URL)
                .then()
                .statusCode(401)
                .body("errors", hasSize(1))
                .body("errors[0]", is("invalid username and/or password"));

    }

    private String createAValidUser(Service service) throws JsonProcessingException {

        String username = randomAlphanumeric(10) + UUID.randomUUID();
        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("password", "password-" + username)
                .put("email", "user-" + username + "@example.com")
                .put("service_external_ids", new String[]{service.getExternalId()})
                .put("telephone_number", "45334534634")
                .put("otp_key", "34f34")
                .put("role_name", "admin")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(userPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then()
                .statusCode(201);

        return username;
    }
}
