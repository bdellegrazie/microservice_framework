package uk.gov.justice.services.core.dispatcher;

import uk.gov.justice.services.core.handler.registry.HandlerRegistry;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DispatcherFactory {

    public Dispatcher createNew() {
        return new Dispatcher(new HandlerRegistry());
    }
}
