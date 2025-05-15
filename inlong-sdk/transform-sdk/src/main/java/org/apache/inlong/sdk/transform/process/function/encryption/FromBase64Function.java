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

package org.apache.inlong.sdk.transform.process.function.encryption;

import org.apache.inlong.sdk.transform.decode.SourceData;
import org.apache.inlong.sdk.transform.process.Context;
import org.apache.inlong.sdk.transform.process.function.FunctionConstant;
import org.apache.inlong.sdk.transform.process.function.TransformFunction;
import org.apache.inlong.sdk.transform.process.operator.OperatorTools;
import org.apache.inlong.sdk.transform.process.parser.ValueParser;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * FromBase64Function  ->  from_base64(base64Str)
 * description:
 * - Return NULL if base64Str is NULL
 * - Return the base64-decoded result from string
 */
@TransformFunction(type = FunctionConstant.ENCRYPTION_TYPE, names = {
        "from_base64"}, parameter = "(String base64Str)", descriptions = {
                "- Return \"\" if 'base64Str' is NULL;",
                "- Return the base64-decoded result from 'base64Str'."
        }, examples = {"from_base64('QXBhY2hlIEluTG9uZw==') = \"Apache InLong\""})
public class FromBase64Function implements ValueParser {

    private final ValueParser stringParser;

    public FromBase64Function(Function expr) {
        List<Expression> expressions = expr.getParameters().getExpressions();
        stringParser = OperatorTools.buildParser(expressions.get(0));
    }

    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        Object stringObj = stringParser.parse(sourceData, rowIndex, context);
        if (stringObj == null) {
            return null;
        }
        String encodedString = OperatorTools.parseString(stringObj);

        if (encodedString == null) {
            return null;
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // handle decoding exceptions and log exception information
            throw new RuntimeException("Invalid Base64 input: " + encodedString, e);
        }
    }
}
