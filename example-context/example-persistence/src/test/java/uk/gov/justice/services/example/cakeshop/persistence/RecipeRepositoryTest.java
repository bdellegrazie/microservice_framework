package uk.gov.justice.services.example.cakeshop.persistence;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItems;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.deltaspike.jpa.api.config.base.JpaBaseConfig;
import org.junit.Ignore;
import uk.gov.justice.services.example.cakeshop.persistence.entity.Recipe;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import javax.transaction.*;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

//@RunWith(CdiTestRunner.class)
public class RecipeRepositoryTest {

    private static final UUID RECIPE_ID_A = UUID.randomUUID();
    private static final String RECIPE_NAME_A = "Chocolate Cake";
    private static final UUID RECIPE_ID_B = UUID.randomUUID();
    private static final String RECIPE_NAME_B = "Sponge Cake";
    private static final UUID RECIPE_ID_C = UUID.randomUUID();
    private static final String RECIPE_NAME_C = "Muffin";
    private static final boolean RECIPE_GLUTEN_FREE_A = true;


    @Inject
    private RecipeRepository recipeRepository;

    private Recipe recipeA;
    private Recipe recipeB;
    private Recipe recipeC;
  //  private static DataSource CAKE_SHOP_DS;

/*    private  void initCakeShopDb() throws Exception {
        CAKE_SHOP_DS = initDatabase("db.cakeshop.url", "db.cakeshop.userName", "db.cakeshop.password",
                "liquibase/view-store-db-changelog.xml");
    }*/

    @Before
    public void setup() throws Exception {
       // initCakeShopDb();
        recipeC = createRecipe(RECIPE_ID_C, RECIPE_NAME_C, true);
        recipeRepository.save(recipeC);
        recipeA = createRecipe(RECIPE_ID_A, RECIPE_NAME_A, RECIPE_GLUTEN_FREE_A);
        recipeRepository.save(recipeA);
        recipeB = createRecipe(RECIPE_ID_B, RECIPE_NAME_B, false);
        recipeRepository.save(recipeB);

    }
    private static final String H2_DRIVER = "org.h2.Driver";
    private static DataSource initDatabase(final String dbUrlPropertyName,
                                           final String dbUserNamePropertyName,
                                           final String dbPasswordPropertyName,
                                           final String liquibaseEventStoreDbChangelogXml) throws Exception {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(H2_DRIVER);
       /* TestProperties properties = TestProperties.getInstance();
        dataSource.setUrl(properties.value(dbUrlPropertyName));
        dataSource.setUsername(properties.value(dbUserNamePropertyName));
        dataSource.setPassword(properties.value(dbPasswordPropertyName));
*/
       /* Liquibase liquibase = new Liquibase(liquibaseEventStoreDbChangelogXml,
                new ClassLoaderResourceAccessor(), new JdbcConnection(dataSource.getConnection()));
        liquibase.dropAll();
        liquibase.update("");
     */   return dataSource;
    }

    //@Ignore


    public void shouldFindRecipeById() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
      //  userTxn.begin();

        Recipe recipe = recipeRepository.findBy(RECIPE_ID_A);

        assertThat(recipe, is(notNullValue()));
        assertThat(recipe.getId(), equalTo(RECIPE_ID_A));
        assertThat(recipe.getName(), equalTo(RECIPE_NAME_A));
        assertThat(recipe.isGlutenFree(), is(RECIPE_GLUTEN_FREE_A));
      //  userTxn.commit();

    }

    @Ignore
    public void shouldReturnPage() throws Exception {
        final int pageSize = 2;
        List<Recipe> recipeList = recipeRepository.findBy(pageSize, Optional.empty(), Optional.empty());

        assertThat(recipeList, hasSize(2));
        assertThat(recipeList, hasItems(recipeA, recipeC));

    }

    @Ignore
    public void shouldReturnNullIfRecipeNotFound() {
        Recipe recipe = recipeRepository.findBy(UUID.randomUUID());

        assertThat(recipe, is(nullValue()));
    }


    @Ignore
    public void shouldReturnListOfRecipesMatchingName() {
        List<Recipe> recipeList = recipeRepository.findBy(10, Optional.of("Cake"), Optional.empty());

        assertThat(recipeList, hasSize(2));
        assertThat(recipeList, hasItems(recipeA, recipeB));

    }

    @Ignore
    public void shouldReturnListOfGlutenFreeOfRecipes() {
        List<Recipe> recipeList = recipeRepository.findBy(10, Optional.empty(), Optional.of(true));

        assertThat(recipeList, hasSize(2));
        assertThat(recipeList, hasItems(recipeA, recipeC));
    }


    @Ignore
    public void shouldReturnEmptyListOfRecipesIfSearchDoesNotMatch() {
        List<Recipe> recipeList = recipeRepository.findBy(10, Optional.of("InvalidName"), Optional.empty());

        assertThat(recipeList, hasSize(0));
    }

    //@Test
    public void shouldReturnAllRecipes() {
        List<Recipe> recipeList = recipeRepository.findAll();

        assertThat(recipeList, hasSize(3));
        assertThat(recipeList, hasItems(recipeA));
        assertThat(recipeList, hasItems(recipeB));
        assertThat(recipeList, hasItems(recipeC));
    }

    private Recipe createRecipe(final UUID id, final String name, final boolean glutenFree) {
        return new Recipe(id, name, glutenFree);
    }
}