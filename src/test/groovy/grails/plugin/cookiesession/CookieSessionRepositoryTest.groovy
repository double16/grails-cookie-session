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
 *  Ben Lucchesi
 *  benlucchesi@gmail.com
 *  Patrick Double
 *  patrick.double@objectpartners.com or pat@patdouble.com
 */
package grails.plugin.cookiesession

import grails.core.GrailsApplication
import org.springframework.context.ApplicationContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification

import javax.crypto.spec.SecretKeySpec

class CookieSessionRepositoryTest extends Specification {
    CookieSessionRepository sessionRepository
    MockHttpServletRequest request
    MockHttpServletResponse response
    SerializableSession session

    def setup() {
        request = new MockHttpServletRequest()
        response = new MockHttpServletResponse()
        session = new SerializableSession()

        GrailsApplication grailsApplication = Stub(GrailsApplication)
        grailsApplication.getClassLoader() >> getClass().classLoader
        ApplicationContext applicationContext = Stub(ApplicationContext)
        sessionRepository = new CookieSessionRepository(applicationContext: applicationContext)
        sessionRepository.serializer = 'javaSerializer'
        sessionRepository.encryptCookie = false
        sessionRepository.secure = false
        sessionRepository.httpOnly = true
        applicationContext.getBean('javaSerializer') >> (new JavaSessionSerializer(grailsApplication: grailsApplication))
        applicationContext.getBean('kryoSerializer') >> (new KryoSessionSerializer(grailsApplication: grailsApplication))
    }

    void "session repository can save data to a response"() {
        when:
        session.setAttribute('k1', 'v1')
        session.setAttribute('k2', 'v2')
        session.setAttribute('k3', 'v3')
        sessionRepository.cookieCount = 5
        sessionRepository.maxCookieSize = 4096
        sessionRepository.cookieName = 'testcookie'
        sessionRepository.saveSession(session, response)

        then:
        response.cookies.size() == 5
        response.cookies[0].name == 'testcookie-0'
        response.cookies[1].name == 'testcookie-1'
        response.cookies[2].name == 'testcookie-2'
        response.cookies[3].name == 'testcookie-3'
        response.cookies[4].name == 'testcookie-4'
        response.cookies.each {
            println "${it.name} : ${it.value}"
        }

        when:
        request.cookies = response.cookies
        session = sessionRepository.restoreSession(request)

        then:
        session.k1 == 'v1'
        session.k2 == 'v2'
        session.k3 == 'v3'
    }

    void "session repository should produce cookies that live only as long as the browser is open"() {
        when:
        sessionRepository.cookieCount = 1
        sessionRepository.maxCookieSize = 4096
        sessionRepository.maxInactiveInterval = -1
        sessionRepository.cookieName = 'testcookie'
        sessionRepository.saveSession(session, response)

        then:
        response.cookies.size() == 1
        response.cookies[0].name == 'testcookie-0'
        response.cookies[0].maxAge == -1
    }

    void "session repository should produce cookies that expire in the session timeout period"() {
        when:
        sessionRepository.cookieCount = 1
        sessionRepository.maxCookieSize = 4096
        sessionRepository.maxInactiveInterval = 10
        sessionRepository.cookieName = 'testcookie'
        sessionRepository.saveSession(session, response)

        then:
        response.cookies.size() == 1
        response.cookies[0].name == 'testcookie-0'
        response.cookies[0].maxAge == 10
    }

    void "session exceeds max session size"() {
        when: 'when the session exceeds the the max storable session size'
        session.setAttribute('key', 'ABCDEFGHIJ')
        sessionRepository.cookieCount = 1
        sessionRepository.maxCookieSize = 10
        sessionRepository.cookieName = 'testcookie'
        sessionRepository.encryptCookie = false
        sessionRepository.saveSession(session, response)

        then:
        thrown(Exception)
    }

