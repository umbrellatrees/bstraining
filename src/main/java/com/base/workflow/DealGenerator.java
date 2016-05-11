package com.base.workflow;

/**
 * Created by pawelp on 11.05.2016.
 */

import com.base.services.AuthenticationProvider;
import com.getbase.services.DealsService;
import com.getbase.services.UsersService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import com.getbase.Client;
import com.getbase.models.Account;
import com.getbase.models.Address;
import com.getbase.models.AssociatedContact;
import com.getbase.models.Contact;
import com.getbase.models.Deal;
import com.getbase.models.Lead;
import com.getbase.models.LossReason;
import com.getbase.models.Note;
import com.getbase.models.Pipeline;
import com.getbase.models.Source;
import com.getbase.models.Stage;
import com.getbase.models.Tag;
import com.getbase.models.Task;
import com.getbase.models.User;
import com.getbase.sync.Meta;
import com.getbase.sync.Sync;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;


@Configuration
@EnableScheduling
public class DealGenerator {

    private Client client;
    private User salesRepPerson;

    @Value("${AUTH_TOKEN}")
    private String AUTH_TOKEN;

    @Value("${SALES_EMAIL}")
    private String SALES_EMAIL;

    @Value("${DEVICE_UUID}")
    private String DEVICE_UUID;

    @PostConstruct
    private void initPersons() {
        if (client == null) {
            client = new Client(new com.getbase.Configuration.Builder()
                    .accessToken(AUTH_TOKEN)
                    .verbose()
                    .build());
        }

        if (salesRepPerson == null) {
            salesRepPerson = client.users()
                    .list(new UsersService.SearchCriteria()
                            .email(SALES_EMAIL)).get(0);
        }
    }

    @Scheduled(fixedDelay = 15000)
    public void checkForCompanyUpdates() {

        new Sync(client, DEVICE_UUID)
                .subscribe(Contact.class, this::processContact)
                .subscribe(Account.class, (meta, account) -> true)
                .subscribe(Address.class, (meta, address) -> true)
                .subscribe(AssociatedContact.class, (meta, associatedContact) -> true)
                .subscribe(Deal.class, (meta, deal) -> true)
                .subscribe(LossReason.class, (meta, lossReason) -> true)
                .subscribe(Note.class, (meta, note) -> true)
                .subscribe(Pipeline.class, (meta, pipeline) -> true)
                .subscribe(Source.class, (meta, source) -> true)
                .subscribe(Stage.class, (meta, stage) -> true)
                .subscribe(Tag.class, (meta, tag) -> true)
                .subscribe(Task.class, (meta, task) -> true)
                .subscribe(User.class, (meta, user) -> true)
                .subscribe(Lead.class, (meta, lead) -> true)
                .fetch();

    }

    private boolean processContact(Meta meta, Contact contact) {

        if (isCreated(meta) &&
                isCompany(contact) &&
                isOwnedBySalesRep(contact.getOwnerId()) &&
                noDealsFor(contact.getContactId())) {
            createDeal(contact);
        }
        return true;
    }

    private void createDeal(Contact contact) {
        Deal deal = new Deal();
        deal.setName(createName(contact.getName()));
        deal.setContactId(contact.getId());
        deal.setOwnerId(contact.getOwnerId());
        client.deals().create(deal);
    }

    private boolean isCreated(Meta meta) {
        return meta.getSync().getEventType().equals("created");
    }

    private boolean isOwnedBySalesRep(Long userId) {
        return salesRepPerson.getId().equals(userId);
    }

    private boolean isCompany(Contact contact) {
        return Boolean.TRUE.equals(contact.getIsOrganization());
    }

    private String createName(String contactName) {
        return contactName + " " + LocalDateTime.now().format(DateTimeFormatter.ISO_TIME);
    }

    private boolean noDealsFor(Long contactId) {
        return client.deals().list(new DealsService.SearchCriteria().contactId(contactId)).isEmpty();
    }

}