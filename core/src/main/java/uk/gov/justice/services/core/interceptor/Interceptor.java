package uk.gov.justice.services.core.interceptor;

/**
 * Interface that all interceptors must implement.  Provides default priority values and process
 * signature.
 */
public interface Interceptor {

    int EVENT_BUFFER = 1000;
    int AUDIT_BEFORE_ACCESS_CONTROL = 2000;
    int ACCESS_CONTROL = 3000;
    int AUDIT_AFTER_ACCESS_CONTROL = 4000;
    int USER = 100000;

    /**
     * Process an interception with the given {@link InterceptorContext} and {@link
     * InterceptorChain}.
     *
     * @param interceptorContext the interceptor context
     * @param interceptorChain   the interceptor chain, call this with processNext after completion
     *                           of task
     * @return an interceptor context
     */
    InterceptorContext process(final InterceptorContext interceptorContext, final InterceptorChain interceptorChain);

    /**
     * Provides the priority level of an interceptor, lower is higher priority.  This is used to
     * priorities the calling chain of interceptors.
     *
     * @return the priority level of the interceptor
     */
    int priority();
}