    void "session repository can encrypt data stored in the session"() {
        when: 'the session repository encrypts a session'
        session.setAttribute('k1', 'v1')
        session.setAttribute('k2', 'v2')
        session.setAttribute('k3', 'v3')
        sessionRepository.cookieCount = 5
        sessionRepository.maxCookieSize = 4096
        sessionRepository.cookieName = 'testcookie'
        sessionRepository.encryptCookie = true
        sessionRepository.cryptoKey = new SecretKeySpec('testsecret'.getBytes(), sessionRepository.cryptoAlgorithm.split('/')[0])
        sessionRepository.saveSession(session, response)

        then: 'the requisite number of cookie are created'
        response.cookies.size() == 5
        response.cookies[0].name == 'testcookie-0'
        response.cookies[1].name == 'testcookie-1'
        response.cookies[2].name == 'testcookie-2'
        response.cookies[3].name == 'testcookie-3'
        response.cookies[4].name == 'testcookie-4'
        response.cookies.each {
            println "${it.name} : ${it.value}"
        }

        when: 'the session is restored'
        request.cookies = response.cookies
        session = sessionRepository.restoreSession(request)
        then: 'the values are recoverable from the session'
        session.k1 == 'v1'
        session.k2 == 'v2'
        session.k3 == 'v3'
    }


    void "session repository can store an unmodifiable collection"() {
        when: 'an unmodifiable collection is written to the session'
        List<String> strings = new ArrayList<String>()
        strings.add('string 1')
        strings.add('string 2')
        strings.add('string 3')
        strings.add('string 4')
        session.setAttribute('unmodifiable', Collections.unmodifiableList(strings))
        sessionRepository.cookieCount = 5
        sessionRepository.maxCookieSize = 4096
        sessionRepository.cookieName = 'testcookie'
        sessionRepository.encryptCookie = true
        sessionRepository.cryptoKey = new SecretKeySpec('testsecret'.getBytes(), sessionRepository.cryptoAlgorithm.split('/')[0])
        sessionRepository.saveSession(session, response)

        then: 'the requested number of cookie are created'
        response.cookies.size() == 5
        response.cookies[0].name == 'testcookie-0'
        response.cookies[1].name == 'testcookie-1'
        response.cookies[2].name == 'testcookie-2'
        response.cookies[3].name == 'testcookie-3'
        response.cookies[4].name == 'testcookie-4'
        response.cookies.each {
            println "${it.name} : ${it.value}"
        }

        when: 'the session is restored'
        request.cookies = response.cookies
        session = sessionRepository.restoreSession(request)
        then: 'the values are recoverable from the session'
        session.unmodifiable.size() == 4
        session.unmodifiable[0] == 'string 1'
        session.unmodifiable[1] == 'string 2'
        session.unmodifiable[2] == 'string 3'
        session.unmodifiable[3] == 'string 4'
    }

    void testStringToStoreInSessionIsLessThanMaxCookieSize() {
        given:
        CookieSessionRepository cookieSessionRepository = new CookieSessionRepository()
        cookieSessionRepository.cookieCount = 4
        cookieSessionRepository.maxCookieSize = 4
        String input = '123'

        when:
        String[] output = cookieSessionRepository.splitString(input)

        then:
        output != null
        output.length == 4
        output[0] == '123'
        output[1] == null
        output[2] == null
        output[3] == null
    }

    void testStringToStoreInSessionIsDivisibleByMaxCookieSize() {
        given:
        CookieSessionRepository cookieSessionRepository = new CookieSessionRepository()
        cookieSessionRepository.cookieCount = 4
        cookieSessionRepository.maxCookieSize = 4
        String input = '12345678'

        when:
        String[] output = cookieSessionRepository.splitString(input)

        then:
        output != null
        output.length == 4
        output[0] == '1234'
        output[1] == '5678'
        output[2] == null
        output[3] == null
    }

    void testStringToStoreInSessionIsNotDivisibleByMaxCookieSize() {
        given:
        CookieSessionRepository cookieSessionRepository = new CookieSessionRepository()
        cookieSessionRepository.cookieCount = 4
        cookieSessionRepository.maxCookieSize = 4
        String input = '1234567'

        when:
        String[] output = cookieSessionRepository.splitString(input)

        then:
        output != null
        output.length == 4
        output[0] == '1234'
        output[1] == '567'
        output[2] == null
        output[3] == null
    }


    void testStringToStoreInSessionIsEmpty() {
        given:
        CookieSessionRepository cookieSessionRepository = new CookieSessionRepository()
        cookieSessionRepository.cookieCount = 4
        cookieSessionRepository.maxCookieSize = 4
        String input = ''

        when:
        String[] output = cookieSessionRepository.splitString(input)

        then:
        output != null
        output.length == 4
        output[0] == null
        output[1] == null
        output[2] == null
        output[3] == null
    }
}
