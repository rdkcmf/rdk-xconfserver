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
 */
package com.comcast.hesperius.dataaccess.core.rest.query;

import com.comcast.hesperius.dataaccess.core.dao.util.DataUtils;
import com.comcast.hydra.astyanax.misc.Operation;

import org.apache.commons.lang.StringUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Supported operation and serialization format:
 * byFieldName=value
 * byFieldName={Operation.name(),value}
 *
 * value is url-encoded string and may contain:
 *  - string separated by Operation.IN_SEPARATOR values of IN filter
 *  - simple value of type of fieldName
 *
 *
 *
 * Author: jmccann
 * Date: 10/20/11
 * Time: 1:56 PM
 */
public class Filter {
	private Operation operation;
	private String field;
	private ByteBuffer valueByteBuffer;
	private Object value;
	private Class valueClass;
    private List<Filter> inFilters = null;

	public Filter() { }

	public Filter(String queryKey, String queryValue, Class klass) {
		parse(queryKey, queryValue, klass);
	}

	protected void parse(String queryKey, String queryValue, Class klass) {
		if(!queryKey.startsWith("by"))
			throw new IllegalArgumentException("invalid Filter queryKey: " + queryKey);

		if(queryValue == null || queryValue.length() == 0)
			throw new IllegalArgumentException("invalid Filter value: " + queryValue);

		this.field = StringUtils.uncapitalize(queryKey.substring(2));
		String parsedValue;
		if(queryValue.startsWith("{") && queryValue.endsWith("}")) {
			// treat it like an expression
			String[] parts = queryValue.substring(1, queryValue.length()-1).split(",", 2);
			if(parts.length != 2)
				throw new IllegalArgumentException("Invalid Filter value: " + queryValue);
			operation = Operation.valueOf(parts[0].toUpperCase());
            if(operation == Operation.IN) {
                String[] inValues = parts[1].split(Operation.IN_SEPARATOR);
                if(inValues.length == 0) {
                    throw new IllegalArgumentException("Invalid Filter value: " + queryValue);
                }
                inFilters = new ArrayList<Filter>(inValues.length);
                for (int i = 0; i < inValues.length; i++) {
                   inFilters.add(new Filter(queryKey, inValues[i], klass));
                }
                return;
            } else {
			    parsedValue = parts[1];
            }
		}
		else {
			// treat it like an equality
			operation = Operation.EQUALS;
			parsedValue = queryValue;
		}

		this.valueClass = DataUtils.getFieldType(klass, field);
		if(valueClass == null)
			throw new IllegalArgumentException("Field " + field + " not found in class " + klass.getSimpleName());
		this.value = DataUtils.stringToObject(parsedValue, valueClass);
		this.valueByteBuffer = DataUtils.stringToByteBuffer(parsedValue, valueClass);
	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public ByteBuffer getValueByteBuffer() {
        if(this.operation.equals(Operation.IN)) {
            throw new IllegalArgumentException("Use getInnerFilters to obtain ByteBuffer values.");
        } else {
		    return valueByteBuffer;
        }
	}

	public void setValueByteBuffer(ByteBuffer valueByteBuffer) {
		this.valueByteBuffer = valueByteBuffer;
	}
	
	public Object getValue() {
        if(this.operation.equals(Operation.IN)) {
            throw new IllegalArgumentException("Use getInnerFilters to obtain values.");
        } else {
		    return this.value;
        }
	}

    public List<Filter> getInnerFilters() {
        return inFilters;
    }
	
	public boolean compareTo(Object obj) {
        if(this.operation.equals(Operation.IN)) {
            for (Filter inFilter : inFilters) {
                if(inFilter.compareTo(obj)) {
                    return true;
                }
            }
            return false;
        }

		if(this.operation.equals(Operation.EQUALS)) {
			return obj.equals(getValue());
		}
		else {
			if(valueClass.equals(Integer.class)) {
				Integer n1 = (Integer) obj, n2 = (Integer) value;
				if(this.operation.equals(Operation.GTE))
					return n1 >= n2;
				else if(this.operation.equals(Operation.LTE))
					return n1 <= n2;
			}
			else if(valueClass.equals(Long.class)) {
				Long n1 = (Long) obj, n2 = (Long) value;
				if(this.operation.equals(Operation.GTE))
					return n1 >= n2;
				else if(this.operation.equals(Operation.LTE))
					return n1 <= n2;
			}
			else if(valueClass.equals(Double.class)) {
				Double n1 = (Double) obj, n2 = (Double) value;
				if(this.operation.equals(Operation.GTE))
					return n1 >= n2;
				else if(this.operation.equals(Operation.LTE))
					return n1 <= n2;
			}
			else if(valueClass.equals(Float.class)) {
				Float n1 = (Float) obj, n2 = (Float) value;
				if(this.operation.equals(Operation.GTE))
					return n1 >= n2;
				else if(this.operation.equals(Operation.LTE))
					return n1 <= n2;
			}
			else if(valueClass.equals(Date.class)) {
				Date n1 = (Date) obj, n2 = (Date) value;
				if(this.operation.equals(Operation.GTE))
					return n1.after(n2);
				else if(this.operation.equals(Operation.LTE))
					return n1.before(n2);
			}
			else if(valueClass.equals(String.class)) {
				String n1 = (String) obj, n2 = (String) value;
				if(this.operation.equals(Operation.GTE))
					return n1.compareTo(n2) >= 0;
				else if(this.operation.equals(Operation.LTE))
					return n1.compareTo(n2) <= 0;
			}

		}

		return false;
	}
}
