package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import uk.gov.pay.adminusers.model.User;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;

public class UserResourceCreateAndGetTest extends UserResourceTestBase {

    @Test
    public void shouldCreateAUserWithSortedGatewayAccountIdsArraySuccessfully() throws Exception {

        String username = randomAlphanumeric(10) + randomUUID().toString();
        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("email", "user-" + username + "@example.com")
                .put("gateway_account_ids", new String[]{"111111", "222"})
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
                .statusCode(201)
                .body("id", nullValue())
                .body("username", is(username))
                .body("password", nullValue())
                .body("email", is("user-" + username + "@example.com"))
                .body("gateway_account_ids", hasSize(2))
                .body("gateway_account_ids[0]", is("222"))
                .body("gateway_account_ids[1]", is("111111"))
                .body("telephone_number", is("45334534634"))
                .body("otp_key", is("34f34"))
                .body("login_counter", is(0))
                .body("disabled", is(false))
                .body("_links", hasSize(1))
                .body("_links[0].href", is("http://localhost:8080/v1/api/users/" + username))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("self"))
                .body("role.name", is("admin"))
                .body("role.description", is("Administrator"))
                .body("permissions", hasSize(30)); //we could consider removing this assertion if the permissions constantly changing

        //TODO - WIP This will be removed when PP-1612 is done.
        // This is an extra check to verify that new created user gateways are registered withing the new Services Model as well as in users table
        List<Map<String, Object>> userByName = databaseTestHelper.findUserByName(username);
        List<Map<String, Object>> servicesAssociatedToUser = databaseTestHelper.findUserServicesByUserId((Integer) userByName.get(0).get("id"));
        assertThat(servicesAssociatedToUser.size(), is(1));
    }

    @Test
    public void shouldCreateAUserWithinAServiceIfServiceIdIsInPayloadIgnoringGatewayAccountIds() throws Exception {

        String username = randomAlphanumeric(10) + randomUUID().toString();
        databaseTestHelper.addService(666, "1", "2");

        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("email", "user-" + username + "@example.com")
                .put("gateway_account_ids", new String[]{"55"})
                .put("service_ids", new String[]{"666"})
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
                .statusCode(201)
                .body("id", nullValue())
                .body("username", is(username))
                .body("password", nullValue())
                .body("email", is("user-" + username + "@example.com"))
                .body("gateway_account_ids", hasSize(2))
                .body("gateway_account_ids[0]", is("1"))
                .body("gateway_account_ids[1]", is("2"))
                .body("telephone_number", is("45334534634"))
                .body("otp_key", is("34f34"))
                .body("login_counter", is(0))
                .body("disabled", is(false))
                .body("_links", hasSize(1))
                .body("_links[0].href", is("http://localhost:8080/v1/api/users/" + username))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("self"))
                .body("role.name", is("admin"))
                .body("role.description", is("Administrator"))
                .body("permissions", hasSize(30)); //we could consider removing this assertion if the permissions constantly changing

        //TODO - WIP This will be removed when PP-1612 is done.
        // This is an extra check to verify that new created user gateways are registered withing the new Services Model as well as in users table
        List<Map<String, Object>> userByName = databaseTestHelper.findUserByName(username);
        List<Map<String, Object>> servicesAssociatedToUser = databaseTestHelper.findUserServicesByUserId((Integer) userByName.get(0).get("id"));
        assertThat(servicesAssociatedToUser.size(), is(1));
    }

    @Test
    public void shouldFailCreatingAUserForAServiceThatDoesNotExist() throws Exception {

        String username = randomAlphanumeric(10) + randomUUID().toString();

        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("email", "user-" + username + "@example.com")
                .put("gateway_account_ids", new String[]{"55"})
                .put("service_ids", new String[]{"123"})
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
                .statusCode(400);
    }

    @Test
    public void shouldAddUserToAServiceWhenCreatingTheUserWithAnAlreadyExistingGatewayAccount() throws Exception {

        int serviceId = 123;
        String gatewayAccount = "666";
        databaseTestHelper.addService(serviceId, gatewayAccount);
        String username = randomAlphanumeric(10) + randomUUID().toString();
        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("email", "user-" + username + "@example.com")
                .put("gateway_account_ids", new String[]{gatewayAccount})
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
                .statusCode(201)
                .body("username", is(username))
                .body("gateway_account_ids", hasSize(1))
                .body("gateway_account_ids[0]", is(gatewayAccount));

        //TODO - WIP PP-1483 This will be amended when the story is done.
        // This is an extra check to verify that new created user gateways are registered withing the new Services Model as well as in users table
        List<Map<String, Object>> userByName = databaseTestHelper.findUserByName(username);
        List<Map<String, Object>> servicesAssociatedToUser = databaseTestHelper.findUserServicesByUserId((Integer) userByName.get(0).get("id"));
        assertThat(servicesAssociatedToUser.size(), is(1));
        assertThat(servicesAssociatedToUser.get(0).get("service_id"), is(123));

        List<Map<String, Object>> gatewayAccountsAssociatedToUser = databaseTestHelper.findGatewayAccountsByService(((Integer) servicesAssociatedToUser.get(0).get("service_id")).longValue());
        assertThat(gatewayAccountsAssociatedToUser.size(), is(1));
        assertThat(gatewayAccountsAssociatedToUser.get(0).get("gateway_account_id"), is(gatewayAccount));
    }

    @Test
    public void shouldError400_IfRoleDoesNotExist() throws Exception {
        String username = randomAlphanumeric(10) + randomUUID().toString();
        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("email", "user-" + username + "@example.com")
                .put("gateway_account_ids", new String[]{"1", "2"})
                .put("telephone_number", "45334534634")
                .put("otp_key", "34f34")
                .put("role_name", "invalid-role")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(userPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then()
                .statusCode(400)
                .body("errors", hasSize(1))
                .body("errors[0]", is("role [invalid-role] not recognised"));
    }

    @Test
    public void shouldError400_whenFieldsMissingForUserCreation() throws Exception {
        ImmutableMap<Object, Object> invalidPayload = ImmutableMap.builder().build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invalidPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then()
                .statusCode(400)
                .body("errors", hasSize(5))
                .body("errors", hasItems(
                        "Field [username] is required",
                        "Field [email] is required",
                        "Field [gateway_account_ids] is required",
                        "Field [telephone_number] is required",
                        "Field [role_name] is required"));
    }

    @Test
    public void shouldError409_IfUsernameAlreadyExists() throws Exception {

        String username = randomAlphanumeric(10) + randomUUID().toString();
        String gatewayAccountId = "3";
        User user = User.from(randomInt(), username, "password", "user-" + username + "@example.com", Arrays.asList(gatewayAccountId), newArrayList(), "otpKey", "3543534");
        int serviceId = RandomUtils.nextInt();
        databaseTestHelper.addService(serviceId, gatewayAccountId);
        databaseTestHelper.add(user);
        //add servicerole

        ImmutableMap<Object, Object> userPayload = ImmutableMap.builder()
                .put("username", username)
                .put("email", "user-" + username + "@example.com")
                .put("gateway_account_ids", new String[]{gatewayAccountId})
                .put("telephone_number", "45334534634")
                .put("role_name", "admin")
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(userPayload))
                .contentType(JSON)
                .accept(JSON)
                .post(USERS_RESOURCE_URL)
                .then()
                .statusCode(409)
                .body("errors", hasSize(1))
                .body("errors[0]", is(format("username [%s] already exists", username)));
    }

    @Test
    public void shouldReturn404_whenGetUserWithNonExistentUsername() throws Exception {

        givenSetup()
                .when()
                .accept(JSON)
                .get(format(USER_RESOURCE_URL, "non-existent-user"))
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldReturn404_whenGetUser_withInvalidMaxLengthUsername() throws Exception {

        givenSetup()
                .when()
                .accept(JSON)
                .get(format(USER_RESOURCE_URL, RandomStringUtils.randomAlphanumeric(256)))
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldReturnUser_whenGetUserWithUsername() throws Exception {

        String username = createAValidUser();

        givenSetup()
                .when()
                .contentType(JSON)
                .accept(JSON)
                .get(format(USER_RESOURCE_URL, username))
                .then()
                .statusCode(200)
                .body("username", is(username))
                .body("password", nullValue())
                .body("email", is("user-" + username + "@example.com"))
                .body("gateway_account_ids", hasSize(2))
                .body("gateway_account_ids[0]", is("1"))
                .body("gateway_account_ids[1]", is("2"))
                .body("telephone_number", is("45334534634"))
                .body("otp_key", is("34f34"))
                .body("login_counter", is(0))
                .body("disabled", is(false))
                .body("_links", hasSize(1))
                .body("_links[0].href", is("http://localhost:8080/v1/api/users/" + username))
                .body("_links[0].method", is("GET"))
                .body("_links[0].rel", is("self"))
                .body("role.name", is("admin"))
                .body("role.description", is("Administrator"))
                .body("permissions", hasSize(30)); //we could consider removing this assertion if the permissions constantly changing
    }
}
