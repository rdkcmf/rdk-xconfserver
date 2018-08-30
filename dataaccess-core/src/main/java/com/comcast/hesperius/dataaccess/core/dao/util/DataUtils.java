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
package com.comcast.hesperius.dataaccess.core.dao.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import com.comcast.hesperius.dataaccess.core.rest.util.SerializationUtils;
import com.netflix.astyanax.model.Composite;
import com.netflix.astyanax.serializers.BooleanSerializer;
import com.netflix.astyanax.serializers.ByteBufferSerializer;
import com.netflix.astyanax.serializers.DateSerializer;
import com.netflix.astyanax.serializers.DoubleSerializer;
import com.netflix.astyanax.serializers.FloatSerializer;
import com.netflix.astyanax.serializers.IntegerSerializer;
import com.netflix.astyanax.serializers.LongSerializer;
import com.netflix.astyanax.serializers.StringSerializer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Author: jmccann
 * Date: 10/21/11
 * Time: 11:23 AM
 * @deprecated {@link com.comcast.hesperius.dataaccess.core.util.bean.BeanUtils} must be used
 */
@Deprecated
public class DataUtils {
    private static final Logger log = LoggerFactory.getLogger(DataUtils.class);

    public static Method getGetter(Object obj, String field) throws NoSuchMethodException {
        try {
            return SerializationUtils.getMethod(obj, "get" + StringUtils.capitalize(field));
        }
        catch(NoSuchMethodException nme) { }

        // try to get Getter as Boolean type
		return SerializationUtils.getMethod(obj, "is" + StringUtils.capitalize(field));
	}

    public static Object invokeGetter(Object obj, String field) {
		try {
			Method m = getGetter(obj, field);
			return m.invoke(obj);
		}
		catch(NoSuchMethodException nme) { }
		catch(IllegalAccessException iae) { }
		catch(InvocationTargetException ite) {
            log.warn(String.format("Failed to get property value for %s, class %s", field, obj.getClass()), ite);
        }

		return null;
	}

    public static Object stringToObject(String value, Class type) {
		if(type.equals(String.class))
			return value;
		else if(type.equals(Boolean.class))
			return Boolean.valueOf(value);
		else if(type.equals(Date.class)) {
			try {
				return DateFormat.getInstance().parse(value);
			}
			catch(ParseException dpe) { }	// return null
		}
		else if(type.equals(Double.class))
			return Double.valueOf(value);
		else if(type.equals(Float.class))
			return Float.valueOf(value);
		else if(type.equals(Integer.class))
			return Integer.valueOf(value);
		else if(type.equals(Long.class))
			return Long.valueOf(value);
        else if (type.isEnum()) {
            return Enum.valueOf(type, value);
        }
		return null;
	}

    public static Method getSetter(Object obj, String field, Class klass) throws NoSuchMethodException {
		for(Method method : obj.getClass().getMethods()) {
			if(SerializationUtils.getMethodSimpleName(method).equals("set" + StringUtils.capitalize(field)))
				return method;
		}
		return null;
	}
    public static boolean invokeSetter(Object obj, String field, ByteBuffer value) {
		try {
			Method m = getSetter(obj, field, value.getClass());
			if(m != null) {
				String name = m.getParameterTypes()[0].getSimpleName();
				if(name.equals("String"))
					m.invoke(obj, StringSerializer.get().fromByteBuffer(value));
				else if(name.equals("Boolean"))
					m.invoke(obj, BooleanSerializer.get().fromByteBuffer(value));
				else if(name.equals("Date"))
					m.invoke(obj, DateSerializer.get().fromByteBuffer(value));
				else if(name.equals("Double"))
					m.invoke(obj, DoubleSerializer.get().fromByteBuffer(value));
				else if(name.equals("Float"))
					m.invoke(obj, FloatSerializer.get().fromByteBuffer(value));
				else if(name.equals("Integer"))
					m.invoke(obj, IntegerSerializer.get().fromByteBuffer(value));
				else if(name.equals("Long"))
					m.invoke(obj, LongSerializer.get().fromByteBuffer(value));
				return true;
			}
		}
		catch(NoSuchMethodException nme) { }
		catch(IllegalAccessException iae) { }
		catch(InvocationTargetException ite) { }

		return false;
	}

