package uk.gov.justice.services.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.*;

/**
 * Producer of {:link EntityManager} for use with JPA (Delta-spike).
 */
public class EntityManagerProducer {
    private static final String UTC = "UTC";

    @PersistenceUnit
    EntityManagerFactory entityManagerFactory;

    @PersistenceContext (type = PersistenceContextType.EXTENDED)
    EntityManager entityManager;

    @Produces
    public EntityManager create() {
        TimeZone.setDefault(TimeZone.getTimeZone(UTC));
        if (entityManager == null) {
            entityManager = entityManagerFactory.createEntityManager();
        }
        return entityManager;
//        return entityManagerFactory.createEntityManager();
    }

    public void close(@Disposes final EntityManager em) {
        if (em.isOpen()) {
            em.close();
        }
        if (entityManager.equals(em)) {
            entityManager = null;
        }
    }
}