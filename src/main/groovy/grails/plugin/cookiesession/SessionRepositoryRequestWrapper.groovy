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
 *  benlucchesi@gmail.com
 */
package grails.plugin.cookiesession

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import javax.servlet.http.HttpSession

@CompileStatic
@Slf4j
class SessionRepositoryRequestWrapper extends HttpServletRequestWrapper {
    ServletContext servletContext
    SerializableSession session
    SessionRepository sessionRepository
    Collection<SessionPersistenceListener> sessionPersistenceListeners

    SessionRepositoryRequestWrapper(HttpServletRequest request, SessionRepository sessionRepository) {
        super(request)
        this.sessionRepository = sessionRepository
    }

    void restoreSession() {
        log.trace('restoreSession()')

        // use the sessionRepository to attempt to retrieve session object
        // if a session was restored
        // - set isNew == false
        // - assign the servlet context

        session = sessionRepository.restoreSession(this)

        if (sessionPersistenceListeners != null) {
            // call sessionPersistenceListeners
            for (SessionPersistenceListener listener : sessionPersistenceListeners) {
                try {
                    listener.afterSessionRestored(session)
                }
                catch (Exception excp) {
                    log.error('Error calling SessionPersistenceListener.afterSessionRestored()', excp)
                }
            }
        }
    }

    @Override
    HttpSession getSession(boolean create) {

        log.trace('getSession({})', create)

        // if there isn't an existing session object and create == true, then
        // - create a new session object
        // - set isNew == true
        // - assign the servlet context
        // else
        // - return the session field regardless if what it contains

        if (session == null && create) {
            log.trace('creating new session')
            session = new SerializableSession()
            session.setIsNewSession(true)
            session.setServletContext(this.getServletContext())
        }

        return (session)
    }

    @Override
    HttpSession getSession() {
        log.trace('getSession()')
        return (this.getSession(true))
    }

    @Override
    boolean isRequestedSessionIdValid() {
        log.trace('isRequestedSessionIdValid()')

        // session repository is responsible for determining if the requested session id is valid.
        return sessionRepository.isSessionIdValid(this.getRequestedSessionId())
    }

    ServletContext getServletContext() {
        return servletContext
    }

    void setServletContext(ServletContext ctx) {
        servletContext = ctx
    }
}
