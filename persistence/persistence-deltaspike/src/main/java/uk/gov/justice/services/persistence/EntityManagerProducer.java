package uk.gov.justice.services.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.*;
import javax.transaction.TransactionScoped;
import javax.transaction.Transactional;

/**
 * Producer of {:link EntityManager} for use with JPA (Delta-spike).
 */
public class EntityManagerProducer {
    private static final String UTC = "UTC";

    @PersistenceUnit
    EntityManagerFactory entityManagerFactory;// = Persistence.createEntityManagerFactory("Cakeshop",setProperties());



    @Produces
    @RequestScoped
    public EntityManager create() {
        TimeZone.setDefault(TimeZone.getTimeZone(UTC));
        return entityManagerFactory.createEntityManager();
    }

    public void close(@Disposes final EntityManager em) {
        if (em.isOpen()) {
            em.close();
        }
    }
  /*  DS.cakeshop = new://Resource?type=DataSource
    DS.cakeshop.JdbcDriver = org.h2.Driver
    DS.cakeshop.JdbcUrl = jdbc:h2:mem:test
    DS.cakeshop.JtaManaged = true
    DS.cakeshop.UserName = sa
    DS.cakeshop.Password =
    Cakeshop.hibernate.dialect = org.hibernate.dialect.HSQLDialect
    Cakeshop.hibernate.hbm2ddl.auto = create-drop
  */  /*protected Properties setProperties() {
       Properties properties = new Properties();
        //properties.setProperty("hibernate.dialect", "org.hibernate.dialect.Oracle10gDialect");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        properties.setProperty("hibernate.enable_lazy_load_no_trans", "true");
        properties.setProperty("hibernate.jdbc.batch_size", "20");
        properties.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        properties.setProperty("hibernate.connection.url", "jdbc:h2:mem:test");
        properties.setProperty("javax.persistence.jdbc.user", "sa");
        properties.setProperty("javax.persistence.jdbc.password", "");
        properties.setProperty("org.hibernate.flushMode", "ALWAYS");
        return properties;
    }*/
}