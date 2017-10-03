/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Ben Lucchesi
 *  ben@granicus.com or benlucchesi@gmail.com
 */
package grails.plugin.cookiesession

import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.InitializingBean
import org.springframework.security.web.savedrequest.SavedRequest

@Slf4j
class SecurityContextSessionPersistenceListener implements SessionPersistenceListener, InitializingBean {
    static final String SPRING__SECURITY__CONTEXT = 'SPRING_SECURITY_CONTEXT'
    // needed for backwards compatibility
    static
    final List<String> SPRING_SECURITY_SAVED_REQUEST = ['SPRING_SECURITY_SAVED_REQUEST_KEY', 'SPRING_SECURITY_SAVED_REQUEST']

    GrailsApplication grailsApplication
    Class securityContextHolder

    String cookieName = 'gsession'

    @Override
    void afterPropertiesSet() {
        log.trace 'afterPropertiesSet()'
        if (grailsApplication.config.grails.plugin.cookiesession.containsKey('cookiename')) {
            cookieName = grailsApplication.config.grails.plugin.cookiesession.cookiename
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
            SavedRequest sr = (SavedRequest) session.getAttribute(savedRequestKey)
            if (sr) {
                sr.@cookies.removeIf { it.name ==~ cookieName }
                sr.@headers.keySet().removeIf { it.toLowerCase() == 'cookie' }
                log.trace 'removed cookies from saved request in {}', savedRequestKey
            }
        }

        if (session.getAttribute(SPRING__SECURITY__CONTEXT) && !session.getAttribute(SPRING__SECURITY__CONTEXT).is(securityContextHolder.getContext())) {
            log.info 'persisting security context to session'
            session.setAttribute(SPRING__SECURITY__CONTEXT, securityContextHolder.getContext())
        } else {
            log.trace 'not persisting security context'
        }
    }
}
