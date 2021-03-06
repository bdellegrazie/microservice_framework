package uk.gov.justice.services.example.cakeshop.it;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonassert.JsonAssert.emptyCollection;
import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.Arrays.asList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.client.Entity.entity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.fail;

import uk.gov.justice.services.eventsourcing.repository.jdbc.eventlog.EventLog;
import uk.gov.justice.services.example.cakeshop.it.util.ApiResponse;
import uk.gov.justice.services.example.cakeshop.it.util.StandaloneEventLogJdbcRepository;
import uk.gov.justice.services.example.cakeshop.it.util.TestProperties;
import uk.gov.justice.services.test.utils.core.http.HttpResponsePoller;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.json.JsonObjectBuilder;
import javax.sql.DataSource;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CakeShopIT {

    private static final int OK = 200;
    private static final int ACCEPTED = 202;
    private static final int NOT_FOUND = 404;
    private static final int BAD_REQUEST = 400;
    private static final String H2_DRIVER = "org.h2.Driver";
    private static final String RECIPES_RESOURCE_URI = "http://localhost:8080/example-command-api/command/api/rest/cakeshop/recipes/";
    private static final String CAKES_RESOURCE_URI = "http://localhost:8080/example-command-api/command/api/rest/cakeshop/cakes/";
    private static final String ORDERS_RESOURCE_URI = "http://localhost:8080/example-command-api/command/api/rest/cakeshop/orders/";
    private static final String RECIPES_RESOURCE_QUERY_URI = "http://localhost:8080/example-query-api/query/api/rest/cakeshop/recipes/";
    private static final String ORDERS_RESOURCE_QUERY_URI = "http://localhost:8080/example-query-api/query/api/rest/cakeshop/orders/";
    private static final String ADD_RECIPE_MEDIA_TYPE = "application/vnd.cakeshop.add-recipe+json";
    private static final String REMOVE_RECIPE_MEDIA_TYPE = "application/vnd.cakeshop.remove-recipe+json";
    private static final String MAKE_CAKE_MEDIA_TYPE = "application/vnd.cakeshop.make-cake+json";
    private static final String ORDER_CAKE_MEDIA_TYPE = "application/vnd.cakeshop.order-cake+json";
    private static final String QUERY_RECIPE_MEDIA_TYPE = "application/vnd.cakeshop.recipe+json";
    private static final String QUERY_RECIPES_MEDIA_TYPE = "application/vnd.cakeshop.recipes+json";
    private static final String QUERY_ORDER_MEDIA_TYPE = "application/vnd.cakeshop.order+json";

    public final static String JMS_USERNAME = "jmsuser";

    public final static String JMS_PASSWORD = "jms@user123";
    private static final String JMS_BROKER_URL = "tcp://localhost:61616";

    private static StandaloneEventLogJdbcRepository EVENT_LOG_REPOSITORY;
    private static ActiveMQConnectionFactory JMS_CONNECTION_FACTORY;
    private static DataSource CAKE_SHOP_DS;

    private Client client;

    private HttpResponsePoller httpResponsePoller;

    @BeforeClass
    public static void beforeClass() throws Exception {
        DataSource eventStoreDataSource = initEventStoreDb();
        EVENT_LOG_REPOSITORY = new StandaloneEventLogJdbcRepository(eventStoreDataSource);
        JMS_CONNECTION_FACTORY = new ActiveMQConnectionFactory(JMS_BROKER_URL);
        initCakeShopDb();
    }

    @Test
    public void shouldReturn202ResponseWhenAddingRecipe() throws Exception {
        String recipeId = "163af847-effb-46a9-96bc-32a0f7526f88";
        Response response = sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(entity(addRecipeCommand(), ADD_RECIPE_MEDIA_TYPE));
        assertThat(response.getStatus(), is(ACCEPTED));
    }

    @Test
    public void shouldReturn202ResponseWhenRemovingRecipe() throws Exception {
        String recipeId = "163af847-effb-46a9-96bc-32a0f7526f25";
        Response response = sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(entity(addRecipeCommand(), ADD_RECIPE_MEDIA_TYPE));
        assertThat(response.getStatus(), is(ACCEPTED));
        response = sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(entity(addRecipeCommand(), REMOVE_RECIPE_MEDIA_TYPE));
        assertThat(response.getStatus(), is(ACCEPTED));
    }

    @Test
    public void shouldReturn400ResponseWhenJsonNotAdheringToSchemaIsSent() throws Exception {
        String cakeId = "163af847-effb-46a9-96bc-32a0f7526f77";
        Response response = sendTo(CAKES_RESOURCE_URI + cakeId).request()
                .post(entity("{}", MAKE_CAKE_MEDIA_TYPE));
        assertThat(response.getStatus(), is(BAD_REQUEST));
    }

    @Test
    public void shouldRegisterRecipeAddedEvent() {
        String recipeId = "163af847-effb-46a9-96bc-32a0f7526f99";
        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(entity(
                        jsonObject()
                                .add("name", "Vanilla cake")
                                .add("glutenFree", false)
                                .add("ingredients", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("name", "vanilla")
                                                .add("quantity", 2)
                                        ).build()
                                ).build().toString(),
                        ADD_RECIPE_MEDIA_TYPE));

        await().until(() -> eventsWithPayloadContaining(recipeId).count() == 1);

        EventLog event = eventsWithPayloadContaining(recipeId).findFirst().get();
        assertThat(event.getName(), is("cakeshop.recipe-added"));
        with(event.getMetadata())
                .assertEquals("stream.id", recipeId)
                .assertEquals("stream.version", 1);
        String eventPayload = event.getPayload();
        with(eventPayload)
                .assertThat("$.recipeId", equalTo(recipeId))
                .assertThat("$.name", equalTo("Vanilla cake"))
                .assertThat("$.glutenFree", equalTo(false))
                .assertThat("$.ingredients[0].name", equalTo("vanilla"))
                .assertThat("$.ingredients[0].quantity", equalTo(2));
    }

    @Test
    public final void singleAddAndRemoveRecipe() throws InterruptedException {
        shouldRegisterRecipeRemovedEvent();
    }


    @Test
    public final void sequentialAddAndRemoveRecipe() throws InterruptedException {
        shouldRegisterRecipeRemovedEvent();
        shouldRegisterRecipeRemovedEvent();
        shouldRegisterRecipeRemovedEvent();
        shouldRegisterRecipeRemovedEvent();
        shouldRegisterRecipeRemovedEvent();
    }

    @Test
    public final void concurrentAddAndRemoveRecipe() throws InterruptedException {
        ExecutorService exec = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 10; i++) {
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    shouldRegisterRecipeRemovedEvent();
                }
            });
        }
        exec.shutdown();
        exec.awaitTermination(180, TimeUnit.SECONDS);
    }


    public void shouldRegisterRecipeRemovedEvent() {
        final String recipeId = UUID.randomUUID().toString();

        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(entity(
                        jsonObject()
                                .add("name", "Vanilla cake")
                                .add("glutenFree", false)
                                .add("ingredients", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("name", "vanilla")
                                                .add("quantity", 2)
                                        ).build()
                                ).build().toString(),
                        ADD_RECIPE_MEDIA_TYPE));

        await().until(() -> eventsWithPayloadContaining(recipeId).count() == 1);

        EventLog event = eventsWithPayloadContaining(recipeId).findFirst().get();
        assertThat(event.getName(), is("cakeshop.recipe-added"));
        with(event.getMetadata())
                .assertEquals("stream.id", recipeId)
                .assertEquals("stream.version", 1);
        String eventPayload = event.getPayload();
        with(eventPayload)
                .assertThat("$.recipeId", equalTo(recipeId))
                .assertThat("$.name", equalTo("Vanilla cake"))
                .assertThat("$.glutenFree", equalTo(false))
                .assertThat("$.ingredients[0].name", equalTo("vanilla"))
                .assertThat("$.ingredients[0].quantity", equalTo(2));

        ApiResponse response = queryForRecipe(recipeId);
        assertThat(response.httpCode(), is(OK));

        sendTo(RECIPES_RESOURCE_URI + recipeId).request().post(entity(jsonObject()
                .add("recipeId", recipeId).build().toString(), REMOVE_RECIPE_MEDIA_TYPE));

        await().until(() -> eventsWithPayloadContaining(recipeId).count() == 2);

        final String notFoundResponse = httpResponsePoller.pollUntilNotFound(RECIPES_RESOURCE_QUERY_URI + recipeId, QUERY_RECIPE_MEDIA_TYPE);
        assertThat(notFoundResponse, notNullValue());
    }

    @Test
    public void shouldReturn200WhenQueryingForRecipes() throws Exception {
        Response response = sendTo(RECIPES_RESOURCE_QUERY_URI + "?pagesize=10").request().accept(QUERY_RECIPES_MEDIA_TYPE).get();
        assertThat(response.getStatus(), is(OK));
    }

    @Test
    public void shouldReturn400WhenMandatoryQueryParamNotProvided() throws Exception {
        Response response = sendTo(RECIPES_RESOURCE_QUERY_URI).request().accept(QUERY_RECIPES_MEDIA_TYPE).get();
        assertThat(response.getStatus(), is(BAD_REQUEST));
    }

    @Test
    public void shouldReturn400WhenIncorrectMediaTypeInAccept() throws Exception {
        Response response = sendTo(RECIPES_RESOURCE_QUERY_URI).request().accept("*/*").get();
        assertThat(response.getStatus(), is(BAD_REQUEST));
    }

    @Test
    public void shouldReturn404IfRecipeDoesNotExist() {
        ApiResponse response = queryForRecipe("163af847-effb-46a9-96bc-32a0f7526f00");
        assertThat(response.httpCode(), is(NOT_FOUND));
    }

    @Test
    public void shouldReturnRecipeOfGivenId() {
        String recipeId = "163af847-effb-46a9-96bc-32a0f7526f22";
        final String recipeName = "Cheesy cheese cake";
        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(recipeEntity(recipeName));


        await().until(() -> queryForRecipe(recipeId).httpCode() == OK);

        ApiResponse response = queryForRecipe(recipeId);

        with(response.body())
                .assertThat("$.id", equalTo(recipeId))
                .assertThat("$.name", equalTo(recipeName));
    }

    @Test
    public void shouldFailTransactionOnDBFailureAndRedirectEventToDLQ() throws Exception {
        Session jmsSession = null;
        try {
            jmsSession = jmsSession();
            final MessageConsumer dlqConsumer = queueConsumerOf(jmsSession, "DLQ");
            clear(dlqConsumer);

            //closing db to cause transaction error
            closeCakeShopDb();

            String recipeId = "363af847-effb-46a9-96bc-32a0f7526f12";
            sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                    .post(recipeEntity("Cheesy cheese cake"));

            final TextMessage messageFromDLQ = (TextMessage) dlqConsumer.receive();

            with(messageFromDLQ.getText())
                    .assertThat("$._metadata.name", equalTo("cakeshop.recipe-added"))
                    .assertThat("$.recipeId", equalTo(recipeId));

            initCakeShopDb();
        } finally {
            jmsSession.close();
        }
    }


    @Test
    public void shouldReturnRecipes() {
        //adding 2 recipes
        String recipeId = "163af847-effb-46a9-96bc-32a0f7526e14";
        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(recipeEntity("Cheesy cheese cake"));

        String recipeId2 = "163af847-effb-46a9-96bc-32a0f7526e15";
        sendTo(RECIPES_RESOURCE_URI + recipeId2).request()
                .post(recipeEntity("Chocolate muffin"));


        await().until(() -> {
            String responseBody = queryForRecipes(asList(new BasicNameValuePair("pagesize", "30"))).body();
            return responseBody.contains(recipeId) && responseBody.contains(recipeId2);
        });

        ApiResponse response = queryForRecipes();
        assertThat(response.httpCode(), is(OK));

        with(response.body())
                .assertThat("$.recipes[?(@.id=='" + recipeId + "')].name", hasItem("Cheesy cheese cake"))
                .assertThat("$.recipes[?(@.id=='" + recipeId2 + "')].name", hasItem("Chocolate muffin"));
    }

    @Test
    public void shouldFilterRecipesUsingPageSize() {
        //adding 2 recipes
        String recipeId = "263af847-effb-46a9-96bc-32a0f7526e44";
        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(recipeEntity("Absolutely cheesy cheese cake"));

        String recipeId2 = "263af847-effb-46a9-96bc-32a0f7526e55";
        sendTo(RECIPES_RESOURCE_URI + recipeId2).request()
                .post(recipeEntity("Chocolate muffin"));


        final int pageSize = 1;
        await().until(() -> queryForRecipes().body().contains(recipeId));

        ApiResponse response = queryForRecipes(asList(new BasicNameValuePair("pagesize", "1")));
        assertThat(response.httpCode(), is(OK));

        with(response.body())
                .assertThat("$.recipes[?(@.id=='" + recipeId2 + "')]", emptyCollection())
                .assertThat("$.recipes[?(@.id=='" + recipeId + "')].name", hasItem("Absolutely cheesy cheese cake"));
    }

    @Test
    public void shouldFilterGlutenFreeRecipes() {
        //adding 2 recipes
        String recipeId = "163af847-effb-46a9-96bc-32a0f7526e66";
        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(recipeEntity("Muffin", false));

        String recipeId2 = "163af847-effb-46a9-96bc-32a0f7526e77";
        sendTo(RECIPES_RESOURCE_URI + recipeId2).request()
                .post(recipeEntity("Oat cake", true));

        await().until(() -> queryForRecipes().body().contains(recipeId2));

        ApiResponse response = queryForRecipes(asList(
                new BasicNameValuePair("pagesize", "30"),
                new BasicNameValuePair("glutenFree", "true")));

        assertThat(response.httpCode(), is(OK));

        with(response.body())
                .assertThat("$.recipes[?(@.id=='" + recipeId + "')]", emptyCollection())
                .assertThat("$.recipes[?(@.id=='" + recipeId2 + "')].name", hasItem("Oat cake"));
    }

    @Test
    public void shouldReturn400WhenInvalidNumericParamPassed() {
        ApiResponse response = queryForRecipes(asList(
                new BasicNameValuePair("pagesize", "invalid")));

        assertThat(response.httpCode(), is(BAD_REQUEST));
    }


    @Test
    public void shouldReturn400WhenInvalidBooleanParamPassed() {
        ApiResponse response = queryForRecipes(asList(
                new BasicNameValuePair("pagesize", "30"),
                new BasicNameValuePair("glutenFree", "invalid")));

        assertThat(response.httpCode(), is(BAD_REQUEST));
    }

    @Test
    public void shouldReturn202ResponseWhenMakingCake() throws Exception {
        String cakeId = "163af847-effb-46a9-96bc-32a0f7526f11";
        Response response = sendTo(CAKES_RESOURCE_URI + cakeId).request()
                .post(entity(makeCakeCommand(), MAKE_CAKE_MEDIA_TYPE));
        assertThat(response.getStatus(), is(ACCEPTED));
    }

    @Test
    public void shouldNotPersistRecipeWhenIngredientPersistenceFailsDueToSharedTransaction() throws InterruptedException {
        final String recipeId = UUID.randomUUID().toString();

        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(entity(
                        jsonObject()
                                .add("name", "Transaction Failure Recipe Rollback Cake")
                                .add("ingredients", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("name", "ingredient-with-long-name-to-exceed-database-column-length")
                                                .add("quantity", 1)
                                        ).build()
                                ).build().toString(),
                        ADD_RECIPE_MEDIA_TYPE));

        Thread.sleep(500);

        assertThat(queryForRecipe(recipeId).httpCode(), is(NOT_FOUND));
    }

    @Test
    public void shouldPublishEventToPublicTopic() throws Exception {
        Session jmsSession = jmsSession();
        final MessageConsumer publicTopicConsumer = topicConsumerOf(jmsSession, "public.event");

        final String recipeId = "163af847-effb-46a9-96bc-32a0f7526e13";
        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(recipeEntity("Apple pie", false));

        final TextMessage message = (TextMessage) publicTopicConsumer.receive();
        with(message.getText())
                .assertThat("$._metadata.name", equalTo("cakeshop.recipe-added"))
                .assertThat("$.recipeId", equalTo(recipeId))
                .assertThat("$.name", equalTo("Apple pie"));

        jmsSession.close();
    }

    private MessageConsumer topicConsumerOf(final Session session, final String topicName) throws JMSException {
        final Topic topic = session.createTopic(topicName);
        return session.createConsumer(topic);
    }

    private MessageConsumer queueConsumerOf(final Session session, final String queueName) throws JMSException {
        final Queue queue = session.createQueue(queueName);
        return session.createConsumer(queue);
    }

    private Session jmsSession() throws JMSException {
        final javax.jms.Connection connection = JMS_CONNECTION_FACTORY.createConnection(JMS_USERNAME, JMS_PASSWORD);
        connection.start();
        return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }


    @Test
    public void shouldReturnOrderWithUTCOrderDate() {
        final String orderId = "263af847-effb-46a9-96bc-32a0f7526e12";

        final Response commandResponse =
                sendTo(ORDERS_RESOURCE_URI + orderId).request()
                        .post(entity(
                                jsonObject()
                                        .add("recipeId", "163af847-effb-46a9-96bc-32a0f7526f11")
                                        .add("deliveryDate", "2016-07-25T23:09:01.0+05:00")
                                        .build().toString(),
                                ORDER_CAKE_MEDIA_TYPE));

        assertThat(commandResponse.getStatus(), is(ACCEPTED));

        await().until(() -> queryForOrder(orderId).httpCode() == OK);

        final ApiResponse queryResponse = queryForOrder(orderId);

        with(queryResponse.body())
                .assertThat("$.orderId", equalTo(orderId))
                .assertThat("$.deliveryDate", equalTo("2016-07-25T18:09:01Z"));
    }

    private static void initCakeShopDb() throws Exception {
        CAKE_SHOP_DS = initDatabase("db.cakeshop.url", "db.cakeshop.userName", "db.cakeshop.password",
                "liquibase/view-store-db-changelog.xml", "liquibase/event-buffer-changelog.xml");
    }


    private static DataSource initEventStoreDb() throws Exception {
        return initDatabase("db.eventstore.url", "db.eventstore.userName",
                "db.eventstore.password", "liquibase/event-store-db-changelog.xml");
    }

    private void closeCakeShopDb() throws SQLException {
        final Connection connection = CAKE_SHOP_DS.getConnection();
        final Statement statement = connection.createStatement();
        statement.execute("SHUTDOWN");
        connection.close();
    }

    private ApiResponse queryForRecipe(final String recipeId) {
        final Response jaxrsResponse = sendTo(RECIPES_RESOURCE_QUERY_URI + recipeId).request().accept(QUERY_RECIPE_MEDIA_TYPE).get();
        return ApiResponse.from(jaxrsResponse);
    }

    private ApiResponse queryForOrder(final String orderId) {
        final Response jaxrsResponse = sendTo(ORDERS_RESOURCE_QUERY_URI + orderId).request().accept(QUERY_ORDER_MEDIA_TYPE).get();
        return ApiResponse.from(jaxrsResponse);
    }

    private ApiResponse queryForRecipes() {
        return queryForRecipes(asList(new BasicNameValuePair("pagesize", "50")));
    }

    private ApiResponse queryForRecipes(final List<NameValuePair> queryParams) {
        try {
            URIBuilder uri = new URIBuilder(RECIPES_RESOURCE_QUERY_URI);
            uri.addParameters(queryParams);
            Response jaxrRsResponse = sendTo(uri.toString()).request().accept(QUERY_RECIPES_MEDIA_TYPE).get();
            return ApiResponse.from(jaxrRsResponse);
        } catch (URISyntaxException e) {
            fail(e.getMessage());
            return null;
        }
    }

    private String addRecipeCommand() {
        return jsonObject()
                .add("name", "Chocolate muffin in six easy steps")
                .add("glutenFree", false)
                .add("ingredients", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("name", "chocolate")
                                .add("quantity", 1)
                        ).build())
                .build().toString();
    }

    private static DataSource initDatabase(final String dbUrlPropertyName,
                                           final String dbUserNamePropertyName,
                                           final String dbPasswordPropertyName,
                                           final String... liquibaseChangeLogXmls) throws Exception {
        final BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(H2_DRIVER);
        final TestProperties properties = TestProperties.getInstance();
        dataSource.setUrl(properties.value(dbUrlPropertyName));
        dataSource.setUsername(properties.value(dbUserNamePropertyName));
        dataSource.setPassword(properties.value(dbPasswordPropertyName));
        boolean dropped = false;
        final JdbcConnection jdbcConnection = new JdbcConnection(dataSource.getConnection());
        for (String liquibaseChangeLogXml : liquibaseChangeLogXmls) {
            Liquibase liquibase = new Liquibase(liquibaseChangeLogXml,
                    new ClassLoaderResourceAccessor(), jdbcConnection);
            if (!dropped) {
                liquibase.dropAll();
                dropped = true;
            }
            liquibase.update("");
        }
        return dataSource;
    }

    private String makeCakeCommand() {
        return jsonObject()
                .add("recipeId", "163af847-effb-46a9-96bc-32a0f7526f99")
                .build().toString();
    }

    private JsonObjectBuilder jsonObject() {
        return createObjectBuilder();
    }

    private Stream<EventLog> eventsWithPayloadContaining(final String string) {
        return EVENT_LOG_REPOSITORY.findAll().filter(e -> e.getPayload().contains(string));
    }

    private WebTarget sendTo(String url) {
        return client.target(url);
    }

    private Entity<String> recipeEntity(final String recipeName) {
        return recipeEntity(recipeName, false);
    }

    private Entity<String> recipeEntity(String recipeName, boolean glutenFree) {
        return entity(
                jsonObject()
                        .add("name", recipeName)
                        .add("glutenFree", glutenFree)
                        .add("ingredients", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("name", "someIngredient")
                                        .add("quantity", 1)
                                ).build()
                        ).build().toString(),
                ADD_RECIPE_MEDIA_TYPE);
    }

    private void clear(MessageConsumer msgConsumer) throws JMSException {
        while (msgConsumer.receiveNoWait() != null) {
        }
    }

    @Before
    public void before() throws Exception {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();
        cm.setMaxTotal(200); // Increase max total connection to 200
        cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20
        ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
        client = new ResteasyClientBuilder().httpEngine(engine).build();
        httpResponsePoller = new HttpResponsePoller();
    }

    @After
    public void cleanup() throws Exception {
        client.close();

    }

}
