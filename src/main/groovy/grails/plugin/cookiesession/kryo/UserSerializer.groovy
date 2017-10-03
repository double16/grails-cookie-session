package grails.plugin.cookiesession.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import groovy.util.logging.Slf4j

import java.lang.reflect.Constructor

/**
 * Kryo serializer for org.springframework.security.core.userdetails.User.
 */
@Slf4j
class UserSerializer extends Serializer<Object> {
    Class targetClass

    @Override
    void write(Kryo kryo, Output output, Object user) {
        log.trace 'starting writing {}', user
        //NOTE: note writing authorities on purpose - those get written as part of the UsernamePasswordAuthenticationToken
        kryo.writeObject(output, user.username)
        kryo.writeObject(output, user.isAccountNonExpired())
        kryo.writeObject(output, user.isAccountNonLocked())
        kryo.writeObject(output, user.isCredentialsNonExpired())
        kryo.writeObject(output, user.isEnabled())
        //kryo.writeClassAndObject(output,user.authorities)
        log.trace 'finished writing {}', user
    }

    @Override
    Object read(Kryo kryo, Input input, Class<Object> type) {
        log.trace 'starting reading GrailsUser'
        String username = kryo.readObject(input, String)
        String accountNonExpired = kryo.readObject(input, String)
        boolean accountNonLocked = kryo.readObject(input, Boolean)
        boolean credentialsNonExpired = kryo.readObject(input, Boolean)
        boolean enabled = kryo.readObject(input, Boolean)
        def authorities = []
        Constructor constructor = targetClass.getConstructor(String, String, boolean.class, boolean.class, boolean.class, boolean.class, Collection)
        def user = constructor.newInstance(username,
                "",
                enabled,
                accountNonExpired,
                credentialsNonExpired,
                accountNonLocked,
                authorities)
        log.trace 'finished reading {}', user
        return user
    }
}
