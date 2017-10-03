package grails.plugin.cookiesession.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import groovy.util.logging.Slf4j

import java.lang.reflect.Constructor

@Slf4j
class GrantedAuthorityImplSerializer extends Serializer<Object> {
    Class targetClass

    @Override
    void write(Kryo kryo, Output output, Object grantedAuth) {
        log.trace 'started writing GrantedAuthorityImpl {}', grantedAuth
        kryo.writeObject(output, grantedAuth.authority)
        log.trace 'finished writing GrantedAuthorityImpl {}', grantedAuth
    }

    @Override
    Object read(Kryo kryo, Input input, Class<Object> type) {
        log.trace 'starting reading GrantedAuthorityImpl'
        def role = kryo.readObject(input, String)

        Constructor constructor = targetClass.getConstructor(String)
        def ga = constructor.newInstance(role)

        log.trace 'finished reading GrantedAuthorityImpl: {}', ga
        return ga
    }
}
