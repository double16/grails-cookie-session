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
 *  ben@granicus.com or benlucchesi@gmail.com
 *  Patrick Double
 *  patrick.double@objectpartners.com or pat@patdouble.com
 */
package grails.plugin.cookiesession

import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.InitializingBean

import java.util.regex.Pattern

/**
 * Handles spring-security-core specifics for session attributes.
 *
 * The primary issue addressed relates to when the spring-security core's SecurityContextPersistenceFilter
 * writes the current security context to the SecurityContextRepository. In most cases, the
 * SecurityContextPersistenceFilter stores the current security context after the current web response has been written.
 * This is a problem for the cookie-session plugin because the session is stored in cookies in the web response. As a
 * result, the current security context is never saved in the session, in effect losing the security context after each
 * request. To work around this issue, the cookie-session plugin writes the
 * current security context to the session just before the session is serialized and saved in cookies.
 *
 * The next issue involves cookies saved in the DefaultSavedRequest. DefaultSavedRequest is created by spring security
 * core and stored in the session during redirects, such as after authentication. This listener detects the presence of
 * a DefaultSavedRequest in the session and removes any
 * cookie-session cookies it may be storing. This ensures that old session information doesn't replace more current
 * session information when following a redirect. This also reduces the size of the the serialized session because the
 * DefaultSavedRequest is storing an old copy of a session in the current session.
 */
@Slf4j
class SecurityContextSessionPersistenceListener implements SessionPersistenceListener, InitializingBean {
    static final String SPRING__SECURITY__CONTEXT = 'SPRING_SECURITY_CONTEXT'

    static final List<String> SPRING_SECURITY_SAVED_REQUEST = [
            'SPRING_SECURITY_SAVED_REQUEST_KEY', // needed for backwards compatibility
            'SPRING_SECURITY_SAVED_REQUEST',
    ]

    GrailsApplication grailsApplication
    Class securityContextHolder

    Pattern cookiePattern = Pattern.compile(/^gsession-\d+$/)

    @Override
    void afterPropertiesSet() {
        log.trace 'afterPropertiesSet()'
        if (grailsApplication.config.grails.plugin.cookiesession.containsKey('cookiename')) {
            String cookieName = grailsApplication.config.grails.plugin.cookiesession.cookiename
            cookiePattern = Pattern.compile(/^${cookieName}-\d+$/)
        }

        securityContextHolder = grailsApplication.classLoader.loadClass('org.springframework.security.core.context.SecurityContextHolder')
    }

    @Override
    void afterSessionRestored(SerializableSession session) {
    }

    @Override
    void beforeSessionSaved(SerializableSession session) {
        log.trace 'beforeSessionSaved()'

        SPRING_SECURITY_SAVED_REQUEST.each { savedRequestKey ->
            def sr = session.getAttribute(savedRequestKey)
            if (sr) {
                boolean removed = false
                if (sr.@cookies.removeIf { it.name ==~ cookiePattern }) {
                    session.dirty = true
                    removed = true
                }
                if (sr.@headers.keySet().removeIf { it.toLowerCase() == 'cookie' }) {
                    session.dirty = true
                    removed = true
                }
                if (removed) {
                    log.trace 'removed cookies from saved request in {}', savedRequestKey
                }
            }
        }

        if (session.getAttribute(SPRING__SECURITY__CONTEXT)
                && !session.getAttribute(SPRING__SECURITY__CONTEXT).is(securityContextHolder.getContext())) {
            log.info 'persisting security context to session'
            session.setAttribute(SPRING__SECURITY__CONTEXT, securityContextHolder.getContext())
        } else {
            log.trace 'not persisting security context'
        }
    }
}
