/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.transform.process.function.arithmetic;

import org.apache.inlong.sdk.transform.decode.SourceData;
import org.apache.inlong.sdk.transform.process.Context;
import org.apache.inlong.sdk.transform.process.function.FunctionConstant;
import org.apache.inlong.sdk.transform.process.function.TransformFunction;
import org.apache.inlong.sdk.transform.process.operator.OperatorTools;
import org.apache.inlong.sdk.transform.process.parser.ValueParser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Function;

import java.math.BigDecimal;

/**
 * CosdFunction  ->  cosd(numeric)
 * description:
 * - Return NULL if 'numeric' is NULL;
 * - Return the cosine of 'numeric' in units of degrees.
 */
@Slf4j
@TransformFunction(type = FunctionConstant.ARITHMETIC_TYPE, names = {
        "cosd"}, parameter = "(Numeric numeric)", descriptions = {
                "- Return \"\" if 'numeric' is NULL;",
                "- Return the cosine of 'numeric' in units of degrees."
        }, examples = {"cosd(15) = 0.9659258262890683"})
public class CosdFunction implements ValueParser {

    private ValueParser numberParser;

    public CosdFunction(Function expr) {
        numberParser = OperatorTools.buildParser(expr.getParameters().getExpressions().get(0));
    }

    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        Object numberObj = numberParser.parse(sourceData, rowIndex, context);
        if (numberObj == null) {
            return null;
        }
        BigDecimal numberValue = OperatorTools.parseBigDecimal(numberObj);
        return Math.cos(Math.toRadians(numberValue.doubleValue()));
    }
}
