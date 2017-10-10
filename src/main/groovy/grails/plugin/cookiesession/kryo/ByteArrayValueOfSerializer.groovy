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
