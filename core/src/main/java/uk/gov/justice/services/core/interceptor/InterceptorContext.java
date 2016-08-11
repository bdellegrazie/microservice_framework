package uk.gov.justice.services.core.interceptor;

import static uk.gov.justice.services.core.interceptor.ContextPayload.contextPayload;
import static uk.gov.justice.services.core.interceptor.ContextPayload.contextPayloadWith;
import static uk.gov.justice.services.core.interceptor.ContextPayload.copyWithEnvelope;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import javax.enterprise.inject.spi.InjectionPoint;

public class InterceptorContext {

    private final ContextPayload input;
    private final ContextPayload output;
    private final InjectionPoint injectionPoint;

    private InterceptorContext(final ContextPayload input, final ContextPayload output, final InjectionPoint injectionPoint) {
        this.input = input;
        this.output = output;
        this.injectionPoint = injectionPoint;
    }

    private InterceptorContext(final JsonEnvelope input, final InjectionPoint injectionPoint) {
        this.input = contextPayloadWith(input);
        this.output = contextPayload();
        this.injectionPoint = injectionPoint;
    }

    /**
     * Create an interceptor context with the given input and injection point.  Output is set to
     * Optional.empty().
     *
     * @param input          the input JsonEnvelope
     * @param injectionPoint the injection point of the chain process
     * @return the new InterceptorContext
     */
    public static InterceptorContext interceptorContextWith(final JsonEnvelope input, final InjectionPoint injectionPoint) {
        return new InterceptorContext(input, injectionPoint);
    }

    /**
     * Create a copy of the given interceptor context with the given inputEnvelope.
     *
     * @param interceptorContext the interceptor context to copy
     * @param inputEnvelope      the inputEnvelope JsonEnvelope to set as inputEnvelope
     * @return the new InterceptorContext
     */
    public static InterceptorContext copyWithInput(final InterceptorContext interceptorContext, final JsonEnvelope inputEnvelope) {
        return new InterceptorContext(copyWithEnvelope(interceptorContext.inputContext(), inputEnvelope), interceptorContext.outputContext(), interceptorContext.injectionPoint());
    }

    /**
     * Create a copy of the given interceptor context with the given outputEnvelope.
     *
     * @param interceptorContext the interceptor context to copy
     * @param outputEnvelope     the outputEnvelope JsonEnvelope to set as outputEnvelope
     * @return the new InterceptorContext
     */
    public static InterceptorContext copyWithOutput(final InterceptorContext interceptorContext, final JsonEnvelope outputEnvelope) {
        return new InterceptorContext(interceptorContext.inputContext(), copyWithEnvelope(interceptorContext.outputContext(), outputEnvelope), interceptorContext.injectionPoint());
    }

    public JsonEnvelope inputEnvelope() {
        return input.getEnvelope().orElseThrow(() -> new IllegalStateException("No input envelope set."));
    }

    public Optional<JsonEnvelope> outputEnvelope() {
        return output.getEnvelope();
    }

    public Optional<Object> getInputParameter(final String name) {
        return input.getParameter(name);
    }

    public void setInputParameter(final String name, final Object parameter) {
        input.setParameter(name, parameter);
    }

    public Optional<Object> getOutputParameter(final String name) {
        return output.getParameter(name);
    }

    public void setOutputParameter(final String name, final Object parameter) {
        output.setParameter(name, parameter);
    }

    public InjectionPoint injectionPoint() {
        return injectionPoint;
    }

    private ContextPayload inputContext() {
        return input;
    }

    private ContextPayload outputContext() {
        return output;
    }
}
