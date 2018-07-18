/*
 * Copyright 2012-2018 the original author or authors.
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

package grails.plugin.cookiesession

import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.ServletContext
import javax.servlet.http.HttpSession

@Unroll
class SessionRepositoryRequestWrapperTest extends Specification {
    void "restoreSession handles null"() {
        given:
        SessionRepository repo = Stub(SessionRepository)
        SessionRepositoryRequestWrapper wrapper = new SessionRepositoryRequestWrapper(
                new MockHttpServletRequest(),
                repo)
        wrapper.servletContext = Mock(ServletContext)
        repo.restoreSession(wrapper) >> null

        when:
        wrapper.restoreSession()

        then:
        wrapper.@session == null
    }

    void "restoreSession with valid session"() {
        given:
        SerializableSession session = new SerializableSession().defaultSerializer()
        SessionRepository repo = Stub(SessionRepository)
        SessionRepositoryRequestWrapper wrapper = new SessionRepositoryRequestWrapper(
                new MockHttpServletRequest(),
                repo)
        wrapper.servletContext = Mock(ServletContext)

        SessionPersistenceListener l1 = Mock(SessionPersistenceListener)
        SessionPersistenceListener l2 = Mock(SessionPersistenceListener)
        wrapper.sessionPersistenceListeners = [l1, l2]

        repo.restoreSession(wrapper) >> session

        when:
        wrapper.restoreSession()

        then:
        wrapper.@session.is(session)
        !session.isNew()
        !session.isDirty()
        1 * l1.afterSessionRestored(session)
        1 * l2.afterSessionRestored(session)
    }


    void "getSession(create) returns restored session"() {
        given:
        SerializableSession session = new SerializableSession().defaultSerializer()
        SessionRepository repo = Stub(SessionRepository)
        SessionRepositoryRequestWrapper wrapper = new SessionRepositoryRequestWrapper(
                new MockHttpServletRequest(),
                repo)
        wrapper.servletContext = Mock(ServletContext)
        repo.restoreSession(wrapper) >> session
        wrapper.restoreSession()

        when:
        HttpSession returnedSession = wrapper.getSession(true)

        then:
        returnedSession.is(session)
        !returnedSession.isNew()
    }

    void "getSession(create) returns null for no session and no create"() {
        given:
        SessionRepository repo = Stub(SessionRepository)
        SessionRepositoryRequestWrapper wrapper = new SessionRepositoryRequestWrapper(
                new MockHttpServletRequest(),
                repo)
        wrapper.servletContext = Mock(ServletContext)
        repo.restoreSession(wrapper) >> null
        wrapper.restoreSession()

        when:
        HttpSession returnedSession = wrapper.getSession(false)

        then:
        returnedSession == null
    }

    void "getSession(create) creates new session"() {
        given:
        SessionRepository repo = Stub(SessionRepository)
        SessionRepositoryRequestWrapper wrapper = new SessionRepositoryRequestWrapper(
                new MockHttpServletRequest(),
                repo)
        wrapper.servletContext = Mock(ServletContext)
        repo.restoreSession(wrapper) >> null
        wrapper.restoreSession()

        when:
        HttpSession returnedSession = wrapper.getSession(true)

        then:
        returnedSession.isNew()
        returnedSession.servletContext.is(wrapper.servletContext)
    }

    void "getSession returns restored session"() {
        given:
        SerializableSession session = new SerializableSession().defaultSerializer()
        session.isNewSession = false
        SessionRepository repo = Stub(SessionRepository)
        SessionRepositoryRequestWrapper wrapper = new SessionRepositoryRequestWrapper(
                new MockHttpServletRequest(),
                repo)
        wrapper.servletContext = Mock(ServletContext)
        repo.restoreSession(wrapper) >> session
        wrapper.restoreSession()

        when:
        HttpSession returnedSession = wrapper.getSession()

        then:
        returnedSession.is(session)
        !returnedSession.isNew()
    }

    void "getSession creates new session"() {
        given:
        ServletContext servletContext = Mock(ServletContext)
        SessionRepository repo = Stub(SessionRepository)
        SessionRepositoryRequestWrapper wrapper = new SessionRepositoryRequestWrapper(
                new MockHttpServletRequest(),
                repo)
        wrapper.servletContext = servletContext
        repo.restoreSession(wrapper) >> null

        when:
        HttpSession returnedSession = wrapper.getSession()

        then:
        returnedSession != null
        returnedSession.isNew()
        !returnedSession.attributeNames.hasMoreElements()
    }

    void "isRequestedSessionIdValid(#value) delegates to session repository"() {
        given:
        SessionRepository repo = Stub(SessionRepository)
        SessionRepositoryRequestWrapper wrapper = new SessionRepositoryRequestWrapper(
                new MockHttpServletRequest(requestedSessionId: 'simplesession'),
                repo)
        wrapper.servletContext = Mock(ServletContext)
        repo.isSessionIdValid(_) >> value

        expect:
        wrapper.isRequestedSessionIdValid() == value

        where:
        value | _
        false | _
        true  | _
    }
}
