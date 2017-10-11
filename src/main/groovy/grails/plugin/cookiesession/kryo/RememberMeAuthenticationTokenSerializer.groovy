package grails.plugin.cookiesession.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import groovy.util.logging.Slf4j

import java.lang.reflect.Constructor
import java.lang.reflect.Field

/**
 * Kryo serializer for RememberMeAuthenticationToken. This class is not specific to a class, but
 * does expect properties consistent with RememberMeAuthenticationToken.
 */
@Slf4j
class RememberMeAuthenticationTokenSerializer extends Serializer<Object> {
    final Class targetClass
    final Constructor constructor
    final Field keyHashField

    RememberMeAuthenticationTokenSerializer(Class targetClass) {
        this.targetClass = targetClass
        constructor = targetClass.getConstructor(String, Object, Collection)
        keyHashField = targetClass.getDeclaredField('keyHash')
        keyHashField.accessible = true
    }

    @Override
    void write(Kryo kryo, Output output, Object token) {
        log.trace 'starting writing {}', token
        output.writeInt(token.keyHash)
        kryo.writeClassAndObject(output, token.principal)
        kryo.writeClassAndObject(output, token.authorities)
        log.trace 'writing authorities: {} - {}', token.authorities.class.name, token.authorities
        kryo.writeClassAndObject(output, token.details)
        log.trace 'finished writing {}', token
    }

    @Override
    Object read(Kryo kryo, Input input, Class<Object> type) {
        log.trace 'starting reading UsernamePasswordAuthenticationToken'
        int keyHash = input.readInt()
        def principal = kryo.readClassAndObject(input)
        def authorities = kryo.readClassAndObject(input)
        log.trace 'Authorities: {}', authorities
        if (authorities) {
            authorities.each { log.trace('{}, {}', it.class.name, it) }
        }
        def details = kryo.readClassAndObject(input)

        def token = constructor.newInstance('temporary', principal, authorities)
        token.details = details
        keyHashField.set(token, keyHash)

        log.trace 'finished reading RememberMeAuthenticationToken {}', token

        return token
    }
}
