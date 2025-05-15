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

package org.apache.inlong.sdk.transform.process.function.collection;

import org.apache.inlong.sdk.transform.decode.SourceData;
import org.apache.inlong.sdk.transform.process.Context;
import org.apache.inlong.sdk.transform.process.function.FunctionConstant;
import org.apache.inlong.sdk.transform.process.function.TransformFunction;
import org.apache.inlong.sdk.transform.process.operator.OperatorTools;
import org.apache.inlong.sdk.transform.process.parser.ValueParser;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
/**
 * ArraySortFunction  ->  ARRAY_SORT(array[, ascending_order[, null_first]])
 * description:
 * - Return NULL if 'array' is null;
 * - Return the array in sorted order.
 * Note: The function sorts an array, defaulting to ascending order with NULLs at the start when only the array is input.
 *       Specifying ascending_order as true orders the array in ascending with NULLs first, and setting it to false orders
 *       it in descending with NULLs last. Independently, null_first as true moves NULLs to the beginning, and as false
 *       to the end, irrespective of the sorting order. The function returns null if any input is null.
 */
@TransformFunction(type = FunctionConstant.COLLECTION_TYPE, names = {
        "array_sort"}, parameter = "(Array array[,Boolean ascending_order[,Boolean null_first]])", descriptions = {
                "- Return \"\" if 'array' is null;", "- Return the array in sorted order.",
                "Note: The function sorts an array, defaulting to ascending order with NULLs at the start when only the "
                        +
                        "array is input. Specifying 'ascending_order' as true orders the array in ascending with NULLs first"
                        +
                        ", and setting it to false orders it in descending with NULLs last. Independently, 'null_first' as true "
                        +
                        "moves NULLs to the beginning, and as false to the end, irrespective of the sorting order."
        }, examples = {
                "array_sort(array('he',7,'xxd')) = [7, he, xxd]", "array_sort(array(3,7,5)) = [3, 5, 7]",
                "array_sort(array(,3,7),false,false) = [7, 3, ]",
                "array_sort(array(3,7,),true,false) = [3, 7, ]"
        })
public class ArraySortFunction implements ValueParser {

    private ValueParser arrayParser;

    private ValueParser ascendingOrderParser;

    private ValueParser nullFirstParser;

    public ArraySortFunction(Function expr) {
        List<Expression> expressions = expr.getParameters().getExpressions();
        if (!expressions.isEmpty()) {
            this.arrayParser = OperatorTools.buildParser(expr.getParameters().getExpressions().get(0));
            if (expressions.size() >= 2) {
                this.ascendingOrderParser = OperatorTools.buildParser(expr.getParameters().getExpressions().get(1));
                if (expressions.size() >= 3) {
                    this.nullFirstParser = OperatorTools.buildParser(expr.getParameters().getExpressions().get(2));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        Object arrayObj = arrayParser.parse(sourceData, rowIndex, context);
        Object ascendingOrderObj = null;
        Object nullFirstObj = null;
        if (ascendingOrderParser != null) {
            ascendingOrderObj = ascendingOrderParser.parse(sourceData, rowIndex, context);
        }
        if (nullFirstParser != null) {
            nullFirstObj = nullFirstParser.parse(sourceData, rowIndex, context);
        }
        if (arrayObj == null) {
            return null;
        }
        // default value
        boolean ascendingOrder = ascendingOrderObj == null || OperatorTools.parseBoolean(ascendingOrderObj);
        boolean nullFirst = nullFirstObj == null || OperatorTools.parseBoolean(nullFirstObj);

        if (arrayObj instanceof ArrayList) {
            ArrayList<Object> array = (ArrayList<Object>) arrayObj;
            if (array.isEmpty()) {
                return null;
            }
            // sort array
            return arraySort(array, ascendingOrder, nullFirst);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Object> arraySort(List<Object> array, Boolean ascendingOrder, Boolean nullFirst) {
        if (array == null || ascendingOrder == null || nullFirst == null) {
            return null;
        }

        // Separate the null and non-null elements
        List<Object> nonNullElements = new ArrayList<>();
        List<Object> nullElements = new ArrayList<>();

        for (Object element : array) {
            if (element == null || element.equals("")) {
                nullElements.add(element);
            } else {
                if (element instanceof Comparable) {
                    nonNullElements.add(element);
                } else {
                    throw new IllegalArgumentException("Array contains non-comparable elements.");
                }
            }
        }

        // Sort the non-null elements based on the ascendingOrder flag
        if (ascendingOrder) {
            Collections.sort(nonNullElements, new Comparator<Object>() {

                @SuppressWarnings("unchecked")
                @Override
                public int compare(Object o1, Object o2) {
                    return ((Comparable<Object>) o1).compareTo(o2);
                }
            });
        } else {
            Collections.sort(nonNullElements, new Comparator<Object>() {

                @SuppressWarnings("unchecked")
                @Override
                public int compare(Object o1, Object o2) {
                    return ((Comparable<Object>) o2).compareTo(o1);
                }
            });
        }

        // Combine null and non-null elements based on nullFirst flag
        List<Object> sortedArray = new ArrayList<>();
        if (nullFirst) {
            // NULLs go first
            sortedArray.addAll(nullElements);
            sortedArray.addAll(nonNullElements);
        } else {
            // NULLs go last
            sortedArray.addAll(nonNullElements);
            sortedArray.addAll(nullElements);
        }

        return sortedArray;
    }
}
