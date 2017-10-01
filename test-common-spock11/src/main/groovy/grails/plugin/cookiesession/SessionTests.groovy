package grails.plugin.cookiesession

import org.junit.Test
import spock.lang.Specification

import javax.servlet.http.HttpSession

abstract class SessionTests extends Specification {
    abstract String serializeSession(SerializableSession session)

    abstract SerializableSession deserializeSession(String serialized)

    abstract SerializableSession emptySession()

    abstract SerializableSession flashScopeSession()

    abstract SerializableSession authenticatedSession()

    abstract SerializableSession preauthWithSavedRequestSession()

    abstract boolean equals(HttpSession session1, HttpSession session2)

    abstract int maxSessionSize()

    @Test
    void "serialize empty session"() {
        given:
        def session = emptySession()
        when:
        def serialized = serializeSession(session)
        def session2 = deserializeSession(serialized)
        then:
        equals(session, session2)
        and:
        serialized.length() < maxSessionSize()
    }

    @Test
    void "serialize flash scope session"() {
        given:
        def session = flashScopeSession()
        when:
        def serialized = serializeSession(session)
        def session2 = deserializeSession(serialized)
        then:
        equals(session, session2)
        and:
        serialized.length() < maxSessionSize()
    }
}
