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
import static com.jayway.awaitility.Awaitility.await
import static java.lang.Thread.sleep
import static java.util.concurrent.TimeUnit.SECONDS

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

        def suffix = LocalDateTime.now().format(DateTimeFormatter.ISO_TIME);
        Contact createdCompanyContact = companyForTest(salesRepPerson.id, suffix)

        expect:
        await().atMost(15, SECONDS).until {
            bsClient
                    .deals()
                    .list(
                    new DealsService.SearchCriteria()
                            .contactId(createdCompanyContact.id)
                            .ownerId(salesRepPerson.id)).size() == 1
        }

    }

    private companyForTest(Long salesRepId, String suffix) {
        Contact companyContact = new Contact()
        companyContact.name = "Company $suffix"
        companyContact.firstName = "Company firstName $suffix"
        companyContact.lastName = "Company lastName $suffix"
        companyContact.ownerId = salesRepId
        companyContact.setIsOrganization(true)
        bsClient
                .contacts()
                .create(companyContact)
    }
}