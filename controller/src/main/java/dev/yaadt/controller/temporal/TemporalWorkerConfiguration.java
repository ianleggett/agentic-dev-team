package dev.yaadt.controller.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import dev.yaadt.controller.executor.AgentRunService;

@Configuration
@EnableConfigurationProperties(TemporalProperties.class)
public class TemporalWorkerConfiguration {
    @Bean(destroyMethod = "shutdown")
    WorkflowServiceStubs workflowServiceStubs(TemporalProperties properties) {
        return WorkflowServiceStubs.newServiceStubs(WorkflowServiceStubsOptions.newBuilder().setTarget(properties.target()).build());
    }
    @Bean(destroyMethod = "")
    WorkflowClient workflowClient(WorkflowServiceStubs service, TemporalProperties properties) {
        return WorkflowClient.newInstance(service, WorkflowClientOptions.newBuilder().setNamespace(properties.namespace()).build());
    }
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "yaadt.temporal", name = "worker-enabled", havingValue = "true")
    WorkerFactory temporalWorkerFactory(WorkflowClient client, TemporalProperties properties, AgentRunService runs,
            dev.yaadt.controller.manager.ProjectManagerPlanningService planning,
            dev.yaadt.controller.manager.ControllerLeaseService leases,
            dev.yaadt.controller.sprint.SprintCommandService commands,
            dev.yaadt.controller.sprint.SprintPlanVersionRepository plans) {
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(properties.taskQueue());
        worker.registerWorkflowImplementationTypes(TemporalWorkflowImplementations.ProjectManager.class, TemporalWorkflowImplementations.Sprint.class);
        worker.registerActivitiesImplementations(new TemporalWorkflowImplementations.Activities(runs, planning, commands, plans, leases));
        return factory;
    }
}
