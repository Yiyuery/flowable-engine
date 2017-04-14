/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.impl.cmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.engine.common.impl.util.io.BytesStreamSource;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricTaskInstance;
import org.flowable.engine.impl.HistoricActivityInstanceQueryImpl;
import org.flowable.engine.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.engine.impl.bpmn.deployer.BpmnDeployer;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.DeploymentEntityManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricTaskInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntityManager;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.runtime.ProcessInstance;

public abstract class AbstractDynamicInjectionCmd {

    protected void createDerivedProcessDefinitionForTask(CommandContext commandContext, String taskId) {
        TaskEntity taskEntity = commandContext.getTaskEntityManager().findById(taskId);
        ProcessInstance processInstance = commandContext.getExecutionEntityManager().findById(taskEntity.getProcessInstanceId());
        createDerivedProcessDefinition(commandContext, processInstance);
    }

    protected void createDerivedProcessDefinitionForProcessInstance(CommandContext commandContext, String processInstanceId) {
        ProcessInstance processInstance = commandContext.getExecutionEntityManager().findById(processInstanceId);
        createDerivedProcessDefinition(commandContext, processInstance);
    }

    protected void createDerivedProcessDefinition(CommandContext commandContext, ProcessInstance processInstance) {
        ProcessDefinitionEntity originalProcessDefinitionEntity = commandContext.getProcessDefinitionEntityManager().findById(processInstance.getProcessDefinitionId());
        DeploymentEntity deploymentEntity = createDerivedDeployment(commandContext, originalProcessDefinitionEntity);
        BpmnModel bpmnModel = createBpmnModel(commandContext, originalProcessDefinitionEntity, deploymentEntity);
        storeBpmnModelAsByteArray(commandContext, bpmnModel, deploymentEntity, originalProcessDefinitionEntity.getResourceName());
        ProcessDefinitionEntity derivedProcessDefinitionEntity = deployDerivedDeploymentEntity(commandContext, deploymentEntity, originalProcessDefinitionEntity);
        updateExecutions(commandContext, derivedProcessDefinitionEntity, (ExecutionEntity) processInstance);
    }

    protected DeploymentEntity createDerivedDeployment(CommandContext commandContext, ProcessDefinitionEntity processDefinitionEntitty) {
        DeploymentEntityManager deploymentEntityManager = commandContext.getDeploymentEntityManager();
        DeploymentEntity deploymentEntity = deploymentEntityManager.findById(processDefinitionEntitty.getDeploymentId());

        DeploymentEntity newDeploymentEntity = deploymentEntityManager.create();
        newDeploymentEntity.setName(deploymentEntity.getName());
        newDeploymentEntity.setCategory(deploymentEntity.getCategory());
        newDeploymentEntity.setKey(deploymentEntity.getKey());
        newDeploymentEntity.setTenantId(deploymentEntity.getTenantId());

        newDeploymentEntity.setDerivedFrom(deploymentEntity.getId());
        if (deploymentEntity.getDerivedFromRoot() != null) {
            newDeploymentEntity.setDerivedFromRoot(deploymentEntity.getDerivedFromRoot());
        } else {
            newDeploymentEntity.setDerivedFromRoot(deploymentEntity.getId());
        }

        deploymentEntityManager.insert(newDeploymentEntity);
        return newDeploymentEntity;
    }

    protected ResourceEntity storeBpmnModelAsByteArray(CommandContext commandContext, BpmnModel bpmnModel, DeploymentEntity deploymentEntity, String resourceName) {
        BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
        byte[] bytes = bpmnXMLConverter.convertToXML(bpmnModel);
        return addResource(commandContext, deploymentEntity, resourceName, bytes);
    }

    protected ResourceEntity addResource(CommandContext commandContext, DeploymentEntity deploymentEntity, String resourceName, byte[] bytes) {
        ResourceEntityManager resourceEntityManager = commandContext.getResourceEntityManager();
        ResourceEntity resourceEntity = resourceEntityManager.create();
        resourceEntity.setDeploymentId(deploymentEntity.getId());
        resourceEntity.setName(resourceName);
        resourceEntity.setBytes(bytes);
        resourceEntityManager.insert(resourceEntity);
        deploymentEntity.addResource(resourceEntity);
        return resourceEntity;
    }

    protected ProcessDefinitionEntity deployDerivedDeploymentEntity(CommandContext commandContext, 
            DeploymentEntity deploymentEntity, ProcessDefinitionEntity originalProcessDefinitionEntity) {

        Map<String, Object> deploymentSettings = new HashMap<String, Object>();
        deploymentSettings.put(DeploymentSettings.IS_DERIVED_DEPLOYMENT, true);
        deploymentSettings.put(DeploymentSettings.DERIVED_PROCESS_DEFINITION_ID, originalProcessDefinitionEntity.getId());
        if (originalProcessDefinitionEntity.getDerivedFromRoot() != null) {
            deploymentSettings.put(DeploymentSettings.DERIVED_PROCESS_DEFINITION_ROOT_ID, originalProcessDefinitionEntity.getDerivedFromRoot());
        } else {
            deploymentSettings.put(DeploymentSettings.DERIVED_PROCESS_DEFINITION_ROOT_ID, originalProcessDefinitionEntity.getId());
        }

        BpmnDeployer bpmnDeployer = commandContext.getProcessEngineConfiguration().getBpmnDeployer();
        deploymentEntity.setNew(true);
        bpmnDeployer.deploy(deploymentEntity, deploymentSettings);

        return deploymentEntity.getDeployedArtifacts(ProcessDefinitionEntity.class).get(0);
    }

