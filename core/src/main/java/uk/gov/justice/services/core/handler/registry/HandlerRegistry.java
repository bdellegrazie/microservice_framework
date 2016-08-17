package uk.gov.justice.services.core.handler.registry;

import static java.lang.String.format;
import static uk.gov.justice.services.core.handler.Handlers.handlerMethodsFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.handler.HandlerMethod;
import uk.gov.justice.services.core.handler.exception.MissingHandlerException;
import uk.gov.justice.services.core.handler.registry.exception.DuplicateHandlerException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for storing a map of which command handlers handle which commands.
 */

public class HandlerRegistry {

    private final Map<String, HandlerMethod> handlerMethods = new HashMap<>();

    public HandlerMethod get(final String name) {
        final HandlerMethod handlerMethod = handlerMethods.getOrDefault(name, handlerMethods.get("*"));

        if (handlerMethod != null) {
            return handlerMethod;
        } else {
            throw new MissingHandlerException(
                    format("No handler registered to handle action %s", name));
        }
    }

    /**
     * Registers a handler instance.
     *
     * @param handlerInstance handler instance to be registered.
     */
    public void register(final Object handlerInstance) {
        handlerMethodsFrom(handlerInstance).forEach(method -> register(handlerInstance, method));
    }

    /**
     * Registers handler instance and method.
     *
     * @param handler handler instance to register.
     * @param method  handler method to register.
     */
    private void register(final Object handler, final Method method) {

        final HandlerMethod handlerMethod = new HandlerMethod(handler, method, method.getReturnType());
        final String name = method.getAnnotation(Handles.class).value();
        if (handlerMethods.containsKey(name)) {
            throw new DuplicateHandlerException(
                    format("Can't register %s because a command handler method %s has " +
                            "already been registered for %s ", handlerMethod, handlerMethods.get(name), name));
        }

        handlerMethods.put(name, handlerMethod);
    }
}
