package com.redhat.example.persistence;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jbpm.persistence.api.integration.EventCollection;
import org.jbpm.persistence.api.integration.EventEmitter;
import org.jbpm.persistence.api.integration.InstanceView;
import org.jbpm.persistence.api.integration.base.BaseEventCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AuditEventEmitter
 */
public class AuditEventEmitter implements EventEmitter {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditEventEmitter.class);    
    private ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void apply(Collection<InstanceView<?>> data) {
        if (data.isEmpty()) {
            return;
        }
        
        executor.execute(() -> {                
            for (InstanceView<?> view : data) {
                System.out.println(view);
                logger.info("view: {}", view);
            }
        });
    }

    @Override
    public void close() {
    }

    @Override
    public void deliver(Collection<InstanceView<?>> data) {
    }

    @Override
    public void drop(Collection<InstanceView<?>> data) {
    }

    @Override
    public EventCollection newCollection() {
        return new BaseEventCollection();
    }
}