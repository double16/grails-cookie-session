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

import org.springframework.context.ApplicationContext
import spock.lang.Specification

import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class CookieSessionFilterTest extends Specification {
    SessionRepository sessionRepository

    void setup() {
        sessionRepository = Mock(SessionRepository)
    }

    void "enforceSession set if securityContextSessionPersistenceListener present"() {
        given:
        ApplicationContext applicationContext = Stub(ApplicationContext)
        applicationContext.containsBeanDefinition('securityContextSessionPersistenceListener') >> true
        applicationContext.getBeansOfType(SessionPersistenceListener) >> [:]
        CookieSessionFilter filter = new CookieSessionFilter(applicationContext: applicationContext, sessionRepository: sessionRepository)

        when:
        filter.afterPropertiesSet()

        then:
        filter.@enforceSession
    }

    void "enforceSession not set if securityContextSessionPersistenceListener missing"() {
        given:
        ApplicationContext applicationContext = Stub(ApplicationContext)
        applicationContext.containsBeanDefinition('securityContextSessionPersistenceListener') >> false
        applicationContext.getBeansOfType(SessionPersistenceListener) >> [:]
        CookieSessionFilter filter = new CookieSessionFilter(applicationContext: applicationContext, sessionRepository: sessionRepository)

        when:
        filter.afterPropertiesSet()

        then:
        !filter.@enforceSession
    }

    void "doFilterInternal"() {
        given:
        ApplicationContext applicationContext = Stub(ApplicationContext)
        applicationContext.containsBeanDefinition('securityContextSessionPersistenceListener') >> false
        SessionPersistenceListener l1 = Mock(SessionPersistenceListener)
        SessionPersistenceListener l2 = Mock(SessionPersistenceListener)
        applicationContext.getBeansOfType(SessionPersistenceListener) >> [l1: l1, l2: l2]
        CookieSessionFilter filter = new CookieSessionFilter(applicationContext: applicationContext, sessionRepository: sessionRepository)
        filter.afterPropertiesSet()
        filter.initFilterBean()
        HttpServletRequest request = Mock(HttpServletRequest)
        HttpServletResponse response = Mock(HttpServletResponse)
        FilterChain chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(request, response, chain)

        then:
        1 * chain.doFilter(_, _)
    }
}
