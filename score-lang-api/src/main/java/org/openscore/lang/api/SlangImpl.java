package org.openscore.lang.api;
/*******************************************************************************
* (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Apache License v2.0 which accompany this distribution.
*
* The Apache License is available at
* http://www.apache.org/licenses/LICENSE-2.0
*
*******************************************************************************/

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.openscore.api.Score;
import org.openscore.api.TriggeringProperties;
import org.openscore.events.EventBus;
import org.openscore.events.EventConstants;
import org.openscore.events.ScoreEventListener;
import org.openscore.lang.compiler.SlangCompiler;
import org.openscore.lang.compiler.SlangSource;
import org.openscore.lang.entities.CompilationArtifact;
import org.openscore.lang.entities.ScoreLangConstants;
import org.openscore.lang.runtime.env.RunEnvironment;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static ch.lambdaj.Lambda.filter;
import static org.hamcrest.Matchers.notNullValue;
import static org.openscore.lang.entities.ScoreLangConstants.EVENT_ACTION_END;
import static org.openscore.lang.entities.ScoreLangConstants.EVENT_ACTION_ERROR;
import static org.openscore.lang.entities.ScoreLangConstants.EVENT_ACTION_START;
import static org.openscore.lang.entities.ScoreLangConstants.EVENT_EXECUTION_FINISHED;
import static org.openscore.lang.entities.ScoreLangConstants.EVENT_INPUT_END;
import static org.openscore.lang.entities.ScoreLangConstants.EVENT_INPUT_START;
import static org.openscore.lang.entities.ScoreLangConstants.EVENT_OUTPUT_END;
import static org.openscore.lang.entities.ScoreLangConstants.EVENT_OUTPUT_START;
import static org.openscore.lang.entities.ScoreLangConstants.SLANG_EXECUTION_EXCEPTION;

/**
 * User: stoneo
 * Date: 03/12/2014
 * Time: 15:20
 */
public class SlangImpl implements Slang {

    @Autowired
    private SlangCompiler compiler;

    @Autowired
    private Score score;

    @Autowired
    private EventBus eventBus;

    private final static Logger logger = Logger.getLogger(SlangImpl.class);

    @Override
    public CompilationArtifact compileOperation(SlangSource source, String operationName, Set<SlangSource> dependencies) {

        Validate.notNull(source, "Source can not be null");
        Set<SlangSource> dependencySources = new HashSet<>(filter(notNullValue(), dependencies));

        try {
            return compiler.compile(source, operationName, dependencySources);
        } catch (Exception e) {
            logger.error("Failed compilation for source : " + source.getName() + " ,Exception is : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompilationArtifact compile(SlangSource source, Set<SlangSource> dependencies) {
        return compileOperation(source, null, dependencies);
    }

    @Override
    public Long run(CompilationArtifact compilationArtifact, Map<String, Serializable> runInputs) {
        Validate.notNull(compilationArtifact, "Compilation artifact can not be null");
        if(runInputs == null){
            runInputs = new HashMap<>();
        }
        Map<String, Serializable> executionContext = new HashMap<>();
        executionContext.put(ScoreLangConstants.RUN_ENV, new RunEnvironment());
        executionContext.put(ScoreLangConstants.USER_INPUTS_KEY, (Serializable) runInputs);

        TriggeringProperties triggeringProperties = TriggeringProperties
                .create(compilationArtifact.getExecutionPlan())
                .setDependencies(compilationArtifact.getDependencies())
                .setContext(executionContext);

        return score.trigger(triggeringProperties);
    }

    @Override
    public Long compileAndRunOperation(SlangSource source, String operationName, Set<SlangSource> dependencies, Map<String, Serializable> runInputs) {
        CompilationArtifact compilationArtifact = compileOperation(source, operationName, dependencies);
        return run(compilationArtifact, runInputs);
    }

    @Override
    public Long compileAndRun(SlangSource source, Set<SlangSource> dependencies, Map<String, Serializable> runInputs) {
        return compileAndRunOperation(source, null, dependencies, runInputs);
    }

    @Override
    public void subscribeOnEvents(ScoreEventListener eventListener, Set<String> eventTypes) {
        eventBus.subscribe(eventListener, eventTypes);
    }

    @Override
    public void unSubscribeOnEvents(ScoreEventListener eventListener) {
        eventBus.unsubscribe(eventListener);
    }

    @Override
    public void subscribeOnAllEvents(ScoreEventListener eventListener) {
        subscribeOnEvents(eventListener, getAllEventTypes());
    }

    private Set<String> getAllEventTypes() {
        Set<String> eventTypes = new HashSet<>();
        eventTypes.add(EventConstants.SCORE_FINISHED_EVENT);
        eventTypes.add(EventConstants.SCORE_BRANCH_FAILURE_EVENT);
        eventTypes.add(EventConstants.SCORE_FINISHED_BRANCH_EVENT);
        eventTypes.add(EventConstants.SCORE_NO_WORKER_FAILURE_EVENT);
        eventTypes.add(EventConstants.SCORE_PAUSED_EVENT);
        eventTypes.add(EventConstants.SCORE_ERROR_EVENT);
        eventTypes.add(EventConstants.SCORE_FAILURE_EVENT);
        eventTypes.add(SLANG_EXECUTION_EXCEPTION);
        eventTypes.add(EVENT_ACTION_START);
        eventTypes.add(EVENT_ACTION_END);
        eventTypes.add(EVENT_ACTION_ERROR);
        eventTypes.add(EVENT_INPUT_START);
        eventTypes.add(EVENT_INPUT_END);
        eventTypes.add(EVENT_OUTPUT_START);
        eventTypes.add(EVENT_OUTPUT_END);
        eventTypes.add(EVENT_EXECUTION_FINISHED);
        return eventTypes;
    }
}