    public static Class getFieldType(Class klass, String field) {
		try {
			return klass.getMethod("get" + StringUtils.capitalize(field)).getReturnType();
			//return klass.getField(field).getType();
		} catch(NoSuchMethodException nsfe) {
            // try to get Getter as Boolean type
            try {
                return klass.getMethod("is" + StringUtils.capitalize(field)).getReturnType();
            } catch (NoSuchMethodException nsfe2) {
                // caller will handle this because null is returned
            }
		}
		return null;
	}

    public static String[] parseComparatorsDefinition(String comparatorAlias) {
        Boolean isComposite = comparatorAlias.endsWith(")");
        String[] comparatorsStr;
        if (isComposite) {
            comparatorsStr = comparatorAlias.split(",");
            comparatorsStr[0] = comparatorsStr[0].substring(comparatorsStr[0].indexOf('(') + 1);
            String lastToken = comparatorsStr[comparatorsStr.length - 1];
            comparatorsStr[comparatorsStr.length - 1] = lastToken.substring(0, lastToken.indexOf(')'));
        } else {
            comparatorsStr = new String[]{comparatorAlias};
        }

        return comparatorsStr;
    }


    public static ByteBuffer stringToByteBuffer(String value, Class type) {
        return toByteBuffer(stringToObject(value, type));
    }

    public static ByteBuffer toByteBuffer(Object value) {
        if(value instanceof String)
            return StringSerializer.get().toByteBuffer((String)value);
        else if(value instanceof Boolean)
            return BooleanSerializer.get().toByteBuffer((Boolean) value);
        else if(value instanceof Date)
            return DateSerializer.get().toByteBuffer((Date)value);
        else if(value instanceof Double)
            return DoubleSerializer.get().toByteBuffer((Double) value);
        else if(value instanceof Float)
            return FloatSerializer.get().toByteBuffer((Float) value);
        else if(value instanceof Integer)
            return IntegerSerializer.get().toByteBuffer((Integer) value);
        else if(value instanceof Long)
            return LongSerializer.get().toByteBuffer((Long) value);
        return null;
    }

    /**
     * De-serializes ByteBuffers and Composites of ByteBuffers, so their contents can be seen. Useful for inspecting
     * data from Cassandra before it has been mapped to an object.
     * @return A version of the given object with its ByteBuffers de-serialized.
     */
    public static Object genericByteBufferDeserializer(Object o) {
        if (o instanceof Composite) {
            Composite visible = new Composite();
            for (Object component : (Composite)o) {
                if (component instanceof ByteBuffer)
                    visible.add(byteBufferToString((ByteBuffer)component));
                else
                    visible.add(o); // not a byte buffer -- hope it's something readable
            }
            return visible;
        } else if (o instanceof ByteBuffer) {
            return byteBufferToString((ByteBuffer)o);
        } else {
            return o;
        }
    }

    /**
     * Produces a somewhat readable string representation of <i>buf</i> when you don't have a deserializer for it.
     * Similar to ByteBufferSerializer.getString(), which produces 2 hex digits per byte.
     * @param buf
     * @return The bytes of <i>buf</i> as URL encoded ISO-8859-1 characters (one unsigned byte per character).
     */
    public static String byteBufferToString(ByteBuffer buf) {
        byte[] b = new byte[buf.remaining()];
        buf.duplicate().get(b); // duplicate or get() eats bytes from buf
        String s = new String(b, Charset.forName("ISO-8859-1")); // 1 char per byte, valid encodings for all bytes
        try {
            return URLEncoder.encode(s, "ISO-8859-1");
        } catch (UnsupportedEncodingException ex) {
            return ByteBufferSerializer.get().getString(buf); // should never happen as ISO-8859-1 is required on JVMs
        }
    }

    /**
     * @return The prefix of buf with at most length bytes remaining, or buf if length is 0.
     */
    public static ByteBuffer truncate(ByteBuffer buf, int length) {
        if (0 < length && length < buf.remaining()) {
            log.info("Truncating object field value to {} bytes: {}", length,
                    new String(buf.array(), buf.position(), Math.min(500, buf.remaining()))+"...");
            return ByteBuffer.wrap(buf.array(), buf.position(), length);
        } else
            return buf;
    }
}

