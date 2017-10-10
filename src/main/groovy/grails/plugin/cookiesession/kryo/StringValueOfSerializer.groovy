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
