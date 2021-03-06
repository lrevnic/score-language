/*******************************************************************************
* (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Apache License v2.0 which accompany this distribution.
*
* The Apache License is available at
* http://www.apache.org/licenses/LICENSE-2.0
*
*******************************************************************************/

package org.openscore.lang.compiler.utils;

import ch.lambdaj.Lambda;
import org.openscore.lang.compiler.model.Flow;
import org.openscore.lang.compiler.model.Operation;
import org.openscore.lang.compiler.model.Task;
import org.openscore.lang.entities.ResultNavigation;
import org.openscore.lang.entities.bindings.Result;
import org.apache.commons.collections4.CollectionUtils;
import org.openscore.api.ExecutionPlan;
import org.openscore.api.ExecutionStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.Matchers.equalTo;

/*
 * Created by orius123 on 11/11/14.
 */
@Component
public class ExecutionPlanBuilder {

    @Autowired
    private ExecutionStepFactory stepFactory;

    private static final String SLANG_NAME = "slang";
    private static final int NUMBER_OF_TASK_EXECUTION_STEPS = 2;
    private static final long FLOW_END_STEP_ID = 0L;
    private static final long FLOW_START_STEP_ID = 1L;

    public ExecutionPlan createOperationExecutionPlan(Operation compiledOp) {
        ExecutionPlan executionPlan = new ExecutionPlan();
        executionPlan.setName(compiledOp.getName());
        executionPlan.setLanguage(SLANG_NAME);
        executionPlan.setFlowUuid(compiledOp.getId());

        executionPlan.setBeginStep(1L);

        executionPlan.addStep(stepFactory.createStartStep(1L, compiledOp.getPreExecActionData(), compiledOp.getInputs(),
                compiledOp.getName()));
        executionPlan.addStep(stepFactory.createActionStep(2L, compiledOp.getAction().getActionData()));
        executionPlan.addStep(stepFactory.createEndStep(3L, compiledOp.getPostExecActionData(), compiledOp.getOutputs(),
                compiledOp.getResults(), compiledOp.getName()));
        return executionPlan;
    }

    public ExecutionPlan createFlowExecutionPlan(Flow compiledFlow) {
        ExecutionPlan executionPlan = new ExecutionPlan();
        executionPlan.setName(compiledFlow.getName());
        executionPlan.setLanguage(SLANG_NAME);
        executionPlan.setFlowUuid(compiledFlow.getId());

        executionPlan.setBeginStep(FLOW_START_STEP_ID);
        //flow start step
        executionPlan.addStep(stepFactory.createStartStep(FLOW_START_STEP_ID, compiledFlow.getPreExecActionData(),
                compiledFlow.getInputs(), compiledFlow.getName()));
        //flow end step
        executionPlan.addStep(stepFactory.createEndStep(FLOW_END_STEP_ID, compiledFlow.getPostExecActionData(),
                compiledFlow.getOutputs(), compiledFlow.getResults(), compiledFlow.getName()));

        Map<String, Long> taskReferences = new HashMap<>();
        for (Result result : compiledFlow.getResults()) {
            taskReferences.put(result.getName(), FLOW_END_STEP_ID);
        }

        Deque<Task> tasks = compiledFlow.getWorkflow().getTasks();

        if (CollectionUtils.isEmpty(tasks)) {
            throw new RuntimeException("Flow: " + compiledFlow.getName() + " has no tasks");
        }

        List<ExecutionStep> taskExecutionSteps = buildTaskExecutionSteps(tasks.getFirst(), taskReferences, tasks);
        executionPlan.addSteps(taskExecutionSteps);

        return executionPlan;
    }

    private List<ExecutionStep> buildTaskExecutionSteps(Task task,
                                                        Map<String, Long> taskReferences, Deque<Task> tasks) {

        List<ExecutionStep> taskExecutionSteps = new ArrayList<>();

        String taskName = task.getName();
        Long currentId = getCurrentId(taskReferences);

        //Begin Task
        taskReferences.put(taskName, currentId);
        taskExecutionSteps.add(stepFactory.createBeginTaskStep(currentId++, task.getPreTaskActionData(),
                task.getRefId(), taskName));

        //End Task
        Map<String, ResultNavigation> navigationValues = new HashMap<>();
        for (Map.Entry<String, String> entry : task.getNavigationStrings().entrySet()) {
            String nextStepName = entry.getValue();
            if (taskReferences.get(nextStepName) == null) {
                Task nextTaskToCompile = Lambda.selectFirst(tasks, having(on(Task.class).getName(), equalTo(nextStepName)));
                taskExecutionSteps.addAll(buildTaskExecutionSteps(nextTaskToCompile, taskReferences, tasks));
            }
			long nextStepId = taskReferences.get(nextStepName);
			String presetResult = (FLOW_END_STEP_ID == nextStepId) ? nextStepName : null;
			navigationValues.put(entry.getKey(), new ResultNavigation(nextStepId, presetResult));
        }
        taskExecutionSteps.add(stepFactory.createFinishTaskStep(currentId, task.getPostTaskActionData(),
                navigationValues, taskName));
        return taskExecutionSteps;
    }

    private Long getCurrentId(Map<String, Long> taskReferences) {
        Long max = Lambda.max(taskReferences);
        return max + NUMBER_OF_TASK_EXECUTION_STEPS;
    }

}
