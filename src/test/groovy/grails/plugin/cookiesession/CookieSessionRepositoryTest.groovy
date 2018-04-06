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

import ch.qos.logback.classic.Level
import grails.config.Config
import grails.core.GrailsApplication
import groovyx.gpars.GParsPool
import org.grails.config.PropertySourcesConfig
import org.springframework.context.ApplicationContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockSessionCookieConfig
import spock.lang.Specification
import spock.lang.Unroll

import javax.crypto.spec.SecretKeySpec
import javax.servlet.ServletContext
import java.util.concurrent.Future

@Unroll
class CookieSessionRepositoryTest extends Specification {
    CookieSessionRepository sessionRepository
    MockHttpServletRequest request
    MockHttpServletResponse response
    SerializableSession session

    def setup() {
        request = new MockHttpServletRequest()
        response = new MockHttpServletResponse()
        session = new SerializableSession()

        ServletContext servletContext = Mock(ServletContext)
        servletContext.majorVersion >> 3
        servletContext.minorVersion >> 0
        servletContext.sessionCookieConfig >> new MockSessionCookieConfig()

        GrailsApplication grailsApplication = Stub(GrailsApplication)
        grailsApplication.getClassLoader() >> getClass().classLoader
        ApplicationContext applicationContext = Stub(ApplicationContext)
        sessionRepository = new CookieSessionRepository(applicationContext: applicationContext)
        sessionRepository.serializer = CookieSessionRepository.JAVA_SESSION_SERIALIZER_BEAN_NAME
        sessionRepository.encryptCookie = false
        sessionRepository.secure = false
        sessionRepository.httpOnly = true
        sessionRepository.servletContext = servletContext
        sessionRepository.grailsApplication = grailsApplication
        applicationContext.getBean(CookieSessionRepository.JAVA_SESSION_SERIALIZER_BEAN_NAME) >> (new JavaSessionSerializer(grailsApplication: grailsApplication))
        applicationContext.getBean(CookieSessionRepository.KRYO_SESSION_SERIALIZER_BEAN_NAME) >> (new KryoSessionSerializer(grailsApplication: grailsApplication, springSecurityCompatibility: true, springSecurityPluginVersion: '3.0.0'))
        applicationContext.getBean(CookieSessionRepository.SERVLET_CONTEXT_BEAN) >> servletContext
        applicationContext.containsBean(CookieSessionRepository.SERVLET_CONTEXT_BEAN) >> true

        applicationContext.getBean('customSessionSerializer') >> (new JavaSessionSerializer(grailsApplication: grailsApplication))
        applicationContext.getType('customSessionSerializer') >> JavaSessionSerializer
        applicationContext.containsBean('customSessionSerializer') >> true

        sessionRepository.log.level = Level.TRACE
    }

    void "session repository can save data to a response with java serializer"() {
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
        thrown(MaxSessionSizeException)
    }

    void "session repository can encrypt data stored in the session using #crypto and secret #secret"() {
        given:
        Config config = new PropertySourcesConfig().merge(new ConfigSlurper('test').parse("""
grails.plugin.cookiesession.enabled = true
grails.plugin.cookiesession.cookiename = 'testcookie'
grails.plugin.cookiesession.cookiecount = 5
grails.plugin.cookiesession.maxcookiesize = 4096
grails.plugin.cookiesession.encryptcookie = true
grails.plugin.cookiesession.cryptoalgorithm = '${crypto}'
grails.plugin.cookiesession.secret = ${secret}
"""))
        sessionRepository.grailsApplication.config >> config
        sessionRepository.afterPropertiesSet()

        when: 'the session repository encrypts a session'
        session.setAttribute('k1', 'v1')
        session.setAttribute('k2', 'v2')
        session.setAttribute('k3', 'v3')
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

        where:
        crypto                                           | secret
        CookieSessionRepository.DEFAULT_CRYPTO_ALGORITHM | '"testsecret"'
        CookieSessionRepository.DEFAULT_CRYPTO_ALGORITHM | '[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15] as byte[]'
        CookieSessionRepository.DEFAULT_CRYPTO_ALGORITHM | '[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]'
        CookieSessionRepository.DEFAULT_CRYPTO_ALGORITHM | 'null'
        'Blowfish/ECB/PKCS5Padding'                      | '"testsecret"'
        'Blowfish/ECB/PKCS5Padding'                      | '[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15] as byte[]'
        'Blowfish/ECB/PKCS5Padding'                      | '[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]'
        'Blowfish/ECB/PKCS5Padding'                      | 'null'
        'Blowfish/CBC/PKCS5Padding'                      | '"testsecret"'
        'Blowfish/CBC/PKCS5Padding'                      | '[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15] as byte[]'
        'Blowfish/CBC/PKCS5Padding'                      | '[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]'
        'Blowfish/CBC/PKCS5Padding'                      | 'null'
        'AES/CBC/PKCS5Padding'                           | '"testsecret123456"'
        'AES/CBC/PKCS5Padding'                           | '[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15] as byte[]'
        'AES/CBC/PKCS5Padding'                           | '[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]'
        'AES/CBC/PKCS5Padding'                           | 'null'
        'AES/GCM/PKCS5Padding'                           | '"testsecret123456"'
        'AES/GCM/PKCS5Padding'                           | '[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15] as byte[]'
        'AES/GCM/PKCS5Padding'                           | '[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]'
        'AES/GCM/PKCS5Padding'                           | 'null'
    }

