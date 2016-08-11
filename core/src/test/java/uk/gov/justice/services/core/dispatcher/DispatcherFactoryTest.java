package uk.gov.justice.services.core.dispatcher;

import static org.apache.commons.lang3.reflect.FieldUtils.readField;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.core.handler.registry.HandlerRegistry;

import org.junit.Test;

public class DispatcherFactoryTest {

    private static final String HANDLER_REGISTRY_FIELD_NAME = "handlerRegistry";

    @Test
    public void shouldCreateANewHandlerRegistryForEachDispatcherInstance() throws Exception {

        final DispatcherFactory dispatcherFactory = new DispatcherFactory();

        final Dispatcher dispatcher1 = dispatcherFactory.createNew();
        final Dispatcher dispatcher2 = dispatcherFactory.createNew();

        final HandlerRegistry handlerRegistry1 = getHandlerRegistryFrom(dispatcher1);
        final HandlerRegistry handlerRegistry2 = getHandlerRegistryFrom(dispatcher2);

        assertThat(handlerRegistry1, is(not(sameInstance(handlerRegistry2))));
    }

    private HandlerRegistry getHandlerRegistryFrom(final Dispatcher dispatcher_1) throws IllegalAccessException {
        return (HandlerRegistry) readField(dispatcher_1, HANDLER_REGISTRY_FIELD_NAME, true);
    }
}
