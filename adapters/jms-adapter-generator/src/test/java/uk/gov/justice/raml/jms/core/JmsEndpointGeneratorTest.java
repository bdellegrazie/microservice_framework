package uk.gov.justice.raml.jms.core;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.raml.model.ActionType;
import uk.gov.justice.raml.core.Generator;
import uk.gov.justice.raml.core.GeneratorConfig;
import uk.gov.justice.services.adapters.test.utils.compiler.JavaCompilerUtil;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.dispatcher.Dispatcher;
import uk.gov.justice.services.core.jms.AbstractJMSListener;
import uk.gov.justice.services.messaging.Envelope;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.raml.model.ActionType.DELETE;
import static org.raml.model.ActionType.GET;
import static org.raml.model.ActionType.HEAD;
import static org.raml.model.ActionType.OPTIONS;
import static org.raml.model.ActionType.PATCH;
import static org.raml.model.ActionType.POST;
import static org.raml.model.ActionType.TRACE;
import static uk.gov.justice.services.adapters.test.utils.builder.ActionBuilder.action;
import static uk.gov.justice.services.adapters.test.utils.builder.RamlBuilder.raml;
import static uk.gov.justice.services.adapters.test.utils.builder.ResourceBuilder.resource;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

public class JmsEndpointGeneratorTest {

    private static final String BASE_PACKAGE = "uk.test";
    private static final String BASE_PACKAGE_FOLDER = "/uk/test";

    private Generator generator = new JmsEndpointGenerator();

    @Rule
    public TemporaryFolder outputFolder = new TemporaryFolder();
    private JavaCompilerUtil compiler;

    @Before
    public void setup() throws Exception {
        compiler = new JavaCompilerUtil(outputFolder.getRoot(), outputFolder.getRoot());
    }

