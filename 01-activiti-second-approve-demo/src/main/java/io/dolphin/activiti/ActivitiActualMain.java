package io.dolphin.activiti;

import com.google.common.collect.Maps;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Description:
 * @Author: Eric Liang
 * @Since: 2021-1-24 15:02
 */
public class ActivitiActualMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiActualMain.class);

    public static void main(String[] args) throws ParseException {
        LOGGER.info("启动程序");
        // 1. 创建流程引擎
        ProcessEngine processEngine = getProcessEngine();

        // 2. 部署流程定义文件
        ProcessDefinition processDefinition = getProcessDefinition(processEngine);

        // 3. 启动运行流程
        ProcessInstance processInstance = getProcessInstance(processEngine, processDefinition);

        // 4. 处理流程任务
        processTask(processEngine, processInstance);

        LOGGER.info("结束程序");
    }

    private static void processTask(ProcessEngine processEngine, ProcessInstance processInstance) throws ParseException {
        Scanner scanner = new Scanner(System.in);
        while (processInstance != null && !processInstance.isEnded()) {
            TaskService taskService = processEngine.getTaskService();
            List<Task> taskList = taskService.createTaskQuery().list();
            LOGGER.info("待处理任务数量:[{}]", taskList.size());
            for (Task task : taskList) {
                LOGGER.info("待处理任务:[{}]", task.getName());
                Map<String, Object> variables = getStringObjectMap(processEngine, scanner, task);

                taskService.complete(task.getId(), variables);
                processInstance = processEngine.getRuntimeService()
                        .createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .singleResult();
            }
        }
        scanner.close();
    }

    private static Map<String, Object> getStringObjectMap(ProcessEngine processEngine, Scanner scanner, Task task) throws ParseException {
        FormService formService = processEngine.getFormService();
        TaskFormData taskFormData = formService.getTaskFormData(task.getId());
        List<FormProperty> formProperties = taskFormData.getFormProperties();
        Map<String, Object> variables = Maps.newHashMap();
        for (FormProperty property : formProperties) {
            String line = null;
            if (StringFormType.class.isInstance(property.getType())) {
                LOGGER.info("请输入:[{}] ?", property.getName());
                line = scanner.nextLine();
                variables.put(property.getId(), line);
            } else if (DateFormType.class.isInstance(property.getType())) {
                LOGGER.info("请输入:[{}] ? 格式 (yyyy-MM-dd)", property.getName());
                line = scanner.nextLine();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = dateFormat.parse(line);
                variables.put(property.getId(), date);
            } else {
                LOGGER.info("类型暂不支持 [{}]", property.getType());
            }
            LOGGER.info("输入的内容:[{}]", line);
        }
        return variables;
    }

    private static ProcessInstance getProcessInstance(ProcessEngine processEngine, ProcessDefinition processDefinition) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        LOGGER.info("启动流程:[{}]", processInstance.getProcessDefinitionKey());
        return processInstance;
    }

    private static ProcessDefinition getProcessDefinition(ProcessEngine processEngine) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("second_approve.bpmn20.xml");
        Deployment deployment = deploymentBuilder.deploy();
        String deploymentId = deployment.getId();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();
        LOGGER.info("流程定义文件:[{}], 流程id:[{}]", processDefinition.getName(), processDefinition.getId());
        return processDefinition;
    }

    private static ProcessEngine getProcessEngine() {
        ProcessEngineConfiguration cfg = new StandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = cfg.buildProcessEngine();
        String name = processEngine.getName();
        String version = ProcessEngine.VERSION;
        LOGGER.info("流程引擎名称：[{}],版本：[{}]", name, version);
        return processEngine;
    }
}
