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

import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

/**
 * Delegates saving sessions to persistent storage using an instance of SessionRepository.
 */
@CompileStatic
@Slf4j
class SessionRepositoryResponseWrapper extends HttpServletResponseWrapper {
    private final SessionRepository sessionRepository
    private final SessionRepositoryRequestWrapper request
    private final boolean enforceSession
    private Collection<SessionPersistenceListener> sessionPersistenceListeners
    private boolean sessionSaved = false

    SessionRepositoryResponseWrapper(HttpServletResponse response,
                                     SessionRepository sessionRepository,
                                     SessionRepositoryRequestWrapper request,
                                     boolean enforceSession = false) {
        super(response)
        this.sessionRepository = sessionRepository
        this.request = request
        this.enforceSession = enforceSession
    }

    void setSessionPersistenceListeners(Collection<SessionPersistenceListener> value) {
        log.trace('setSessionPersistenceListeners()')
        this.sessionPersistenceListeners = value
    }

    void saveSession() {
        log.trace('saveSession()')

        if (this.isCommitted()) {
            log.trace('response is already committed, not attempting to save.')
            return
        }

        if (sessionSaved) {
            log.trace('session is already saved, not attempting to save again.')
            return
        }

        SerializableSession session = (SerializableSession) request.getSession(this.enforceSession)

        if (session == null) {
            log.trace('session is null, not saving.')
            return
        }

        if (!session.dirty && !session.isNew()) {
            log.trace('session is not dirty, not saving.')
            return
        }

        // flag the session as saved.
        sessionSaved = true

        log.trace('calling session repository to save session.')

        // call sessionPersistenceListeners
        if (sessionPersistenceListeners != null) {
            for (SessionPersistenceListener listener : sessionPersistenceListeners) {
                log.trace('calling session persistence listener: {}', listener)
                try {
                    listener.beforeSessionSaved(session)
                }
                catch (Exception excp) {
                    log.error('Error calling SessionPersistenceListener.beforeSessionSaved()', excp)
                }
            }
        } else {
            log.trace('no session persistence listeners called.')
        }

        sessionRepository.saveSession(session, this)
    }

    @Override
    void setStatus(int sc) {
        log.trace('intercepting setStatus({}) to save session', sc)
        this.saveSession()
        super.setStatus(sc)
    }

    @Override
    @SuppressWarnings('Deprecated')
    void setStatus(int sc, String sm) {
        log.trace('intercepting setStatus({}, {}) to save session', sc, sm)
        this.saveSession()
        super.setStatus(sc, sm)
    }

    @Override
    void flushBuffer() throws IOException {
        log.trace('intercepting flushBuffer to save session')
        this.saveSession()
        super.flushBuffer()
    }

    @Override
    PrintWriter getWriter() throws IOException {
        log.trace('intercepting getWriter to save session')
        this.saveSession()
        super.getWriter()
    }

    @Override
    ServletOutputStream getOutputStream() throws IOException {
        log.trace('intercepting getOutputStream to save session')
        this.saveSession()
        super.getOutputStream()
    }

    @Override
    void sendRedirect(String location) throws IOException {
        log.trace('intercepting sendRedirect({}) to save session', location)
        this.saveSession()
        super.sendRedirect(location)
    }

    @Override
    void sendError(int sc) throws IOException {
        log.trace('intercepting sendError({}) to save session', sc)
        this.saveSession()
        super.sendError(sc)
    }

    @Override
    void sendError(int sc, String msg) throws IOException {
        log.trace('intercepting sendError({}, {}) to save session', sc, msg)
        this.saveSession()
        super.sendError(sc, msg)
    }
}
