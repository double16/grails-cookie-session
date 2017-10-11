package grails.plugin.cookiesession.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.DefaultSerializers
import com.esotericsoftware.kryo.serializers.MapSerializer
import grails.plugin.cookiesession.SerializableSession
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Kryo serializer for SerializableSession. The deserialized session will be marked as not new and not dirty.
 */
@Slf4j
@CompileStatic
class SerializableSessionSerializer extends Serializer<SerializableSession> {
    private final MapSerializer mapSerializer

    SerializableSessionSerializer() {
        mapSerializer = new MapSerializer()
        mapSerializer.setKeysCanBeNull(false)
        mapSerializer.setValuesCanBeNull(false)
        mapSerializer.setKeyClass(String, new DefaultSerializers.StringSerializer())
    }

    @Override
    void write(Kryo kryo, Output output, SerializableSession session) {
        log.trace 'starting writing {}', session
        output.writeLong(session.creationTime, true)
        output.writeLong(session.lastAccessedTime, true)
        kryo.writeObject(output, session.attributes, mapSerializer)
        log.trace 'finished writing {}', session
    }

    @Override
    SerializableSession read(Kryo kryo, Input input, Class<SerializableSession> type) {
        log.trace 'starting reading SerializableSession'
        long creationTime = input.readLong(true)
        long lastAccessedTime = input.readLong(true)
        Map<String, Serializable> attributes = kryo.readObject(input, HashMap, mapSerializer)
        SerializableSession session = new SerializableSession(creationTime, lastAccessedTime, attributes)
        log.trace 'finished reading {}', session
        return session
    }
}
