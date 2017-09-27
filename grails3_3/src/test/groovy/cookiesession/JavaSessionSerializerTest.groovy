package cookiesession

import grails.plugin.cookiesession.JavaSessionSerializer
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import spock.lang.Specification

@TestMixin([GrailsUnitTestMixin, ControllerUnitTestMixin])
class JavaSessionSerializerTest extends Specification implements SessionFixture {
    JavaSessionSerializer serializer

    def setup() {
        serializer = new JavaSessionSerializer()
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
