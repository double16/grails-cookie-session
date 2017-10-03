package grails.plugin.cookiesession.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import groovy.util.logging.Slf4j

@Slf4j
class UsernamePasswordAuthenticationTokenSerializer extends Serializer<Object> {
    Class targetClass

    UsernamePasswordAuthenticationTokenSerializer() {
    }

    @Override
    void write(Kryo kryo, Output output, Object token) {
        log.trace 'starting writing {}', token
        kryo.writeClassAndObject(output, token.principal)
        kryo.writeClassAndObject(output, token.credentials)
        kryo.writeClassAndObject(output, token.authorities)
        log.trace 'writing authorities: {} - {}', token.authorities.class.name, token.authorities
        kryo.writeClassAndObject(output, token.details)
        log.trace 'finished writing {}', token
    }

    @Override
    Object read(Kryo kryo, Input input, Class<Object> type) {
        log.trace 'starting reading UsernamePasswordAuthenticationToken'
        def principal = kryo.readClassAndObject(input)
        def credentials = kryo.readClassAndObject(input)
        def authorities = kryo.readClassAndObject(input)
        log.trace 'Authorities: {}', authorities
        if (authorities) {
            authorities.each { log.trace('{}, {}', it.class.name, it) }
        }
        def details = kryo.readClassAndObject(input)

        def constructor = targetClass.getConstructor(Object, Object, Collection)
        def token = constructor.newInstance(principal, credentials, authorities)
        token.details = details

        log.trace 'finished reading UsernamePasswordAuthenticationToken {}', token

        return token
    }
}