    @Test
    public void shouldCreateJmsClass() throws Exception {
        generator.run(
                raml()
                        .with(resource()
                                .withRelativeUri("/structure.controller.commands")
                                .withDefaultAction())
                        .build(),
                configurationWithBasePackage(BASE_PACKAGE));

        File packageDir = new File(outputFolder.getRoot().getAbsolutePath() + BASE_PACKAGE_FOLDER);
        File[] files = packageDir.listFiles();
        assertThat(files.length, is(1));
        assertThat(files[0].getName(), is("StructureControllerCommandsJmsListener.java"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCreateMultipleJmsClasses() throws Exception {
        generator.run(
                raml()
                        .with(resource()
                                .withRelativeUri("/structure.controller.commands")
                                .withDefaultAction())
                        .with(resource()
                                .withRelativeUri("/people.controller.commands")
                                .withDefaultAction())
                        .build(),
                configurationWithBasePackage(BASE_PACKAGE));

        File packageDir = new File(outputFolder.getRoot().getAbsolutePath() + BASE_PACKAGE_FOLDER);
        File[] files = packageDir.listFiles();
        assertThat(files.length, is(2));
        assertThat(files,
                arrayContainingInAnyOrder(hasProperty("name", equalTo("PeopleControllerCommandsJmsListener.java")),
                        hasProperty("name", equalTo("StructureControllerCommandsJmsListener.java"))));

    }

    @Test
    public void shouldOverwriteJmsClass() throws Exception {
        String path = outputFolder.getRoot().getAbsolutePath() + BASE_PACKAGE_FOLDER;
        File packageDir = new File(path);
        packageDir.mkdirs();
        Files.write(Paths.get(path + "/StructureControllerCommandsJmsListener.java"),
                asList("Old file content"));

        generator.run(
                raml()
                        .with(resource()
                                .withRelativeUri("/structure.controller.commands")
                                .withDefaultAction())
                        .build(),
                configurationWithBasePackage(BASE_PACKAGE));

        List<String> lines = Files.readAllLines(Paths.get(path + "/StructureControllerCommandsJmsListener.java"));
        assertThat(lines.get(0), not(containsString("Old file content")));
    }

    @Test
    public void shouldCreateJmsEndpointNamedAfterResourceUri() throws Exception {
        generator.run(
                raml()
                        .with(resource()
                                .withRelativeUri("/structure.controller.commands")
                                .withDefaultAction())
                        .build(),
                configurationWithBasePackage("uk.somepackage"));

        Class<?> compiledClass = compiler.compiledClassOf("uk.somepackage");
        assertThat(compiledClass.getName(), is("uk.somepackage.StructureControllerCommandsJmsListener"));
    }

    @Test
    public void shouldCreateJmsEndpointInADifferentPackage() throws Exception {
        generator.run(
                raml()
                        .with(resource()
                                .withRelativeUri("/structure.controller.commands")
                                .withDefaultAction())
                        .build(),
                configurationWithBasePackage("uk.package2"));

        Class<?> clazz = compiler.compiledClassOf("uk.package2");
        assertThat(clazz.getName(), is("uk.package2.StructureControllerCommandsJmsListener"));
    }

    @Test
    public void shouldCreateJmsEndpointAnnotatedWithCommandHandlerAdapter() throws Exception {
        generator.run(
                raml()
                        .with(resource()
                                .withRelativeUri("/people.handler.commands")
                                .with(action().with(ActionType.POST)))
                        .build(),
                configurationWithBasePackage(BASE_PACKAGE));
        Class<?> clazz = compiler.compiledClassOf(BASE_PACKAGE);
        Adapter adapterAnnotation = clazz.getAnnotation(Adapter.class);
        assertThat(adapterAnnotation, not(nullValue()));
        assertThat(adapterAnnotation.value(), is(COMMAND_HANDLER));

    }

    @Test
    public void shouldCreateJmsEndpointAnnotatedWithCommandControllerAdapter() throws Exception {
        generator.run(
                raml()
                        .with(resource()
                                .withRelativeUri("/people.controller.commands")
                                .with(action().with(ActionType.POST)))
                        .build(),
                configurationWithBasePackage(BASE_PACKAGE));

        Class<?> clazz = compiler.compiledClassOf(BASE_PACKAGE);
        Adapter adapterAnnotation = clazz.getAnnotation(Adapter.class);
        assertThat(adapterAnnotation, not(nullValue()));
        assertThat(adapterAnnotation.value(), is(COMMAND_CONTROLLER));

    }

    @Test
    public void shouldCreateJmsEndpointExtendingAbstractJmsListener() throws Exception {
        generator.run(raml().withDefaults().build(), configurationWithBasePackage(BASE_PACKAGE));

        Class<?> clazz = compiler.compiledClassOf(BASE_PACKAGE);
        assertThat(clazz.getSuperclass(), equalTo(AbstractJMSListener.class));
    }

    @Test
    public void shouldCreateJmsEndpointWithAnnotatedDispatcherProperty() throws Exception {
        generator.run(raml().withDefaults().build(), configurationWithBasePackage(BASE_PACKAGE));

        Class<?> clazz = compiler.compiledClassOf(BASE_PACKAGE);
        Field dispatcherField = clazz.getDeclaredField("dispatcher");
        assertThat(dispatcherField, not(nullValue()));
        assertThat(dispatcherField.getAnnotations(), arrayWithSize(1));
        assertThat(dispatcherField.getAnnotation(Inject.class), not(nullValue()));
    }

    @Test
    public void shouldCreateAnnotatedJmsEndpointWithDestinationLookupProperty() throws Exception {
        generator.run(
                raml()
                        .with(resource()
                                .withRelativeUri("/people.controller.commands")
                                .with(action().with(ActionType.POST)))
                        .build(),
                configurationWithBasePackage(BASE_PACKAGE));

        Class<?> clazz = compiler.compiledClassOf(BASE_PACKAGE);
        assertThat(clazz.getAnnotation(MessageDriven.class), is(notNullValue()));
        assertThat(clazz.getAnnotation(MessageDriven.class).activationConfig(),
                hasItemInArray(allOf(propertyName(equalTo("destinationLookup")),
                        propertyValue(equalTo("people.controller.commands")))));
    }

    @Test
    public void shouldCreateAnnotatedJmsEndpointWithDestinationLookupProperty2() throws Exception {
        generator.run(
                raml()
                        .with(resource()
                                .withRelativeUri("/structure.controller.commands")
                                .with(action().with(ActionType.POST)))
                        .build(),
                configurationWithBasePackage(BASE_PACKAGE));

        Class<?> clazz = compiler.compiledClassOf(BASE_PACKAGE);
        assertThat(clazz.getAnnotation(MessageDriven.class), is(notNullValue()));
        assertThat(clazz.getAnnotation(MessageDriven.class).activationConfig(),
                hasItemInArray(allOf(propertyName(equalTo("destinationLookup")),
                        propertyValue(equalTo("structure.controller.commands")))));
    }

    @Test
    public void shouldCreateAnnotatedJmsEndpointWithDestinationType() throws Exception {
        generator.run(raml().withDefaults().build(), configurationWithBasePackage(BASE_PACKAGE));

        Class<?> clazz = compiler.compiledClassOf(BASE_PACKAGE);
        assertThat(clazz.getAnnotation(MessageDriven.class), is(notNullValue()));
        assertThat(clazz.getAnnotation(MessageDriven.class).activationConfig(),
                hasItemInArray(allOf(propertyName(equalTo("destinationType")),
                        propertyValue(equalTo("javax.jms.Queue")))));
    }

    @Test
    public void shouldCreateAnnotatedJmsEndpointWithMessageSelectorContainingOneCommandWithAPost() throws Exception {
        generator.run(
                raml()
                        .with(resource()
                                .with(action()
                                        .with(ActionType.POST)
                                        .withMediaType("application/vnd.structure.commands.test-cmd+json")))
                        .build(),
                configurationWithBasePackage(BASE_PACKAGE));

        Class<?> clazz = compiler.compiledClassOf(BASE_PACKAGE);
        assertThat(clazz.getAnnotation(MessageDriven.class), is(notNullValue()));
        assertThat(clazz.getAnnotation(MessageDriven.class).activationConfig(),
                hasItemInArray(allOf(propertyName(equalTo("messageSelector")),
                        propertyValue(equalTo("CPPNAME in('structure.commands.test-cmd')")))));
    }

    @Test
    public void shouldOnlyCreateMessageSelectorForPostActionAndIgnoreAllOtherActions() throws Exception {
        generator.run(
                raml()
                        .with(resource()
                                .with(action(POST, "application/vnd.structure.commands.test-cmd1+json"))
                                .with(action(GET, "application/vnd.structure.commands.test-cmd2+json"))
                                .with(action(DELETE, "application/vnd.structure.commands.test-cmd3+json"))
                                .with(action(HEAD, "application/vnd.structure.commands.test-cmd4+json"))
                                .with(action(OPTIONS, "application/vnd.structure.commands.test-cmd5+json"))
                                .with(action(PATCH, "application/vnd.structure.commands.test-cmd6+json"))
                                .with(action(TRACE, "application/vnd.structure.commands.test-cmd7+json")))
                        .build(),
                configurationWithBasePackage(BASE_PACKAGE));

        Class<?> clazz = compiler.compiledClassOf(BASE_PACKAGE);
        assertThat(clazz.getAnnotation(MessageDriven.class), is(notNullValue()));
        assertThat(clazz.getAnnotation(MessageDriven.class).activationConfig(),
                hasItemInArray(allOf(propertyName(equalTo("messageSelector")),
                        propertyValue(equalTo("CPPNAME in('structure.commands.test-cmd1')")))));
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldThrowExceptionIfNoActionsInRaml() throws Exception {

        exception.expect(JmsEndpointGeneratorException.class);
        exception.expectMessage("No actions to process");

        generator.run(
                raml()
                        .with(resource()
                                .withRelativeUri("/structure.controller.commands"))
                        .build(),
                configurationWithBasePackage(BASE_PACKAGE));
    }

    @Test
    public void shouldCreateAnnotatedJmsEndpointWithMessageSelectorContainingTwoCommands() throws Exception {
        generator.run(
                raml()
                        .with(resource()
                                .with(action()
                                        .with(ActionType.POST)
                                        .withMediaType("application/vnd.people.commands.command1+json")
                                        .withMediaType("application/vnd.people.commands.command2+json")))
                        .build(),
                configurationWithBasePackage(BASE_PACKAGE));

        Class<?> clazz = compiler.compiledClassOf(BASE_PACKAGE);
        assertThat(clazz.getAnnotation(MessageDriven.class), is(notNullValue()));
        assertThat(clazz.getAnnotation(MessageDriven.class).activationConfig(),
                hasItemInArray(allOf(propertyName(equalTo("messageSelector")),
                        propertyValue(startsWith("CPPNAME in")),
                        propertyValue(allOf(containsString("'people.commands.command1'"),
                                containsString("'people.commands.command2'"))))));
    }

    @Test
    public void shouldCreateJmsEndpointWithDispatcherGetter() throws Exception {
        generator.run(raml().withDefaults().build(), configurationWithBasePackage(BASE_PACKAGE));

        Class<?> clazz = compiler.compiledClassOf(BASE_PACKAGE);

        Object endpointInstance = clazz.newInstance();
        Dispatcher dispatcher = new DummyDispatcher();
        Field dispatcherField = clazz.getDeclaredField("dispatcher");
        dispatcherField.setAccessible(true);
        dispatcherField.set(endpointInstance, dispatcher);

        Method getDispatcherMethod = clazz.getDeclaredMethod("getDispatcher");
        getDispatcherMethod.setAccessible(true);

        Object getDispatcherResult = getDispatcherMethod.invoke(endpointInstance);
        assertThat(getDispatcherResult, sameInstance(dispatcher));
    }

    private GeneratorConfig configurationWithBasePackage(String basePackageName) {
        Path outputPath = Paths.get(outputFolder.getRoot().getAbsolutePath());
        return new GeneratorConfig(outputPath, outputPath, basePackageName);
    }

    private FeatureMatcher<ActivationConfigProperty, String> propertyName(Matcher<String> matcher) {
        return new FeatureMatcher<ActivationConfigProperty, String>(matcher, "propertyName", "propertyName") {
            @Override
            protected String featureValueOf(ActivationConfigProperty actual) {
                return actual.propertyName();
            }
        };
    }

    private FeatureMatcher<ActivationConfigProperty, String> propertyValue(Matcher<String> matcher) {
        return new FeatureMatcher<ActivationConfigProperty, String>(matcher, "propertyValue", "propertyValue") {
            @Override
            protected String featureValueOf(ActivationConfigProperty actual) {
                return actual.propertyValue();
            }
        };
    }

    public static class DummyDispatcher implements Dispatcher {
        @Override
        public void dispatch(Envelope envelope) {
            // do nothing
        }
    }

}