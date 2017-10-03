package grails.plugin.cookiesession.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import groovy.util.logging.Slf4j

import java.lang.reflect.Constructor

@Slf4j
class GrailsUserSerializer extends Serializer<Object> {
    Class targetClass

    GrailsUserSerializer() {
    }

    @Override
    void write(Kryo kryo, Output output, Object user) {
        log.trace 'starting writing {}', user
        //NOTE: note writing authorities on purpose - those get written as part of the UsernamePasswordAuthenticationToken
        kryo.writeObject(output, user.id)
        kryo.writeObject(output, user.username)
        kryo.writeObject(output, user.accountNonExpired)
        kryo.writeObject(output, user.accountNonLocked)
        kryo.writeObject(output, user.credentialsNonExpired)
        kryo.writeObject(output, user.enabled)
        //kryo.writeClassAndObject(output,user.authorities)
        log.trace 'finished writing {}', user
    }

    @Override
    Object read(Kryo kryo, Input input, Class<Object> type) {
        log.trace 'starting reading GrailsUser'
        String id = kryo.readObject(input, String)
        String username = kryo.readObject(input, String)
        boolean accountNonExpired = kryo.readObject(input, Boolean)
        boolean accountNonLocked = kryo.readObject(input, Boolean)
        boolean credentialsNonExpired = kryo.readObject(input, Boolean)
        boolean enabled = kryo.readObject(input, Boolean)
        def authorities = []
        Constructor constructor = targetClass.getConstructor(String, String, boolean.class, boolean.class, boolean.class, boolean.class, Collection, Object)
        def user = constructor.newInstance(username,
                '',
                enabled,
                accountNonExpired,
                credentialsNonExpired,
                accountNonLocked,
                authorities,
                id)
        log.trace 'finished reading {}', user
        return user
    }
}
