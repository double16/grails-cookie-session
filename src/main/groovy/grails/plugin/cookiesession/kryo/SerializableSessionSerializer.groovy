/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Patrick Double
 *  patrick.double@objectpartners.com or pat@patdouble.com
 */
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
