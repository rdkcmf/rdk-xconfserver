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
 * Author: pbura
 * Created: 17/06/2014  12:15
 */
package com.comcast.xconf;

import com.comcast.apps.hesperius.ruleengine.main.api.*;
import com.comcast.apps.hesperius.ruleengine.main.impl.Condition;
import com.comcast.apps.hesperius.ruleengine.main.impl.Rule;
import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Booleans;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNumeric;

public class RuleHelper {

    private static final Logger LOG = LoggerFactory.getLogger(RuleHelper.class);

    /**
     * provides compareTo implementation compatible with {@code java.util.Comparator<Rule>}
     * that can be used for rule ordering since it takes into account both rule
     * and priority of operations (ascending PERCENT < LIKE < IN < IS)
     *
     * @param r1 first rule to compare
     * @param r2 second rule to compare
     * @return comparison result according to {@link java.util.Comparator#compare(Object, Object)}
     */
    public static int compareRules(final Rule r1, final Rule r2) {


        final int compoundResult = Booleans.compare(r1.isCompound(), r2.isCompound());
        if (compoundResult != 0) return compoundResult;

        final Operation op1 = getFirstChild(r1).getCondition().getOperation();
        final Operation op2 = getFirstChild(r2).getCondition().getOperation();

        if (op1.equals(op2)) {
            return 0;
        } else {
            switch (op1.toString()) {
                case "IS":
                    return 1;
                case "IN_LIST":
                    return op2.toString().equals("IS") ? -1 : 1;
                case "LIKE":
                    return op2.toString().equals("PERCENT") ? 1 : -1;
                case "PERCENT":
                    return -1;
                default:
                    return 0;
            }
        }
    }

    public static Rule getFirstChild(final Rule rule) {
        if (!rule.isCompound()) {
            return rule;
        } else return getFirstChild(rule.getCompoundParts().get(0));
    }

    public static boolean fitsPercent(String accountId, double percent) {
        final double OFFSET = (double)Long.MAX_VALUE + 1;
        final double RANGE = (double)Long.MAX_VALUE * 2 + 1;
        double hashCode = (double)Hashing.sipHash24().hashString(accountId, Charsets.UTF_8).asLong() + OFFSET; // from 0 to (2 * Long.MAX_VALUE + 1)
        double limit = percent / 100 * RANGE;  // from 0 to (2 * Long.MAX_VALUE + 1)
        return (hashCode <= limit); // XAPPS-1978 hashCode is tested for fitness
    }

    public static boolean isOrContains(final Rule rule, final Operation op) {
        return (Iterables.find(toConditions(rule), new Predicate<Condition>() {
            @Override
            public boolean apply(Condition input) {
                return input.getOperation().equals(op);
            }
        }, null) != null);
    }

    public static final Iterable<Condition> toConditions(final Rule rule) {
        final List<Condition> result = new ArrayList<>();
        final List<Rule> tmpRulesQueue = new ArrayList<>();
        tmpRulesQueue.add(rule);
        while (!tmpRulesQueue.isEmpty()) {
            Rule currentRule = tmpRulesQueue.remove(0);
            if (currentRule != null) {
                if (currentRule.getCondition() != null) {
                    result.add(currentRule.getCondition());
                }
                if (CollectionUtils.isNotEmpty(currentRule.getCompoundParts())) {
                    tmpRulesQueue.addAll(currentRule.getCompoundParts());
                }
            }
        }

        return result;
    }
}
