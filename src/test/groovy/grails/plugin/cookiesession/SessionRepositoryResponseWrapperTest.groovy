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
package grails.plugin.cookiesession

import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Unroll
class SessionRepositoryResponseWrapperTest extends Specification {
    HttpServletResponse response
    HttpServletRequest request
    SessionRepository sessionRepository
    SessionPersistenceListener l1
    SessionPersistenceListener l2
    SessionRepositoryResponseWrapper wrapper

    void configure(boolean enforceSession = false) {
        response = Mock(HttpServletResponse)
        request = Mock(HttpServletRequest)
        sessionRepository = Mock(SessionRepository)

        wrapper = new SessionRepositoryResponseWrapper(response, sessionRepository, request, enforceSession)
        l1 = Mock(SessionPersistenceListener)
        l2 = Mock(SessionPersistenceListener)
        wrapper.sessionPersistenceListeners = [l1, l2]
    }

    void "saveSession notifies listeners before saving"() {
        given:
        configure()
        SerializableSession session = new SerializableSession()
        session.setIsNewSession(true)
        request.getSession(false) >> session

        when:
        wrapper.saveSession()

        then:
        1 * l1.beforeSessionSaved(session)
        1 * l2.beforeSessionSaved(session)
        1 * sessionRepository.saveSession(session, wrapper)
    }

    void "method setStatus(int) saves the session"() {
        given:
        configure()
        SerializableSession session = new SerializableSession()
        session.setIsNewSession(true)
        request.getSession(false) >> session

        when:
        wrapper.setStatus(200)

        then:
        1 * l1.beforeSessionSaved(session)
        1 * l2.beforeSessionSaved(session)
        1 * sessionRepository.saveSession(session, wrapper)
        1 * response.setStatus(200)
    }

    @SuppressWarnings('Deprecated')
    void "method setStatus(int,String) saves the session"() {
        given:
        configure()
        SerializableSession session = new SerializableSession()
        session.setIsNewSession(true)
        request.getSession(false) >> session

        when:
        wrapper.setStatus(201, 'message')

        then:
        1 * l1.beforeSessionSaved(session)
        1 * l2.beforeSessionSaved(session)
        1 * sessionRepository.saveSession(session, wrapper)
        1 * response.setStatus(201, 'message')
    }

    void "method flushBuffer() saves the session"() {
        given:
        configure()
        SerializableSession session = new SerializableSession()
        session.setIsNewSession(true)
        request.getSession(false) >> session

        when:
        wrapper.flushBuffer()

        then:
        1 * l1.beforeSessionSaved(session)
        1 * l2.beforeSessionSaved(session)
        1 * sessionRepository.saveSession(session, wrapper)
        1 * response.flushBuffer()
    }

    void "method getWriter() saves the session"() {
        given:
        configure()
        SerializableSession session = new SerializableSession()
        session.setIsNewSession(true)
        request.getSession(false) >> session

        when:
        wrapper.getWriter()

        then:
        1 * l1.beforeSessionSaved(session)
        1 * l2.beforeSessionSaved(session)
        1 * sessionRepository.saveSession(session, wrapper)
        1 * response.getWriter()
    }

    void "method getOutputStream() saves the session"() {
        given:
        configure()
        SerializableSession session = new SerializableSession()
        session.setIsNewSession(true)
        request.getSession(false) >> session

        when:
        wrapper.getOutputStream()

        then:
        1 * l1.beforeSessionSaved(session)
        1 * l2.beforeSessionSaved(session)
        1 * sessionRepository.saveSession(session, wrapper)
        1 * response.getOutputStream()
    }

    void "method sendRedirect(String) saves the session"() {
        given:
        configure()
        SerializableSession session = new SerializableSession()
        session.setIsNewSession(true)
        request.getSession(false) >> session

        when:
        wrapper.sendRedirect('/')

        then:
        1 * l1.beforeSessionSaved(session)
        1 * l2.beforeSessionSaved(session)
        1 * sessionRepository.saveSession(session, wrapper)
        1 * response.sendRedirect('/')
    }

    void "method sendError(int) saves the session"() {
        given:
        configure()
        SerializableSession session = new SerializableSession()
        session.setIsNewSession(true)
        request.getSession(false) >> session

        when:
        wrapper.sendError(500)

        then:
        1 * l1.beforeSessionSaved(session)
        1 * l2.beforeSessionSaved(session)
        1 * sessionRepository.saveSession(session, wrapper)
        1 * response.sendError(500)
    }

    void "method sendError(int,String) saves the session"() {
        given:
        configure()
        SerializableSession session = new SerializableSession()
        session.setIsNewSession(true)
        request.getSession(false) >> session

        when:
        wrapper.sendError(500, 'interval error')

        then:
        1 * l1.beforeSessionSaved(session)
        1 * l2.beforeSessionSaved(session)
        1 * sessionRepository.saveSession(session, wrapper)
        1 * response.sendError(500, 'interval error')
    }

    void "session is not saved if response is committed"() {
        given:
        configure()

        when:
        wrapper.saveSession()

        then:
        1 * response.isCommitted() >> true
        and:
        0 * request.getSession(false)
        0 * l1.beforeSessionSaved(_)
        0 * l2.beforeSessionSaved(_)
        0 * sessionRepository.saveSession(_)
    }

    void "session will only be saved once"() {
        given:
        configure()
        SerializableSession session = new SerializableSession()
        session.setIsNewSession(true)

        when:
        wrapper.saveSession()
        wrapper.saveSession()
        wrapper.saveSession()

        then:
        1 * request.getSession(false) >> session
        1 * l1.beforeSessionSaved(session)
        1 * l2.beforeSessionSaved(session)
        1 * sessionRepository.saveSession(session, wrapper)
    }

    void "session is not saved if it does not exist"() {
        given:
        configure()

        when:
        wrapper.saveSession()

        then:
        1 * request.getSession(false) >> null
        0 * l1.beforeSessionSaved()
        0 * l2.beforeSessionSaved(_)
        0 * sessionRepository.saveSession(_, wrapper)
    }

    void "session is not saved if it is not dirty"() {
        given:
        configure()
        SerializableSession session = new SerializableSession()
        session.setIsNewSession(false)
        session.dirty = false
        request.getSession(false) >> session

        when:
        wrapper.saveSession()

        then:
        0 * l1.beforeSessionSaved()
        0 * l2.beforeSessionSaved(_)
        0 * sessionRepository.saveSession(_, wrapper)
    }

    void "session is saved if it is new"() {
        given:
        configure()
        SerializableSession session = new SerializableSession()
        session.setIsNewSession(true)
        session.dirty = false
        request.getSession(false) >> session

        when:
        wrapper.saveSession()

        then:
        1 * l1.beforeSessionSaved(session)
        1 * l2.beforeSessionSaved(session)
        1 * sessionRepository.saveSession(session, wrapper)
    }

    void "enforce session"() {
        given:
        configure(true)
        SerializableSession session = new SerializableSession()
        session.setIsNewSession(true)
        session.dirty = false
        request.getSession(true) >> session

        when:
        wrapper.saveSession()

        then:
        1 * l1.beforeSessionSaved(session)
        1 * l2.beforeSessionSaved(session)
        1 * sessionRepository.saveSession(session, wrapper)
    }
}
