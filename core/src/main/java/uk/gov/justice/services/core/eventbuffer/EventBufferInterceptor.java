package uk.gov.justice.services.core.eventbuffer;

import static uk.gov.justice.services.core.interceptor.InterceptorContext.copyWithInput;

import uk.gov.justice.services.core.interceptor.Interceptor;
import uk.gov.justice.services.core.interceptor.InterceptorChain;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.event.buffer.api.EventBufferService;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

public class EventBufferInterceptor implements Interceptor {

    private static final int FIRST_CONTEXT = 0;

    @Inject
    EventBufferService eventBufferService;

    @Override
    public InterceptorContext process(final InterceptorContext interceptorContext, final InterceptorChain interceptorChain) {
        //TODO: Simple approach, can do checks on interceptor contexts for failures in dispatch
        final List<InterceptorContext> resultContexts = interceptorChain.processNext(streamFromEventBufferFor(interceptorContext));

        if (resultContexts.isEmpty()) {
            return interceptorContext;
        }

        return resultContexts.get(FIRST_CONTEXT);
    }

    private Stream<InterceptorContext> streamFromEventBufferFor(final InterceptorContext interceptorContext) {
        return eventBufferService.currentOrderedEventsWith(interceptorContext.inputEnvelope())
                .map(jsonEnvelope -> copyWithInput(interceptorContext, jsonEnvelope));
    }

    @Override
    public int priority() {
        return EVENT_BUFFER;
    }
}
