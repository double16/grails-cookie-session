package grails.plugin.cookiesession.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer
import groovy.transform.CompileStatic
import org.springframework.security.web.authentication.WebAuthenticationDetails

import java.lang.reflect.Field

@CompileStatic
class WebAuthenticationDetailsSerializer extends Serializer<WebAuthenticationDetails> {
    static final Field REMOTEADDR_FIELD = WebAuthenticationDetails.getDeclaredField('remoteAddress')
    static final Field SESSIONID_FIELD = WebAuthenticationDetails.getDeclaredField('sessionId')
    static {
        REMOTEADDR_FIELD.setAccessible(true)
        SESSIONID_FIELD.setAccessible(true)
    }

    private final StringSerializer stringSerializer

    WebAuthenticationDetailsSerializer() {
        stringSerializer = new StringSerializer()
    }

    @Override
    void write(Kryo kryo, Output output, WebAuthenticationDetails details) {
        kryo.writeObject(output, REMOTEADDR_FIELD.get(details), stringSerializer)
        kryo.writeObjectOrNull(output, SESSIONID_FIELD.get(details), stringSerializer)
    }

    @Override
    WebAuthenticationDetails read(Kryo kryo, Input input, Class<WebAuthenticationDetails> type) {
        String remoteAddr = kryo.readObject(input, String, stringSerializer)
        String sessionId = kryo.readObjectOrNull(input, String, stringSerializer)
        WebAuthenticationDetails details = kryo.newInstance(WebAuthenticationDetails)
        REMOTEADDR_FIELD.set(details, remoteAddr)
        SESSIONID_FIELD.set(details, sessionId)
        details
    }
}
