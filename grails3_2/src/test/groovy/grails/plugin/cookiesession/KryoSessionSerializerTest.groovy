package grails.plugin.cookiesession

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import spock.lang.Specification

@TestMixin([GrailsUnitTestMixin, ControllerUnitTestMixin])
class KryoSessionSerializerTest extends Specification implements SessionFixture {
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
