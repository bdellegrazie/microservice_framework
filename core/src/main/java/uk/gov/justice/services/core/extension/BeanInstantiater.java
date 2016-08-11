package uk.gov.justice.services.core.extension;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

public class BeanInstantiater {

    @Inject
    BeanManager beanManager;

    /**
     * Instantiates the bean using CDI BeanManager auto-wiring all the dependencies.
     *
     * @param bean the bean from which to get the instance
     * @return an instance of the bean
     */
    public <T> T instantiate(final Bean<T> bean) {
        return beanManager.getContext(bean.getScope())
                .get(bean, beanManager.createCreationalContext(bean));
    }
}