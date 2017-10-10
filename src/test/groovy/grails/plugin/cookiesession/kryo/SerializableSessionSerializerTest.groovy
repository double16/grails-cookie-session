package grails.plugin.cookiesession.kryo

import grails.plugin.cookiesession.SerializableSession

class SerializableSessionSerializerTest extends AbstractKryoSerializerTest {
    void setup() {
        kryo.register(SerializableSession, new SerializableSessionSerializer())
    }

    void "serialize and deserialize"() {
        given:
        SerializableSession session = new SerializableSession()

        when:
        SerializableSession session2 = serde(session)
        then:
        session2.creationTime == session.creationTime
        session2.lastAccessedTime == session.lastAccessedTime
        session2.attributes == session.attributes
    }
}
