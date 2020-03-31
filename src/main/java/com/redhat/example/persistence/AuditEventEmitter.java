package com.redhat.example.persistence;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import com.redhat.example.utils.EMFBuilder;

import org.jbpm.persistence.api.integration.EventCollection;
import org.jbpm.persistence.api.integration.EventEmitter;
import org.jbpm.persistence.api.integration.InstanceView;
import org.jbpm.persistence.api.integration.base.BaseEventCollection;
import org.jbpm.persistence.api.integration.model.CaseInstanceView;
import org.jbpm.persistence.api.integration.model.ProcessInstanceView;
import org.jbpm.persistence.api.integration.model.TaskInstanceView;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AuditEventEmitter
 */
public class AuditEventEmitter implements EventEmitter {

    private static final Logger logger = LoggerFactory.getLogger(AuditEventEmitter.class);
    private ExecutorService executor = Executors.newCachedThreadPool();
    private EntityManagerFactory emf;

    public AuditEventEmitter() {
        try {
            InitialContext ctx = new InitialContext();
            TransactionManager txManager = (TransactionManager) ctx.lookup("java:jboss/TransactionManager");

            Transaction suspendedTx = txManager.suspend();

            emf = EMFBuilder.newEntityManagerFactory();
            txManager.resume(suspendedTx);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void apply(Collection<InstanceView<?>> data) {
        if (data.isEmpty() || emf == null) {
            return;
        }

        executor.execute(() -> {
            for (InstanceView<?> view : data) {
                logger.info("view: {}", view);

                if (view instanceof ProcessInstanceView) {
                    processEvent((ProcessInstanceView) view);
                } else if (view instanceof CaseInstanceView) {
                    caseEvent((CaseInstanceView) view);
                } else if (view instanceof TaskInstanceView) {
                    taskEvent((TaskInstanceView) view);
                }
            }
        });
    }

    private void taskEvent(TaskInstanceView view) {
    }

    private void caseEvent(CaseInstanceView view) {
    }

    private void processEvent(ProcessInstanceView processInstanceView) {
        try {
            EntityManager em = emf.createEntityManager();

            EntityTransaction tx = em.getTransaction();
            tx.begin();
            Query query = em.createQuery(
                    "select pil from ProcessInstanceLog pil where pil.processInstanceId = :processInstanceId",
                    ProcessInstanceLog.class);
            query.setParameter("processInstanceId", processInstanceView.getId());
            Optional<?> first = query.getResultStream()
                                                      .findFirst();
            ProcessInstanceLog processInstanceLog = null;

            if (first.isPresent()) {
                processInstanceLog = (ProcessInstanceLog) first.get();

                Date start = processInstanceLog.getStart();
                Date end = new Date();

                long duration = end.getTime() - start.getTime();

                processInstanceLog.setDuration(duration);
                processInstanceLog.setEnd(end);
                                
                if (processInstanceView.getSource() instanceof RuleFlowProcessInstance) {
                    RuleFlowProcessInstance processInstance = (RuleFlowProcessInstance) processInstanceView.getSource();
                    processInstanceLog.setOutcome(processInstance.getOutcome());
                }
            } else {
                processInstanceLog = new ProcessInstanceLog(processInstanceView.getId(),
                                                            processInstanceView.getProcessId());

                processInstanceLog.setCorrelationKey(processInstanceView.getCorrelationKey());
                processInstanceLog.setParentProcessInstanceId(processInstanceView.getParentId());
                processInstanceLog.setProcessInstanceDescription(processInstanceView.getProcessInstanceDescription());
                processInstanceLog.setProcessName(processInstanceView.getProcessName());
                processInstanceLog.setProcessVersion(processInstanceView.getProcessVersion());
                processInstanceLog.setStatus(processInstanceView.getState());
                processInstanceLog.setExternalId(processInstanceView.getContainerId());
                processInstanceLog.setIdentity(processInstanceView.getInitiator());

                if (processInstanceView.getSource() instanceof RuleFlowProcessInstance) {
                    RuleFlowProcessInstance processInstance = (RuleFlowProcessInstance) processInstanceView.getSource();
                    processInstanceLog.setSlaDueDate(processInstance.getSlaDueDate());
                    processInstanceLog.setSlaCompliance(processInstance.getSlaCompliance());
                    processInstanceLog.setProcessType(processInstance.getRuleFlowProcess().getProcessType());   
                }
            }

            processInstanceLog.setStatus(processInstanceView.getState());

            em.persist(processInstanceLog);
            tx.commit();
        } catch (Exception e) {
            logger.error("Exception {}", e);
        }
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