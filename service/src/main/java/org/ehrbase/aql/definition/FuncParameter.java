/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) Ripple Foundation CIC Ltd, UK, 2017
 * Author: Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.aql.definition;

/**
 * Created by christian on 9/22/2017.
 */
public class FuncParameter {

    private FuncParameterType type;
    private String value;

    public FuncParameter(FuncParameterType type, String value) {
        this.type = type;
        this.value = value;
    }

    public FuncParameterType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public boolean isOperand() {
        return type.equals(FuncParameterType.OPERAND);
    }

    public boolean isIdentifier() {
        return type.equals(FuncParameterType.IDENTIFIER);
    }

    public boolean isVariable() {
        return type.equals(FuncParameterType.VARIABLE);
    }
}
