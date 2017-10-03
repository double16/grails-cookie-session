package grails.plugin.cookiesession.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.serializers.FieldSerializer
import groovy.transform.CompileStatic
import org.springframework.security.web.savedrequest.DefaultSavedRequest

@CompileStatic
class DefaultSavedRequestSerializer extends FieldSerializer<DefaultSavedRequest> {
    DefaultSavedRequestSerializer(Kryo kryo) {
        super(kryo, DefaultSavedRequest)
    }
}
