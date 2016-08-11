package uk.gov.justice.services.persistence;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Scope;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.TimeZone;

/**
 * Producer of {:link EntityManager} for use with JPA (Delta-spike).
 */

public class EntityManagerProducer {
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