    protected BpmnModel createBpmnModel(CommandContext commandContext, ProcessDefinitionEntity originalProcessDefinitionEntity, DeploymentEntity newDeploymentEntity) {
        ResourceEntity originalBpmnResource = commandContext.getResourceEntityManager()
                .findResourceByDeploymentIdAndResourceName(originalProcessDefinitionEntity.getDeploymentId(), originalProcessDefinitionEntity.getResourceName());
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(new BytesStreamSource(originalBpmnResource.getBytes()), false, false);

        org.flowable.bpmn.model.Process process = bpmnModel.getProcessById(originalProcessDefinitionEntity.getKey());
        updateBpmnProcess(commandContext, process, bpmnModel, originalProcessDefinitionEntity, newDeploymentEntity);
        return bpmnModel;
    }

    protected abstract void updateBpmnProcess(CommandContext commandContext, org.flowable.bpmn.model.Process process, 
            BpmnModel bpmnModel, ProcessDefinitionEntity originalProcessDefinitionEntity, DeploymentEntity newDeploymentEntity);

    protected void updateExecutions(CommandContext commandContext, ProcessDefinitionEntity processDefinitionEntity, ExecutionEntity processInstance) {
        processInstance.setProcessDefinitionId(processDefinitionEntity.getId());
        processInstance.setProcessDefinitionVersion(processDefinitionEntity.getVersion());
        
        HistoricProcessInstanceEntityManager historicProcessInstanceEntityManager = commandContext.getHistoricProcessInstanceEntityManager();
        HistoricProcessInstanceEntity historicProcessInstance = (HistoricProcessInstanceEntity) historicProcessInstanceEntityManager.findById(processInstance.getId());
        historicProcessInstance.setProcessDefinitionId(processDefinitionEntity.getId());
        historicProcessInstance.setProcessDefinitionVersion(processDefinitionEntity.getVersion());
        historicProcessInstanceEntityManager.update(historicProcessInstance);

        HistoricTaskInstanceEntityManager historicTaskInstanceEntityManager = commandContext.getHistoricTaskInstanceEntityManager();
        HistoricTaskInstanceQueryImpl taskQuery = new HistoricTaskInstanceQueryImpl();
        taskQuery.processInstanceId(processInstance.getId());
        List<HistoricTaskInstance> historicTasks = historicTaskInstanceEntityManager.findHistoricTaskInstancesByQueryCriteria(taskQuery);
        if (historicTasks != null) {
            for (HistoricTaskInstance historicTaskInstance : historicTasks) {
                HistoricTaskInstanceEntity taskEntity = (HistoricTaskInstanceEntity) historicTaskInstance;
                taskEntity.setProcessDefinitionId(processDefinitionEntity.getId());
                historicTaskInstanceEntityManager.update(taskEntity);
            }
        }
        
        HistoricActivityInstanceEntityManager historicActivityInstanceEntityManager = commandContext.getHistoricActivityInstanceEntityManager();
        HistoricActivityInstanceQueryImpl activityQuery = new HistoricActivityInstanceQueryImpl();
        activityQuery.processInstanceId(processInstance.getId());
        List<HistoricActivityInstance> historicActivities = historicActivityInstanceEntityManager.findHistoricActivityInstancesByQueryCriteria(activityQuery, null);
        if (historicActivities != null) {
            for (HistoricActivityInstance historicActivityInstance : historicActivities) {
                HistoricActivityInstanceEntity activityEntity = (HistoricActivityInstanceEntity) historicActivityInstance;
                activityEntity.setProcessDefinitionId(processDefinitionEntity.getId());
                historicActivityInstanceEntityManager.update(activityEntity);
            }
        }
        
        List<ExecutionEntity> childExecutions = commandContext.getExecutionEntityManager().findChildExecutionsByProcessInstanceId(processInstance.getId());
        for (ExecutionEntity childExecution : childExecutions) {
            childExecution.setProcessDefinitionId(processDefinitionEntity.getId());
        }

        updateExecutions(commandContext, processDefinitionEntity, processInstance, childExecutions);
    }

    protected abstract void updateExecutions(CommandContext commandContext, 
            ProcessDefinitionEntity processDefinitionEntity, ExecutionEntity processInstance, List<ExecutionEntity> childExecutions);
    
    protected List<GraphicInfo> createWayPoints(double x1, double y1, double x2, double y2) {
        List<GraphicInfo> wayPoints = new ArrayList<>();
        wayPoints.add(new GraphicInfo(x1, y1));
        wayPoints.add(new GraphicInfo(x2, y2));
        
        return wayPoints;
    }
    
    protected List<GraphicInfo> createWayPoints(double x1, double y1, double x2, double y2, double x3, double y3) {
        List<GraphicInfo> wayPoints = createWayPoints(x1, y1, x2, y2);
        wayPoints.add(new GraphicInfo(x3, y3));
        
        return wayPoints;
    } 

}