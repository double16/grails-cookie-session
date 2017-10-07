package grails.plugin.cookiesession.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import groovy.util.logging.Slf4j

import java.lang.reflect.Constructor

@Slf4j
class GrailsUserSerializer extends Serializer<Object> {
    final Class targetClass
    final Constructor constructor

    GrailsUserSerializer(Class targetClass) {
        this.targetClass = targetClass
        constructor = targetClass.getConstructor(String, String, boolean.class, boolean.class, boolean.class, boolean.class, Collection, Object)
    }

    @Override
    void write(Kryo kryo, Output output, Object user) {
        log.trace 'starting writing {}', user
        //NOTE: not writing authorities on purpose - those get written as part of the UsernamePasswordAuthenticationToken
        output.writeString(user.id as String)
        output.writeString(user.username as String)
        output.writeBoolean(user.accountNonExpired as boolean)
        output.writeBoolean(user.accountNonLocked as boolean)
        output.writeBoolean(user.credentialsNonExpired as boolean)
        output.writeBoolean(user.enabled as boolean)
        //kryo.writeClassAndObject(output,user.authorities)
        log.trace 'finished writing {}', user
    }

    @Override
    Object read(Kryo kryo, Input input, Class<Object> type) {
        log.trace 'starting reading GrailsUser'
        String id = input.readString()
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
                authorities,
                id)
        log.trace 'finished reading {}', user
        return user
    }
}
