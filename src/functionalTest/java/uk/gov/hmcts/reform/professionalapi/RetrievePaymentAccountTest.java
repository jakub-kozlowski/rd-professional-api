package uk.gov.hmcts.reform.professionalapi;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.professionalapi.controller.request.NewUserCreationRequest.aNewUserCreationRequest;
import static uk.gov.hmcts.reform.professionalapi.controller.request.UserCreationRequest.aUserCreationRequest;
import static uk.gov.hmcts.reform.professionalapi.helper.OrganisationFixtures.someMinimalOrganisationRequest;

import io.restassured.specification.RequestSpecification;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.professionalapi.controller.request.NewUserCreationRequest;
import uk.gov.hmcts.reform.professionalapi.controller.request.OrganisationCreationRequest;
import uk.gov.hmcts.reform.professionalapi.controller.request.UserCreationRequest;

@RunWith(SpringIntegrationSerenityRunner.class)
@WithTags({@WithTag("testType:Functional")})
@ActiveProfiles("functional")
@Slf4j
public class RetrievePaymentAccountTest extends AuthorizationFunctionalTest {

    private RequestSpecification bearerTokenForUser;
    private String orgIdentifier;
    private String email = generateRandomEmail();
    private String lastName = "someLastName";
    private String firstName = "someName";

    public RequestSpecification generateBearerTokenForUser(String roleUnderTest, String... otherRoles) {
        Map<String, Object> response = professionalApiClient.createOrganisation();
        orgIdentifier = (String) response.get("organisationIdentifier");
        professionalApiClient.updateOrganisation(orgIdentifier, hmctsAdmin);

        List<String> userRoles = Arrays.stream(otherRoles).collect(Collectors.toList());
        userRoles.add(roleUnderTest);

        bearerTokenForUser = professionalApiClient.getMultipleAuthHeadersExternal(roleUnderTest, firstName, lastName,
                email);

        NewUserCreationRequest userCreationRequest = aNewUserCreationRequest()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .roles(userRoles)
                .build();

        professionalApiClient.addNewUserToAnOrganisation(orgIdentifier, hmctsAdmin, userCreationRequest,
                HttpStatus.CREATED);

        return bearerTokenForUser;
    }


    @Test
    public void can_retrieve_active_organisation_payment_accounts_user_by_email() {

        Set<String> paymentAccounts = new HashSet<>();
        paymentAccounts.add("PBA" + randomAlphabetic(7));

        OrganisationCreationRequest request = someMinimalOrganisationRequest()
                .paymentAccount(paymentAccounts)
                .superUser(createUserForTest())
                .build();

        Map<String, Object> response =  professionalApiClient.createOrganisation(request);
        String orgIdentifierResponse = (String) response.get("organisationIdentifier");
        assertThat(orgIdentifierResponse).isNotEmpty();
        request.setStatus("ACTIVE");
        professionalApiClient.updateOrganisation(request, hmctsAdmin, orgIdentifierResponse);
        Map<String, Object> orgResponse = professionalApiClient.retrievePaymentAccountsByEmail(email.toLowerCase(),
                hmctsAdmin);
        assertThat(orgResponse).isNotEmpty();
        responseValidate(orgResponse);
    }

    @Test
    public void can_retrieve_active_organisation_payment_accounts_user_by_email_fromHeader() {

        Set<String> paymentAccounts = new HashSet<>();
        paymentAccounts.add("PBA" + randomAlphabetic(7));

        OrganisationCreationRequest request = someMinimalOrganisationRequest()
                .paymentAccount(paymentAccounts)
                .superUser(createUserForTest())
                .build();

        Map<String, Object> response =  professionalApiClient.createOrganisation(request);
        String orgIdentifierResponse = (String) response.get("organisationIdentifier");
        assertThat(orgIdentifierResponse).isNotEmpty();
        request.setStatus("ACTIVE");
        professionalApiClient.updateOrganisation(request, hmctsAdmin, orgIdentifierResponse);
        Map<String, Object> orgResponse = professionalApiClient
                .retrievePaymentAccountsByEmailFromHeader(email.toLowerCase(), hmctsAdmin);
        assertThat(orgResponse).isNotEmpty();
        responseValidate(orgResponse);
    }

    private void responseValidate(Map<String, Object> orgResponse) {

        orgResponse.forEach((k,v) -> {

            if ("organisationIdentifier".equals(k) && "http_status".equals(k)
                    && "name".equals(k) &&  "status".equals(k)
                    && "superUser".equals(k) && "paymentAccount".equals(k)) {

                Assertions.assertThat(v.toString()).isNotEmpty();
                Assertions.assertThat(v.toString().contains("Ok"));
                Assertions.assertThat(v.toString().contains("some-org-name"));
                Assertions.assertThat(v.toString().equals("ACTIVE"));
                Assertions.assertThat(v.toString()).isNotEmpty();
            }

        });

    }

    private UserCreationRequest createUserForTest() {
        UserCreationRequest user = aUserCreationRequest()
                .firstName("some-fname")
                .lastName("some-lname")
                .email(email)
                .build();
        return user;
    }

}
