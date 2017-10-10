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
    final Class targetClass
    final Constructor constructor

    UserSerializer(Class targetClass) {
        this.targetClass = targetClass
        constructor = targetClass.getConstructor(String, String, boolean.class, boolean.class,
                boolean.class, boolean.class, Collection)
    }

    @Override
    void write(Kryo kryo, Output output, Object user) {
        log.trace 'starting writing {}', user
        //NOTE: not writing authorities on purpose - those get written as part of the UsernamePasswordAuthenticationToken
        output.writeString(user.username as String)
        output.writeBoolean(user.isAccountNonExpired() as boolean)
        output.writeBoolean(user.isAccountNonLocked() as boolean)
        output.writeBoolean(user.isCredentialsNonExpired() as boolean)
        output.writeBoolean(user.isEnabled() as boolean)
        //kryo.writeClassAndObject(output,user.authorities)
        log.trace 'finished writing {}', user
    }

    @Override
    Object read(Kryo kryo, Input input, Class<Object> type) {
        log.trace 'starting reading GrailsUser'
        String username = input.readString()
        boolean accountNonExpired = input.readBoolean()
        boolean accountNonLocked = input.readBoolean()
        boolean credentialsNonExpired = input.readBoolean()
        boolean enabled = input.readBoolean()
        def authorities = []
        def user = constructor.newInstance(username,
                '',
                enabled,
                accountNonExpired,
                credentialsNonExpired,
                accountNonLocked,
                authorities)
        log.trace 'finished reading {}', user
        return user
    }
}
