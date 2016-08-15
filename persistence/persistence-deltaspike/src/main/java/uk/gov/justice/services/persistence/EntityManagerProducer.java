package uk.gov.justice.services.persistence;

import javax.ejb.Stateful;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Scope;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.TransactionScoped;
import java.util.TimeZone;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Producer of {:link EntityManager} for use with JPA (Delta-spike).
 */
@ApplicationScoped
public class EntityManagerProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityManagerProducer.class);

    private static final String UTC = "UTC";

    @PersistenceUnit
    EntityManagerFactory entityManagerFactory;

    @Produces
    @RequestScoped
    public EntityManager create() {
        TimeZone.setDefault(TimeZone.getTimeZone(UTC));
        return entityManagerFactory.createEntityManager();
    }

    public void close(@Disposes final EntityManager em) {
        if (em.isOpen()) {
             em.close();
        }
    }
}