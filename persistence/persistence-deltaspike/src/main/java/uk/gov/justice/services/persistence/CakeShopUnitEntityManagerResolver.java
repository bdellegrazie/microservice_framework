package uk.gov.justice.services.persistence;

import org.apache.deltaspike.data.api.EntityManagerResolver;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by mfarouk on 08/08/2016.
 */
@ApplicationScoped
public class CakeShopUnitEntityManagerResolver implements EntityManagerResolver {
    @PersistenceContext (name="CakeShop")
    private EntityManager em;

    @Override
    public EntityManager resolveEntityManager() {
        return em;
    }
}