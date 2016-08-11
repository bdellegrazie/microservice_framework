package uk.gov.justice.services.core.interceptor;

import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWith;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.dispatcher.DispatcherCache;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

@ApplicationScoped
public class InterceptorChainProcessorProducer {

    @Inject
    DispatcherCache dispatcherCache;

    @Inject
    InterceptorCache interceptorCache;

    /**
     * Produces the correct implementation of an synchronous chain process depending on the {@link
     * Adapter} annotation at the injection point.
     *
     * @param injectionPoint class where the {@link InterceptorChainProcessor} is being injected
     * @return the chain process
     */
    @Produces
    public InterceptorChainProcessor produceProcessor(final InjectionPoint injectionPoint) {
        //TODO: As part of integration into the framework the dispatcher needs to be simplified so access is through a single method
        final Function<JsonEnvelope, JsonEnvelope> synchronousDispatch = dispatcherCache.dispatcherFor(injectionPoint)::synchronousDispatch;
        return createProcessor(synchronousDispatch, injectionPoint);
    }

    /**
     * Constructs the {@link InterceptorChainProcessor} for the given dispatcher interceptor target
     * and injection point.
     *
     * @param dispatcher     the dispatcher target method
     * @param injectionPoint the injection point of the {@link InterceptorChainProcessor}
     * @return the chain process function
     */
    private InterceptorChainProcessor createProcessor(final Function<JsonEnvelope, JsonEnvelope> dispatcher, final InjectionPoint injectionPoint) {

        return jsonEnvelope -> {
            final InterceptorChain interceptorChain = new InterceptorChain(interceptorCache.getInterceptors(), new DispatcherTarget(dispatcher));

            return interceptorChain.processNext(interceptorContextWith(jsonEnvelope, injectionPoint))
                    .outputEnvelope()
                    .orElse(null);
        };
    }
}
