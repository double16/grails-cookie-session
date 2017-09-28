package grails.plugin.cookiesession

import grails.testing.web.controllers.ControllerUnitTest
import grails3_3.SecureController
import spock.lang.Specification

class KryoSessionSerializerTest extends Specification implements SessionFixture, ControllerUnitTest<SecureController> {
    KryoSessionSerializer serializer

    def setup() {
        serializer = new KryoSessionSerializer()
        serializer.grailsApplication = grailsApplication
    }

    void "serialize empty session"() {
        given:
        def session = emptySession()
        when:
        def session2 = serializer.deserialize(serializer.serialize(session))
        then:
        equals(session, session2)
    }

    void "serialize flash scope session"() {
        given:
        def session = flashScopeSession()
        when:
        def session2 = serializer.deserialize(serializer.serialize(session))
        then:
        equals(session, session2)
    }
}
