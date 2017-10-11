package grails.plugin.cookiesession.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import grails.plugin.springsecurity.authentication.GrailsAnonymousAuthenticationToken
import groovy.transform.CompileStatic
import org.springframework.security.authentication.AnonymousAuthenticationToken

import java.lang.reflect.Field

/**
 * Kryo serializer for GrailsAnonymousAuthenticationToken.
 */
@CompileStatic
class GrailsAnonymousAuthenticationTokenSerializer extends Serializer<GrailsAnonymousAuthenticationToken> {
    static final Field KEY_HASH_FIELD = AnonymousAuthenticationToken.getDeclaredField('keyHash')
    static {
        KEY_HASH_FIELD.accessible = true
    }

    @Override
    void write(Kryo kryo, Output output, GrailsAnonymousAuthenticationToken object) {
        output.writeInt(object.keyHash)
        kryo.writeClassAndObject(output, object.details)
    }

    @Override
    GrailsAnonymousAuthenticationToken read(Kryo kryo, Input input, Class<GrailsAnonymousAuthenticationToken> type) {
        int keyHash = input.readInt()
        def details = kryo.readClassAndObject(input)
        GrailsAnonymousAuthenticationToken token = new GrailsAnonymousAuthenticationToken('temporary', details)
        KEY_HASH_FIELD.setInt(token, keyHash)
        return token
    }
}
