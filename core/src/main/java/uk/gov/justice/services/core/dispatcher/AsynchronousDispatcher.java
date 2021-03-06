package uk.gov.justice.services.core.dispatcher;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.messaging.JsonEnvelope;

/**
 * Dispatches command to the correct asynchronous handler. The framework will inject the correct
 * implementation based on the {@link Adapter} annotation.
 */
@FunctionalInterface
public interface AsynchronousDispatcher {

    /**
     * Dispatches the {@code envelope} to the correct handler.
     *
     * @param envelope The {@link JsonEnvelope} to be dispatched.
     */
    void dispatch(final JsonEnvelope envelope);
}
