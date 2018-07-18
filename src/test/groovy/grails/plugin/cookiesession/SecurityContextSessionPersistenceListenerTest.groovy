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

import grails.config.Config
import grails.core.GrailsApplication
import org.grails.config.PropertySourcesConfig
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.PortResolver
import org.springframework.security.web.savedrequest.DefaultSavedRequest
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.Cookie

@Unroll
class SecurityContextSessionPersistenceListenerTest extends Specification {
    static final String SPRING__SECURITY__CONTEXT = 'SPRING_SECURITY_CONTEXT'
    static final List<String> SPRING_SECURITY_SAVED_REQUEST = [
            'SPRING_SECURITY_SAVED_REQUEST_KEY', // needed for backwards compatibility
            'SPRING_SECURITY_SAVED_REQUEST',
    ]

    SecurityContextSessionPersistenceListener listener

    void setup() {
        SecurityContext securityContext = new SecurityContextImpl()
        SecurityContextHolder.setContext(securityContext)
        listener = new SecurityContextSessionPersistenceListener()
        listener.securityContextHolder = SecurityContextHolder
    }

    void "afterPropertiesSet preserves default cookie name"() {
        given:
        Config config = new PropertySourcesConfig().merge(new ConfigSlurper('test').parse("""
grails.plugin.cookiesession.enabled = true
"""))
        listener.grailsApplication = Stub(GrailsApplication)
        listener.grailsApplication.config >> config
        listener.grailsApplication.classLoader >> getClass().classLoader

        when:
        listener.afterPropertiesSet()

        then:
        !listener.cookiePattern.matcher('xsession-0').find()
        !listener.cookiePattern.matcher('xsession-1').find()
        and:
        listener.cookiePattern.matcher('gsession-0').find()
        listener.cookiePattern.matcher('gsession-1').find()
    }

    void "afterPropertiesSet accepts custom cookie name"() {
        given:
        Config config = new PropertySourcesConfig().merge(new ConfigSlurper('test').parse("""
grails.plugin.cookiesession.enabled = true
grails.plugin.cookiesession.cookiename = 'xsession'
"""))
        listener.grailsApplication = Stub(GrailsApplication)
        listener.grailsApplication.config >> config
        listener.grailsApplication.classLoader >> getClass().classLoader

        when:
        listener.afterPropertiesSet()

        then:
        listener.cookiePattern.matcher('xsession-0').find()
        listener.cookiePattern.matcher('xsession-1').find()
        and:
        !listener.cookiePattern.matcher('gsession-0').find()
        !listener.cookiePattern.matcher('gsession-1').find()
    }

    void "afterSessionRestored does not fail"() {
        when:
        listener.afterSessionRestored(new SerializableSession().defaultSerializer())
        then:
        notThrown(Exception)
    }

    void "SavedRequest cookies are removed from #key"() {
        given:
        MockHttpServletRequest request = new MockHttpServletRequest()
        request.setCookies(
                new Cookie('gsession-0', 'abc'),
                new Cookie('gsession-1', 'def'),
                new Cookie('JSESSIONID', 'jsessionid-abcdef'),
                new Cookie('state', 'nebraska'),
        )
        request.addHeader('cookie', 'gsession-0=abc; gsession-1=def; state=nebraska')
        request.addHeader('accept', '*/*')
        DefaultSavedRequest sr = new DefaultSavedRequest(request, Mock(PortResolver))
        SerializableSession session = new SerializableSession().defaultSerializer()
        session.setAttribute(key, sr)
        session.dirty = false

        when:
        listener.beforeSessionSaved(session)

        then:
        sr.getCookies()*.name.sort() == ['JSESSIONID', 'state']
        sr.headerNames.sort() == ['accept'] as Set
        session.dirty

        where:
        key << SPRING_SECURITY_SAVED_REQUEST
    }

    void "Session is clean if no SavedRequest cookies are removed from #key"() {
        given:
        MockHttpServletRequest request = new MockHttpServletRequest()
        request.setCookies(
                new Cookie('JSESSIONID', 'jsessionid-abcdef'),
                new Cookie('state', 'nebraska'),
        )
        request.addHeader('accept', '*/*')
        DefaultSavedRequest sr = new DefaultSavedRequest(request, Mock(PortResolver))
        SerializableSession session = new SerializableSession().defaultSerializer()
        session.setAttribute(key, sr)
        session.dirty = false

        when:
        listener.beforeSessionSaved(session)

        then:
        sr.getCookies()*.name.sort() == ['JSESSIONID', 'state']
        sr.headerNames.sort() == ['accept'] as Set
        !session.dirty

        where:
        key << SPRING_SECURITY_SAVED_REQUEST
    }

    void "SPRING_SECURITY_CONTEXT is skipped if not present"() {
        given:
        SerializableSession session = new SerializableSession().defaultSerializer()
        session.dirty = false

        when:
        listener.beforeSessionSaved(session)
        !session.dirty

        then:
        session.getAttribute(SPRING__SECURITY__CONTEXT) == null
    }

    void "SPRING_SECURITY_CONTEXT is replaced if different"() {
        given:
        SerializableSession session = new SerializableSession().defaultSerializer()
        session.setAttribute(SPRING__SECURITY__CONTEXT, new SecurityContextImpl())
        session.dirty = false

        when:
        listener.beforeSessionSaved(session)
        session.dirty

        then:
        session.getAttribute(SPRING__SECURITY__CONTEXT) == SecurityContextHolder.getContext()
    }

    void "SPRING_SECURITY_CONTEXT is not replaced if already the correct instance"() {
        given:
        SerializableSession session = new SerializableSession().defaultSerializer()
        session.setAttribute(SPRING__SECURITY__CONTEXT, SecurityContextHolder.getContext())
        session.dirty = false

        when:
        listener.beforeSessionSaved(session)
        !session.dirty

        then:
        session.getAttribute(SPRING__SECURITY__CONTEXT) == SecurityContextHolder.getContext()
    }
}
