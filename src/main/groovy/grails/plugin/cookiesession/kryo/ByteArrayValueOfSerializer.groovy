/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Patrick Double
 *  patrick.double@objectpartners.com or pat@patdouble.com
 */

package grails.plugin.cookiesession.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.ByteArraySerializer
import groovy.transform.CompileStatic

import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * Kryo serializer that uses the toByteArray() and constructor taking a byte[] or valueOf(byte[]) methods.
 */
@CompileStatic
class ByteArrayValueOfSerializer extends Serializer<Object> {
    final Class targetClass
    final Method toByteArrayMethod
    final Constructor constructor
    final Method valueOfMethod
    final ByteArraySerializer byteArraySerializer

    ByteArrayValueOfSerializer(Class targetClass, String valueOfMethodName = 'valueOf') throws NoSuchMethodException {
        this.targetClass = targetClass
        byteArraySerializer = new ByteArraySerializer()
        toByteArrayMethod = targetClass.getMethod('toByteArray')
        try {
            constructor = targetClass.getConstructor(byte[].class)
            valueOfMethod = null
        } catch (NoSuchMethodException e) {
            constructor = null
            valueOfMethod = targetClass.getMethod(valueOfMethodName, byte[].class)
        }
    }

    @Override
    void write(Kryo kryo, Output output, Object object) {
        byte[] bytes = object == null ? null : (byte[]) toByteArrayMethod.invoke(object)
        byteArraySerializer.write(kryo, output, bytes)
    }

    @Override
    Object read(Kryo kryo, Input input, Class<Object> type) {
        byte[] bytes = byteArraySerializer.read(kryo, input, byte[].class)
        if (bytes == null) {
            return null
        }
        Object[] args = new Object[1]
        args[0] = bytes
        if (constructor) {
            return constructor.newInstance(args)
        }
        return valueOfMethod.invoke(null, args)
    }
}
