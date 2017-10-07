package grails.plugin.cookiesession.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import groovy.transform.CompileStatic
import org.springframework.security.web.authentication.WebAuthenticationDetails

import java.lang.reflect.Field

@CompileStatic
class WebAuthenticationDetailsSerializer extends Serializer<WebAuthenticationDetails> {
    static final Field REMOTEADDR_FIELD = WebAuthenticationDetails.getDeclaredField('remoteAddress')
    static final Field SESSIONID_FIELD = WebAuthenticationDetails.getDeclaredField('sessionId')
    static {
        REMOTEADDR_FIELD.accessible = true
        SESSIONID_FIELD.accessible = true
    }

    @Override
    void write(Kryo kryo, Output output, WebAuthenticationDetails details) {
        output.writeString(REMOTEADDR_FIELD.get(details) as String)
        output.writeString(SESSIONID_FIELD.get(details) as String)
    }

    @Override
    WebAuthenticationDetails read(Kryo kryo, Input input, Class<WebAuthenticationDetails> type) {
        String remoteAddr = input.readString()
        String sessionId = input.readString()
        WebAuthenticationDetails details = kryo.newInstance(WebAuthenticationDetails)
        REMOTEADDR_FIELD.set(details, remoteAddr)
        SESSIONID_FIELD.set(details, sessionId)
        details
    }
}
