package org.openscore.lang.runtime.bindings;

/*******************************************************************************
* (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Apache License v2.0 which accompany this distribution.
*
* The Apache License is available at
* http://www.apache.org/licenses/LICENSE-2.0
*
*******************************************************************************/


import org.openscore.lang.entities.bindings.Input;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InputsBinding {

    @Autowired
    private ScriptEvaluator scriptEvaluator;

    /**
     * Binds the inputs to a new result map
     * @param context : initial context
     * @param inputs : the inputs to bind
     * @return : a new map with all inputs resolved (does not include initial context)
     */
    public Map<String,Serializable> bindInputs(Map<String,Serializable> context,
                                               List<Input> inputs){
        Map<String,Serializable> resultContext = new HashMap<>();
        Map<String,Serializable> srcContext = new HashMap<>(context); //we do not want to change original context map
        for(Input input : inputs){
            bindInput(input,srcContext,resultContext);
        }
        return resultContext;
    }

    private void bindInput(Input input, Map<String,Serializable> context,Map<String,Serializable> targetContext) {
        String inputName = input.getName();
        Validate.notEmpty(inputName);
        Serializable value = resolveValue(inputName, input, context, targetContext);

        if(input.isRequired() && value == null) {
            throw new RuntimeException("Input with name : "+ inputName + " is Required, but value is empty");// todo : add stepName here?
        }

        targetContext.put(inputName,value);
    }

    private Serializable resolveValue(String inputName, Input input, Map<String, Serializable> context,
                                      Map<String, Serializable> targetContext) {
        Serializable value = null;

        if(context.containsKey(inputName) && !input.isOverride()){
            value = context.get(inputName);
        }

        if(value == null && StringUtils.isNotEmpty(input.getExpression())){
            Map<String,Serializable> scriptContext = new HashMap<>(context); //we do not want to change original context map
            scriptContext.putAll(targetContext);//so you can resolve previous inputs already binded

            String expr = input.getExpression();
            value = scriptEvaluator.evalExpr(expr, scriptContext);
        }

        return value;
    }


}
