package grails.plugin.cookiesession.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.DefaultSerializers
import com.esotericsoftware.kryo.serializers.MapSerializer
import groovy.transform.CompileStatic
import org.grails.web.servlet.GrailsFlashScope

import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

/**
 * Kryo serializer for org.grails.web.servlet.GrailsFlashScope.
 */
@CompileStatic
class GrailsFlashScopeSerializer extends Serializer<GrailsFlashScope> {
    static final Field CURRENT_FIELD = GrailsFlashScope.getDeclaredField('current')
    static final Field NEXT_FIELD = GrailsFlashScope.getDeclaredField('next')
    static {
        CURRENT_FIELD.accessible = true
        NEXT_FIELD.accessible = true
    }

    private final MapSerializer mapSerializer

    GrailsFlashScopeSerializer() {
        mapSerializer = new MapSerializer()
        mapSerializer.setKeysCanBeNull(false)
        mapSerializer.setValuesCanBeNull(false)
        mapSerializer.setKeyClass(String, new DefaultSerializers.StringSerializer())
    }

    @Override
    void write(Kryo kryo, Output output, GrailsFlashScope object) {
        kryo.writeObject(output, CURRENT_FIELD.get(object), mapSerializer)
        kryo.writeObject(output, NEXT_FIELD.get(object), mapSerializer)
    }

    @Override
    GrailsFlashScope read(Kryo kryo, Input input, Class<GrailsFlashScope> type) {
        Map current = kryo.readObject(input, ConcurrentHashMap, mapSerializer)
        Map next = kryo.readObject(input, ConcurrentHashMap, mapSerializer)
        GrailsFlashScope gfs = kryo.newInstance(GrailsFlashScope)
        CURRENT_FIELD.set(gfs, current)
        NEXT_FIELD.set(gfs, next)
        return gfs
    }
}
