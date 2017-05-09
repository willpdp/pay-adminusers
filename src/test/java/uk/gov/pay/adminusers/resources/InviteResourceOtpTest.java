package uk.gov.pay.adminusers.resources;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.http.ContentType;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.adminusers.fixtures.InviteDbFixture;

import java.util.List;
import java.util.Map;

import static com.google.common.io.BaseEncoding.base32;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.newId;

public class InviteResourceOtpTest extends IntegrationTest {

    private String code;

    private static final String OTP_KEY = newId();
    private static final int PASSCODE = new GoogleAuthenticator().getTotpPassword(base32().encode(OTP_KEY.getBytes()));
    private static final String EMAIL = "invited-" + random(5) + "@example.com";
    private static final String TELEPHONE_NUMBER = "+447999999999";
    private static final String PASSWORD = "a-secure-password";

    @Before
    public void givenAnExistingInvite() {
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .expired()
                .insertInvite();
    }

    @Test
    public void generateOtp_shouldSucceed_evenWhenTokenIsExpired_sinceItShouldBeValidatedOnGetInvite() throws Exception {

        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("code", code)
                .put("telephone_number", TELEPHONE_NUMBER)
                .put("password", PASSWORD)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(INVITES_GENERATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(OK.getStatusCode());
    }

    @Test
    public void generateOtp_shouldFail_whenInviteDoesNotExist() throws Exception {

        ImmutableMap<Object, Object> invitationRequest = ImmutableMap.builder()
                .put("code", "not-existing-code")
                .put("telephone_number", TELEPHONE_NUMBER)
                .put("password", PASSWORD)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationRequest))
                .contentType(ContentType.JSON)
                .post(INVITES_GENERATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void validateOtp_shouldCreateUserWhenValidOtp() throws Exception {

        // create an invitation
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .withTelephoneNumber(TELEPHONE_NUMBER)
                .withPassword(PASSWORD)
                .expired()
                .insertInvite();

        // generate valid invitationOtpRequest and execute it
        ImmutableMap<Object, Object> invitationOtpRequest = ImmutableMap.builder()
                .put("code", code)
                .put("otp", PASSCODE)
                .build();

        assertThat(databaseHelper.findInviteByCode(code).size(), is(1));

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationOtpRequest))
                .contentType(ContentType.JSON)
                .post(INVITES_VALIDATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(OK.getStatusCode());

        // check if the user has been created and it is not disabled
        List<Map<String, Object>> users = databaseHelper.findUserByUsername(EMAIL);

        assertThat(users.size(), is(1));

        Map<String, Object> createdUser = users.get(0);
        assertThat(createdUser.get("username"), is(EMAIL));
        assertThat(createdUser.get("email"), is(EMAIL));
        assertThat(createdUser.get("password"), is(PASSWORD));
        assertThat(createdUser.get("disabled"), is(false));
    }

    @Test
    public void validateOtp_shouldFail_whenInvalidCode() throws Exception {

        ImmutableMap<Object, Object> invitationOtpRequest = ImmutableMap.builder()
                .put("code", "non-existent-code")
                .put("otp", PASSCODE)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationOtpRequest))
                .contentType(ContentType.JSON)
                .post(INVITES_VALIDATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void validateOtp_shouldFail_whenInvalidOtpAuthCode() throws Exception {

        // create an invitation
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .withTelephoneNumber(TELEPHONE_NUMBER)
                .withPassword(PASSWORD)
                .expired()
                .insertInvite();

        // generate invalid invitationOtpRequest and execute it
        ImmutableMap<Object, Object> invitationOtpRequest = ImmutableMap.builder()
                .put("code", code)
                .put("otp", 123456)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(invitationOtpRequest))
                .contentType(ContentType.JSON)
                .post(INVITES_VALIDATE_OTP_RESOURCE_URL)
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    public void resendOtp_shouldUpdateTelephoneNumber_whenValidOtp() throws Exception {

        // create an invitation with initial telephone number
        String initialTelephoneNumber = "+447451111111";
        code = InviteDbFixture.inviteDbFixture(databaseHelper)
                .withEmail(EMAIL)
                .withOtpKey(OTP_KEY)
                .withTelephoneNumber(initialTelephoneNumber)
                .withPassword(PASSWORD)
                .expired()
                .insertInvite();

        // generate new invitationOtpRequest with new telephone number
        String newTelephoneNumber = "+447452222222";
        ImmutableMap<Object, Object> resendRequest = ImmutableMap.builder()
                .put("code", code)
                .put("telephone_number", newTelephoneNumber)
                .build();

        givenSetup()
                .when()
                .body(mapper.writeValueAsString(resendRequest))
                .contentType(ContentType.JSON)
                .post(INVITES_RESEND_OTP_RESOURCE_URL)
                .then()
                .statusCode(OK.getStatusCode());

        // check if we are using the newTelephoneNumber in the invitation
        List<Map<String, Object>> foundInvites = databaseHelper.findInviteByCode(code);
        assertThat(foundInvites.size(), is(1));
        Map<String, Object> foundInvite = foundInvites.get(0);
        assertThat(foundInvite.get("telephone_number"), is(newTelephoneNumber));
    }
}
