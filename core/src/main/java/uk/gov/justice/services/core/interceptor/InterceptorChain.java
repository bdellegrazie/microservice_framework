package uk.gov.justice.services.core.interceptor;

import java.util.Deque;
import java.util.LinkedList;
import java.util.stream.Stream;

public class InterceptorChain {

    private Deque<Interceptor> interceptors;
    private Target target;

    public InterceptorChain(final Deque<Interceptor> interceptors, final Target target) {
        this.interceptors = interceptors;
        this.target = target;
    }

    /**
     * Process the next {@link Interceptor} on the queue.
     *
     * @param interceptorContext the {@link InterceptorContext} to pass onto the next interceptor in
     *                           the chain
     * @return the interceptor context returned from the interceptor chain
     */
    public InterceptorContext processNext(final InterceptorContext interceptorContext) {
        if (interceptors.isEmpty()) {
            return target.process(interceptorContext);
        }

        return interceptors.poll().process(interceptorContext, copyOfInterceptorChain());
    }

    /**
     * Process each {@link InterceptorContext} from a stream with the all the remaining {@link
     * Interceptor} on the queue.
     *
     * @param interceptorContexts the stream of interceptor contexts to process
     * @return a stream of processed interceptor contexts
     */
    public Stream<InterceptorContext> processNext(final Stream<InterceptorContext> interceptorContexts) {
        return interceptorContexts.map(interceptorContext ->
                copyOfInterceptorChain().processNext(interceptorContext)
        );
    }

    @SuppressWarnings("unchecked")
    private InterceptorChain copyOfInterceptorChain() {
        return new InterceptorChain(new LinkedList<>(interceptors), target);
    }
}
