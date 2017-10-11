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
import groovy.transform.CompileStatic

import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * Kryo serializer that uses the toString() and constructor taking a String or valueOf(String) methods.
 */
@CompileStatic
class StringValueOfSerializer extends Serializer<Object> {
    final Class targetClass
    final Constructor constructor
    final Method valueOfMethod

    StringValueOfSerializer(Class targetClass, String valueOfMethodName = 'valueOf') throws NoSuchMethodException {
        this.targetClass = targetClass
        try {
            try {
                constructor = targetClass.getConstructor(String)
            } catch (NoSuchMethodException e) {
                constructor = targetClass.getConstructor(CharSequence)
            }
            valueOfMethod = null
        } catch (NoSuchMethodException e) {
            constructor = null
            try {
                valueOfMethod = targetClass.getMethod(valueOfMethodName, String)
            } catch (NoSuchMethodException e2) {
                valueOfMethod = targetClass.getMethod(valueOfMethodName, CharSequence)
            }
        }
    }

    @Override
    void write(Kryo kryo, Output output, Object object) {
        output.writeString(object?.toString())
    }

    @Override
    Object read(Kryo kryo, Input input, Class<Object> type) {
        String str = input.readString()
        if (str == null) {
            return null
        }
        if (constructor) {
            return constructor.newInstance(str)
        }
        return valueOfMethod.invoke(null, str)
    }
}
