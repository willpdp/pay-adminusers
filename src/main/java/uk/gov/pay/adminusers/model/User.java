package uk.gov.pay.adminusers.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class User {

    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_SERVICE_EXTERNAL_ID = "service_external_id";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_GATEWAY_ACCOUNT_IDS = "gateway_account_ids";
    public static final String FIELD_TELEPHONE_NUMBER = "telephone_number";
    public static final String FIELD_ROLE_NAME = "role_name";

    private Integer id;
    private String externalId;
    private String username;
    private String password;
    private String email;
    private List<String> gatewayAccountIds = new ArrayList<>();
    private String telephoneNumber;
    @Deprecated // Use serviceRoles instead
    private List<Service> services = new ArrayList<>();
    private String otpKey;
    private Boolean disabled = Boolean.FALSE;
    private Integer loginCounter = 0;
    private List<ServiceRole> serviceRoles = new ArrayList<>();
    @Deprecated // Use serviceRoles instead
    private List<Role> roles = new ArrayList<>();
    private List<Link> links = new ArrayList<>();
    private Integer sessionVersion = 0;

    public static User from(Integer id, String externalId, String username, String password, String email,
                            List<String> gatewayAccountIds, List<Service> services, String otpKey, String telephoneNumber, List<ServiceRole> serviceRoles) {
        return new User(id, externalId, username, password, email, gatewayAccountIds, services, otpKey, telephoneNumber, serviceRoles);
    }

    private User(Integer id, @JsonProperty("external_id") String externalId, @JsonProperty("username") String username, @JsonProperty("password") String password,
                 @JsonProperty("email") String email, @JsonProperty("gateway_account_ids") List<String> gatewayAccountIds,
                 List<Service> services, @JsonProperty("otp_key") String otpKey, @JsonProperty("telephone_number") String telephoneNumber,
                 @JsonProperty("service_roles") List<ServiceRole> serviceRoles) {
        this.id = id;
        this.externalId = externalId;
        this.username = username;
        this.password = password;
        this.email = email;
        this.gatewayAccountIds = gatewayAccountIds;
        this.services = services;
        this.otpKey = otpKey;
        this.telephoneNumber = telephoneNumber;
        this.serviceRoles = serviceRoles;
    }

    @JsonIgnore
    public Integer getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getUsername() {
        return username;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getGatewayAccountIds() {
        return gatewayAccountIds;
    }

    public String getOtpKey() {
        return otpKey;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    @JsonGetter
    public Boolean isDisabled() {
        return disabled;
    }

    public Integer getLoginCounter() {
        return loginCounter;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public void setLoginCounter(Integer loginCounter) {
        this.loginCounter = loginCounter;
    }

    public void setSessionVersion(Integer sessionVersion) {
        this.sessionVersion = sessionVersion;
    }

    public Integer getSessionVersion() {
        return sessionVersion;
    }

    public List<Service> getServices() {
        return services;
    }

    public List<String> getServiceIds() {
        return services.stream().map(service -> String.valueOf(service.getId())).collect(Collectors.toList());
    }

    /**
     * We've agreed that given we are currently supporting only 1 role per user we will not json output a list of 1 role
     * instead the flat role attribute below.
     *
     * @return
     */
    @JsonIgnore
    public List<Role> getRoles() {
        return roles;
    }

    /**
     * Only for the json payload as described above
     *
     * @return
     */
    @JsonProperty("role")
    public Role getRole() {
        return !roles.isEmpty() ? roles.get(0) : null;
    }

    @JsonProperty("permissions")
    public List<String> getPermissions() {
        return !roles.isEmpty() ?
                roles.stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getName)
                        .collect(Collectors.toList())
                : emptyList();
    }


    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    @JsonProperty("_links")
    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    /**
     * its probably not a good idea to toString() password / otpKey
     *
     * @return
     */
    @Override
    public String toString() {
        return "User{" +
                "externalId=" + externalId +
                ", gatewayAccountIds=[" + String.join(", ", gatewayAccountIds) + ']' +
                ", disabled=" + disabled +
                ", serviceRoles=" + serviceRoles +
                '}';
    }

    public List<ServiceRole> getServiceRoles() {
        return serviceRoles;
    }
}
