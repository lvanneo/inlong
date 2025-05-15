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

package org.apache.inlong.sdk.transform.process.function.string;

import org.apache.inlong.sdk.transform.decode.SourceData;
import org.apache.inlong.sdk.transform.process.Context;
import org.apache.inlong.sdk.transform.process.function.FunctionConstant;
import org.apache.inlong.sdk.transform.process.function.TransformFunction;
import org.apache.inlong.sdk.transform.process.operator.OperatorTools;
import org.apache.inlong.sdk.transform.process.parser.ValueParser;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;

import java.util.List;

/**
 * ReplaceFunction  ->  replace(s, s1, s2)
 * description:
 * - Return NULL if any parameter is null
 * - Return the result of replacing string 's1' with string 's2' in string 's'
 */
@TransformFunction(type = FunctionConstant.STRING_TYPE, names = {
        "replace"}, parameter = "(String s, String s1, String s2)", descriptions = {
                "- Return \"\" if any parameter is null;",
                "- Return the result of replacing string 's1' with string 's2' in string 's'."
        }, examples = {"replace('Hello World', '', 'J') = \"JHJeJlJlJoJ JWJoJrJlJdJ\""})
public class ReplaceFunction implements ValueParser {

    private ValueParser stringParser;
    private ValueParser targetParser;
    private ValueParser replacementParser;

    public ReplaceFunction(Function expr) {
        List<Expression> expressions = expr.getParameters().getExpressions();
        stringParser = OperatorTools.buildParser(expressions.get(0));
        targetParser = OperatorTools.buildParser(expressions.get(1));
        replacementParser = OperatorTools.buildParser(expressions.get(2));
    }

    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        Object strObj = stringParser.parse(sourceData, rowIndex, context);
        Object targetObj = targetParser.parse(sourceData, rowIndex, context);
        Object replacementObj = replacementParser.parse(sourceData, rowIndex, context);
        if (strObj == null || targetObj == null || replacementObj == null) {
            return null;
        }
        String str = OperatorTools.parseString(strObj);
        String target = OperatorTools.parseString(targetObj);
        String replacement = OperatorTools.parseString(replacementObj);
        return str.replace(target, replacement);
    }
}