/* 
 * If not stated otherwise in this file or this component's Licenses.txt file the 
 * following copyright and licenses apply:
 *
 * Copyright 2018 RDK Management
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
 *
 * Author: Stanislav Menshykov
 * Created: 2/9/16  11:25 AM
 */
package com.comcast.xconf;

import com.comcast.apps.hesperius.ruleengine.domain.standard.StandardOperation;
import com.comcast.apps.hesperius.ruleengine.main.api.FixedArg;
import com.comcast.apps.hesperius.ruleengine.main.impl.Condition;
import com.comcast.apps.hesperius.ruleengine.main.impl.Rule;
import com.comcast.xconf.estbfirmware.factory.RuleFactory;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;


public class RuleHelperTest {

    @Test
    public void toConditionsNotCompoundRule() throws Exception {
        Condition condition = createCondition("fixedArg");
        Rule rule = createRule(condition);

        List<Condition> actualResult = Lists.newArrayList(RuleHelper.toConditions(rule));

        assertTrue(actualResult.size() == 1);
        assertEquals(Collections.singletonList(condition), actualResult);
    }

    @Test
    public void toConditionsCheckNPE() throws Exception {
        List<Condition> conditionList = Lists.newArrayList(RuleHelper.toConditions(null));

        assertTrue(conditionList.size() == 0);
    }

    @Test
    public void toConditionsCompoundRule() throws Exception {
        Condition condition1 = createCondition("fixedArg1");
        Condition condition2 = null;
        Condition condition3 = createCondition("fixedArg2");
        Rule rule = new Rule();
        rule.setCompoundParts(Lists.newArrayList(createRule(condition1), createRule(condition2), createRule(condition3)));

        List<Condition> actualResult = Lists.newArrayList(RuleHelper.toConditions(rule));

        List<Condition> expectedResult = Lists.newArrayList(condition1, condition3);
        assertTrue(actualResult.size() == 2);
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void toConditionsComplexRule() throws Exception {
        Condition condition1 = createCondition("fixedArg1");
        Condition condition2 = createCondition("fixedArg2");
        Rule rule = createRule(condition1);
        rule.setCompoundParts(Collections.singletonList(createRule(condition2)));

        List<Condition> actualResult = Lists.newArrayList(RuleHelper.toConditions(rule));

        List<Condition> expectedResult = Lists.newArrayList(condition1, condition2);
        assertTrue(actualResult.size() == 2);
        assertEquals(expectedResult, actualResult);
    }

    private Rule createRule(Condition condition) {
        Rule result = new Rule();
        result.setCondition(condition);

        return result;
    }

    private Condition createCondition(Object fixedArgValue) {
        return new Condition(RuleFactory.MODEL, StandardOperation.LIKE, FixedArg.from(fixedArgValue));
    }
}
