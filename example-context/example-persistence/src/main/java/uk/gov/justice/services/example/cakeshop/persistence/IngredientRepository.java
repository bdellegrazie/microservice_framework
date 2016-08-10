package uk.gov.justice.services.example.cakeshop.persistence;

import org.apache.deltaspike.data.api.EntityManagerConfig;
import uk.gov.justice.services.example.cakeshop.persistence.entity.Ingredient;
import uk.gov.justice.services.example.cakeshop.persistence.entity.Recipe;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.justice.services.persistence.CakeShopUnitEntityManagerResolver;

import javax.transaction.Transactional;

//@EntityManagerConfig(entityManagerResolver = CakeShopUnitEntityManagerResolver.class)
@Repository
public interface IngredientRepository extends EntityRepository<Ingredient, UUID> {

    /**
     * Find all {@link Ingredient} by ingedientName (case-insensitive). Accepts '%' wildcard
     * values.
     *
     * @param ingedientName to retrieve the ingredient by, including wildcard characters.
     * @return List of matching ingredients. Never returns null.
     */
    @Query(value = "FROM Ingredient i WHERE LOWER(i.name) LIKE LOWER(?1)")
    public List<Ingredient> findByNameIgnoreCase(final String ingedientName);
}
