package uk.gov.justice.services.example.cakeshop.event.listener;

import org.apache.deltaspike.data.api.EntityManagerDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;

import uk.gov.justice.services.example.cakeshop.domain.event.RecipeAdded;
import uk.gov.justice.services.example.cakeshop.event.listener.converter.RecipeAddedToIngredientsConverter;
import uk.gov.justice.services.example.cakeshop.event.listener.converter.RecipeAddedToRecipeConverter;
import uk.gov.justice.services.example.cakeshop.persistence.IngredientRepository;
import uk.gov.justice.services.example.cakeshop.persistence.RecipeRepository;
import uk.gov.justice.services.example.cakeshop.persistence.entity.Ingredient;
import uk.gov.justice.services.example.cakeshop.persistence.entity.Recipe;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import java.util.UUID;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class RecipeRemovedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeRemovedEventListener.class);
    private static final String FIELD_RECIPE_ID = "recipeId";

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    RecipeAddedToRecipeConverter recipeAddedToRecipeConverter;

    @Inject
    RecipeRepository recipeRepository;

    @Handles("cakeshop.recipe-removed")
    public void recipeRemoved(final JsonEnvelope event) {
        final String recipeId = event.payloadAsJsonObject().getString(FIELD_RECIPE_ID);
        LOGGER.info("=============> Inside remove-recipe Event Listener about to find recipeId: " + recipeId);
        Recipe recipe1 =recipeRepository.findBy(UUID.fromString(recipeId));
        LOGGER.info("=============> Found remove-recipe Event Listener. RecipeId: " + recipe1);
        recipeRepository.remove(recipe1);
        Recipe recipe2 =recipeRepository.findBy(UUID.fromString(recipeId));
        LOGGER.info("=============> Finishing remove-recipe Event Listener. RecipeId: " +  (recipe2==null));


    }
}
