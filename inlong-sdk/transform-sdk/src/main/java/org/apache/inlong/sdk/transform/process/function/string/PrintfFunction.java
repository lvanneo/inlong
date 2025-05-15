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

import java.util.ArrayList;
import java.util.List;
/**
 * PrintfFunction  ->  printf(strfmt [,obj ,...])
 * description:
 * - Return a formatted string from printf-style format strings
 */
@TransformFunction(type = FunctionConstant.STRING_TYPE, names = {
        "printf"}, parameter = "(String strfmt [, Object obj, ...])", descriptions = {
                "- Return a formatted string from printf-style format strings."}, examples = {
                        "printf(\"User %s has %d points and a balance of %.2f.\", \"Bob\", 1500, 99.99) = \" User Bob has "
                                +
                                "1500 points and a balance of 99.99.\""})
public class PrintfFunction implements ValueParser {

    private ValueParser strfmtParser;
    private List<ValueParser> argsParser = new ArrayList<>();
    public PrintfFunction(Function expr) {
        List<Expression> expressions = expr.getParameters().getExpressions();
        strfmtParser = OperatorTools.buildParser(expressions.get(0));
        for (int i = 1; i < expressions.size(); i++) {
            argsParser.add(OperatorTools.buildParser(expressions.get(i)));
        }
    }

    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        Object strfmtObj = strfmtParser.parse(sourceData, rowIndex, context);
        String strfmt = OperatorTools.parseString(strfmtObj);
        int size = argsParser.size();
        Object[] args = new Object[size];
        for (int i = 0; i < size; i++) {
            Object parsed = argsParser.get(i).parse(sourceData, rowIndex, context);
            Object arg = parsed == null ? "null" : parsed.toString();
            args[i] = parse(arg);
        }
        return String.format(strfmt, args);
    }

    public Object parse(Object obj) {
        if (isInteger(obj)) {
            obj = Integer.parseInt(obj.toString());
        } else if (isFloat(obj)) {
            obj = Float.parseFloat(obj.toString());
        }
        return obj;
    }

    public boolean isFloat(Object obj) {
        return obj.toString().matches("^(-?\\d+)(\\.\\d+)?$");
    }

    public boolean isInteger(Object obj) {
        return obj.toString().matches("^-?\\d+$");
    }

}