    void "session repository can store an unmodifiable collection"() {
        when: 'an unmodifiable collection is written to the session'
        List<String> strings = ['string 1', 'string 2', 'string 3', 'string 4']
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

    void "restoreSession handles expired session"() {
        when:
        sessionRepository.maxInactiveInterval = 10
        sessionRepository.cookieName = 'testcookie'
        session.lastAccessedTime = 0
        sessionRepository.saveSession(session, response)
        request.cookies = response.cookies
        SerializableSession restored = sessionRepository.restoreSession(request)

        then:
        restored == null
    }

    void "cookie properties can be set"() {
        given:
        Config config = new PropertySourcesConfig().merge(new ConfigSlurper('test').parse("""
grails.plugin.cookiesession.enabled = true
grails.plugin.cookiesession.cookiename = 'testcookie'
grails.plugin.cookiesession.setsecure = ${setsecure}
grails.plugin.cookiesession.httponly = ${httponly}
grails.plugin.cookiesession.path = '${path}'
grails.plugin.cookiesession.domain = '${domain}'
grails.plugin.cookiesession.comment = '${comment}'
"""))
        sessionRepository.grailsApplication.config >> config
        sessionRepository.afterPropertiesSet()

        when:
        session.setAttribute('k1', 'v1')
        session.setAttribute('k2', 'v2')
        session.setAttribute('k3', 'v3')
        sessionRepository.saveSession(session, response)

        then:
        response.cookies.size() > 1
        response.cookies*.secure.unique() == [setsecure]
        response.cookies*.httpOnly.unique() == [httponly]
        response.cookies*.path.unique() == [path]
        response.cookies*.domain.unique() == [domain]
        response.cookies*.comment.unique() == [comment]

        where:
        setsecure | httponly | path   | domain        | comment
        false     | false    | '/'    | 'nowhere.com' | 'session cookie'
        true      | true     | '/app' | 'nowhere.com' | 'session cookie'
    }

    void "cookie httponly settings is ignored for old servlet context"() {
        given:
        Config config = new PropertySourcesConfig().merge(new ConfigSlurper('test').parse("""
grails.plugin.cookiesession.enabled = true
grails.plugin.cookiesession.cookiename = 'testcookie'
grails.plugin.cookiesession.httponly = true
"""))
        sessionRepository.grailsApplication.config >> config
        sessionRepository.afterPropertiesSet()

        ServletContext servletContext = Mock(ServletContext)
        servletContext.majorVersion >> 2
        servletContext.minorVersion >> 0
        sessionRepository.servletContext = servletContext

        when:
        session.setAttribute('k1', 'v1')
        session.setAttribute('k2', 'v2')
        session.setAttribute('k3', 'v3')
        sessionRepository.saveSession(session, response)

        then:
        response.cookies.size() > 0
        response.cookies*.httpOnly.unique() == [false]
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

    void "invalidate cookie is deleted"() {
        when:
        session.setAttribute('k1', 'v1')
        session.setAttribute('k2', 'v2')
        session.setAttribute('k3', 'v3')
        session.invalidate()
        sessionRepository.cookieCount = 5
        sessionRepository.maxCookieSize = 4096
        sessionRepository.cookieName = 'testcookie'
        sessionRepository.saveSession(session, response)

        then:
        response.cookies.size() == 6
        response.cookies[0].name == 'testcookie-0'
        response.cookies[0].value == ''
        response.cookies[0].maxAge == 0
        response.cookies[1].name == 'testcookie-1'
        response.cookies[1].value == ''
        response.cookies[1].maxAge == 0
        response.cookies[2].name == 'testcookie-2'
        response.cookies[2].value == ''
        response.cookies[2].maxAge == 0
        response.cookies[3].name == 'testcookie-3'
        response.cookies[3].value == ''
        response.cookies[3].maxAge == 0
        response.cookies[4].name == 'testcookie-4'
        response.cookies[4].value == ''
        response.cookies[4].maxAge == 0
        response.cookies[5].name == 'testcookie-5'
        response.cookies[5].value == ''
        response.cookies[5].maxAge == 0
        response.cookies.each {
            println "${it.name} : ${it.value}"
        }

        when:
        request.cookies = response.cookies
        session = sessionRepository.restoreSession(request)

        then:
        session == null
    }

    void "isSessionIdValid should always return true"() {
        expect:
        sessionRepository.isSessionIdValid(null)
        sessionRepository.isSessionIdValid('')
        sessionRepository.isSessionIdValid('simplesession')
    }

    void "handles no cookies"() {
        when:
        request.cookies = null
        session = sessionRepository.restoreSession(request)

        then:
        session == null
    }

    void "serialize using kryo"() {
        given:
        Config config = new PropertySourcesConfig().merge(new ConfigSlurper('test').parse("""
grails.plugin.cookiesession.enabled = true
grails.plugin.cookiesession.serializer = 'kryo'
"""))
        sessionRepository.grailsApplication.config >> config
        sessionRepository.afterPropertiesSet()

        when:
        session.setAttribute('k1', 'v1')
        session.setAttribute('k2', 'v2')
        session.setAttribute('k3', 'v3')
        sessionRepository.cookieCount = 5
        sessionRepository.maxCookieSize = 4096
        sessionRepository.cookieName = 'testcookie'
        sessionRepository.saveSession(session, response)

        then:
        sessionRepository.serializer == CookieSessionRepository.KRYO_SESSION_SERIALIZER_BEAN_NAME
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

    void "serialize using custom serializer"() {
        given:
        Config config = new PropertySourcesConfig().merge(new ConfigSlurper('test').parse("""
grails.plugin.cookiesession.enabled = true
grails.plugin.cookiesession.serializer = 'customSessionSerializer'
"""))
        sessionRepository.grailsApplication.config >> config
        sessionRepository.afterPropertiesSet()

        when:
        session.setAttribute('k1', 'v1')
        session.setAttribute('k2', 'v2')
        session.setAttribute('k3', 'v3')
        sessionRepository.cookieCount = 5
        sessionRepository.maxCookieSize = 4096
        sessionRepository.cookieName = 'testcookie'
        sessionRepository.saveSession(session, response)

        then:
        sessionRepository.serializer == 'customSessionSerializer'
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

    void "serialize using missing custom serializer"() {
        given:
        Config config = new PropertySourcesConfig().merge(new ConfigSlurper('test').parse("""
grails.plugin.cookiesession.enabled = true
grails.plugin.cookiesession.serializer = 'unknownSessionSerializer'
"""))
        sessionRepository.grailsApplication.config >> config
        sessionRepository.afterPropertiesSet()

        when:
        session.setAttribute('k1', 'v1')
        session.setAttribute('k2', 'v2')
        session.setAttribute('k3', 'v3')
        sessionRepository.cookieCount = 5
        sessionRepository.maxCookieSize = 4096
        sessionRepository.cookieName = 'testcookie'
        sessionRepository.saveSession(session, response)

        then:
        sessionRepository.serializer == CookieSessionRepository.JAVA_SESSION_SERIALIZER_BEAN_NAME
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

    void "maxCookieSize sanity check does not allow #maxCookieSize"() {
        given:
        Config config = new PropertySourcesConfig().merge(new ConfigSlurper('test').parse("""
grails.plugin.cookiesession.enabled = true
grails.plugin.cookiesession.maxcookiesize = ${maxCookieSize}
"""))
        sessionRepository.grailsApplication.config >> config

        when:
        sessionRepository.afterPropertiesSet()

        then:
        sessionRepository.maxCookieSize == expectedMaxCookieSize

        where:
        maxCookieSize | expectedMaxCookieSize
        100           | 2048
        1023          | 2048
        1024          | 1024
        4096          | 4096
        4097          | 2048
    }

    void "useSessionCookieConfig is ignored for old servlet context"() {
        given:
        Config config = new PropertySourcesConfig().merge(new ConfigSlurper('test').parse("""
grails.plugin.cookiesession.enabled = true
grails.plugin.cookiesession.cookiename = 'testcookie'
grails.plugin.cookiesession.useSessionCookieConfig = true
"""))
        sessionRepository.grailsApplication.config >> config
        ServletContext servletContext = Mock(ServletContext)
        servletContext.majorVersion >> 2
        servletContext.minorVersion >> 0

        ApplicationContext applicationContext = Stub(ApplicationContext)
        applicationContext.getBean(CookieSessionRepository.JAVA_SESSION_SERIALIZER_BEAN_NAME) >> (new JavaSessionSerializer(grailsApplication: sessionRepository.grailsApplication))
        applicationContext.getBean(CookieSessionRepository.SERVLET_CONTEXT_BEAN) >> servletContext
        applicationContext.containsBean(CookieSessionRepository.SERVLET_CONTEXT_BEAN) >> true
        sessionRepository.applicationContext = applicationContext

        when:
        sessionRepository.afterPropertiesSet()

        then:
        !sessionRepository.useSessionCookieConfig
    }

    void "useSessionCookieConfig using setters"() {
        given:
        Config config = new PropertySourcesConfig().merge(new ConfigSlurper('test').parse("""
grails.plugin.cookiesession.enabled = true
grails.plugin.cookiesession.useSessionCookieConfig = true
"""))
        sessionRepository.grailsApplication.config >> config

        when:
        sessionRepository.afterPropertiesSet()
        sessionRepository.servletContext.sessionCookieConfig.setName('testcookie')
        sessionRepository.servletContext.sessionCookieConfig.setDomain('nowhere.com')

        then:
        sessionRepository.useSessionCookieConfig
        sessionRepository.cookieName == 'testcookie'
        sessionRepository.domain == 'nowhere.com'

        when:
        sessionRepository.servletContext.sessionCookieConfig.setName('testcookie2')
        sessionRepository.servletContext.sessionCookieConfig.setDomain('nowhere.edu')
        sessionRepository.servletContext.sessionCookieConfig.setHttpOnly(true)
        sessionRepository.servletContext.sessionCookieConfig.setSecure(true)
        sessionRepository.servletContext.sessionCookieConfig.setPath('/app')
        sessionRepository.servletContext.sessionCookieConfig.setComment('user session')
        sessionRepository.servletContext.sessionCookieConfig.setMaxAge(20)

        then:
        sessionRepository.cookieName == 'testcookie2'
        sessionRepository.domain == 'nowhere.edu'
        sessionRepository.httpOnly
        sessionRepository.secure
        sessionRepository.path == '/app'
        sessionRepository.comment == 'user session'
        sessionRepository.maxInactiveInterval == 20
    }

    void "useSessionCookieConfig using properties"() {
        given:
        Config config = new PropertySourcesConfig().merge(new ConfigSlurper('test').parse("""
grails.plugin.cookiesession.enabled = true
grails.plugin.cookiesession.useSessionCookieConfig = true
"""))
        sessionRepository.grailsApplication.config >> config

        when:
        sessionRepository.afterPropertiesSet()
        sessionRepository.servletContext.sessionCookieConfig.name = 'testcookie'
        sessionRepository.servletContext.sessionCookieConfig.domain = 'nowhere.com'

        then:
        sessionRepository.useSessionCookieConfig
        sessionRepository.cookieName == 'testcookie'
        sessionRepository.domain == 'nowhere.com'

        when:
        sessionRepository.servletContext.sessionCookieConfig.name = 'testcookie2'
        sessionRepository.servletContext.sessionCookieConfig.domain = 'nowhere.edu'
        sessionRepository.servletContext.sessionCookieConfig.httpOnly = true
        sessionRepository.servletContext.sessionCookieConfig.secure = true
        sessionRepository.servletContext.sessionCookieConfig.path = '/app'
        sessionRepository.servletContext.sessionCookieConfig.comment = 'user session'
        sessionRepository.servletContext.sessionCookieConfig.maxAge = 20

        then:
        sessionRepository.cookieName == 'testcookie2'
        sessionRepository.domain == 'nowhere.edu'
        sessionRepository.httpOnly
        sessionRepository.secure
        sessionRepository.path == '/app'
        sessionRepository.comment == 'user session'
        sessionRepository.maxInactiveInterval == 20
    }

    void "thread safe #serializer"() {
        given:
        sessionRepository.log.level = Level.WARN
        Config config = new PropertySourcesConfig().merge(new ConfigSlurper('test').parse("""
grails.plugin.cookiesession.enabled = true
grails.plugin.cookiesession.serializer = '${serializer}'
"""))
        sessionRepository.grailsApplication.config >> config
        sessionRepository.cookieCount = 5
        sessionRepository.maxCookieSize = 4096
        sessionRepository.cookieName = 'testcookie'
        sessionRepository.afterPropertiesSet()

        def sessionTest = {
            SerializableSession session = new SerializableSession()
            session.setAttribute('k1', '123'*100)
            session.setAttribute('k2', [a: 'first string', b: 'second string'])
            MockHttpServletResponse response = new MockHttpServletResponse()
            sessionRepository.saveSession(session, response)
            MockHttpServletRequest request = new MockHttpServletRequest()
            request.cookies = response.cookies
            SerializableSession session2 = sessionRepository.restoreSession(request)
            return session == session2
        }

        when:
        List<Future<Boolean>> futures = new ArrayList<>(1000)
        GParsPool.withPool {
            (1..200).each {
                futures.addAll(GParsPool.executeAsync(sessionTest, sessionTest, sessionTest, sessionTest, sessionTest))
            }
        }

        then:
        futures.size() == 1000
        futures*.get().unique() == [ true ]

        where:
        serializer | _
        'java'     | _
        'kryo'     | _
    }
}
