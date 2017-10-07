package grails.plugin.cookiesession.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import groovy.util.logging.Slf4j

import java.lang.reflect.Constructor

/**
 * Serializer for GrantedAuthority objects that hold a single String. The target class
 * is expected to have a property named 'authority'.
 */
@Slf4j
class GrantedAuthoritySerializer extends Serializer<Object> {
    final Class targetClass
    final Constructor constructor

    GrantedAuthoritySerializer(Class targetClass) {
        this.targetClass = targetClass
        constructor = targetClass.getConstructor(String)
    }

    @Override
    void write(Kryo kryo, Output output, Object grantedAuth) {
        log.trace 'started writing GrantedAuthority {}', grantedAuth
        output.writeString(grantedAuth.authority as String)
        log.trace 'finished writing GrantedAuthority {}', grantedAuth
    }

    @Override
    Object read(Kryo kryo, Input input, Class<Object> type) {
        log.trace 'starting reading GrantedAuthority'
        def role = input.readString()
        def ga = constructor.newInstance(role)
        log.trace 'finished reading GrantedAuthority: {}', ga
        return ga
    }
}
