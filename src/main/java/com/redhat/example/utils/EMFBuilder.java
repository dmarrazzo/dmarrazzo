package com.redhat.example.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.hibernate.jpa.HibernatePersistenceProvider;

public class EMFBuilder {

    public static EntityManagerFactory newEntityManagerFactory() {
        EntityManagerFactory emf = null;
        try {
            Map<String, Object> configuration = new HashMap<>();
            emf = new HibernatePersistenceProvider().createContainerEntityManagerFactory(getPersistenceUnitInfo(),
                    configuration);
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return emf;
    }

    private static PersistenceUnitInfo getPersistenceUnitInfo() throws NamingException {
        InitialContext ctx = new InitialContext();

        DataSource datasource = (DataSource) ctx.lookup("java:jboss/datasources/AuditDS");
        
        return PersistenceUnitInfoImpl.newPersistenceUnitInfo("org.jbpm.logging.local", entityClassNames(),
                properties(), datasource);

    }

    private static Properties properties() {
        Properties properties = new Properties();

        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        // properties.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        // properties.setProperty("hibernate.connection.url", "jdbc:h2:mem:audit;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        // properties.setProperty("hibernate.connection.username", "sa");
        // properties.setProperty("hibernate.connection.password", "sa");

        properties.setProperty("hibernate.max_fetch_depth", "3");
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.id.new_generator_mappings", "false");
        properties.setProperty("hibernate.transaction.jta.platform",
                "org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform");
 
        return properties;
    }

    private static List<String> entityClassNames() {
        String[] classes = { "org.jbpm.process.audit.ProcessInstanceLog", "org.jbpm.process.audit.NodeInstanceLog",
                "org.jbpm.process.audit.VariableInstanceLog" };

        return Arrays.asList(classes);
    }
}