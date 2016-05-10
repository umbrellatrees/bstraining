package com.base.services

/**
 * Created by pawelp on 10.05.2016.
 */
import com.getbase.Client
import com.getbase.Configuration
import com.getbase.models.Contact
import com.getbase.models.User
import com.getbase.services.DealsService
import com.getbase.services.UsersService
import groovy.util.logging.Slf4j
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.boot.test.WebIntegrationTest
import spock.lang.Specification
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SpringApplicationConfiguration(classes = BsIntegrationSample.class)
@WebIntegrationTest(randomPort = true)
@Slf4j
class BsIntegrationSample extends Specification {

    Client bsClient

    def setup() {
        bsClient = new Client(new Configuration.Builder()
                .accessToken(AuthenticationProvider.AUTH_TOKEN)
                .verbose()
                .build())
    }

    def "should add new deal assigned to ACCOUNT_EMAIL when new contact is created"() {
        given:
        User salesRepPerson = bsClient.users()
                .list(new UsersService.SearchCriteria()
                .email(AuthenticationProvider.SALES_EMAIL)).get(0)

        User accountPerson = bsClient.users()
                .list(new UsersService.SearchCriteria()
                .email(AuthenticationProvider.ACCOUNT_EMAIL)).get(0)
        def suffix = LocalDateTime.now().format(DateTimeFormatter.ISO_TIME);
        Contact createdCompanyContact = companyForTest(salesRepPerson.id, suffix)
        Contact createdContact = contractToCompany(salesRepPerson.id, createdCompanyContact.id, suffix)

        sleep(80_000)
        expect:

            bsClient
                    .deals()
                    .list(
                    new DealsService.SearchCriteria()
                            .contactId(createdContact.id)
                            .ownerId(accountPerson.id)).size() == 1

    }

    private companyForTest(Long salesRepId, String suffix) {
        Contact companyContact = new Contact()
        companyContact.name = "Company $suffix"
        companyContact.firstName = "Company firstName $suffix"
        companyContact.lastName = "Company lastName $suffix"
        companyContact.ownerId = salesRepId
        bsClient
                .contacts()
                .create(companyContact)
    }

    private contractToCompany(Long salesRepId, Long companyId, String suffix) {
        Contact contact = new Contact()
        contact.name = "Contact name $suffix"
        contact.firstName = "Contact firstName $suffix"
        contact.lastName = "Contact lastName $suffix"
        contact.ownerId = salesRepId
        contact.contactId = companyId
        bsClient
                .contacts()
                .create(contact)
    }


}