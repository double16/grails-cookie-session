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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang.builder.ReflectionToStringBuilder
import org.apache.commons.lang.builder.ToStringStyle

import javax.servlet.ServletContext
import javax.servlet.http.HttpSession
import javax.servlet.http.HttpSessionBindingEvent
import javax.servlet.http.HttpSessionBindingListener
import javax.servlet.http.HttpSessionContext
import java.security.DigestOutputStream
import java.security.MessageDigest

/**
 * HttpSession implementation designed for serialization. It also includes deep dirty checking to optimize when the
 * session needs to be sent back to the client. All attribute values in the session must implement Serializable.
 */
@CompileStatic
@Slf4j
class SerializableSession implements HttpSession, Serializable {
    // borrowed from other cookie session plugin to fake a session context
    @SuppressWarnings('deprecation')
    /* ServletAPI */
    private
    static final HttpSessionContext SESSION_CONTEXT = new HttpSessionContext() {
        @SuppressWarnings('UnusedMethodParameter')
        HttpSession getSession(String sessionId) {
            null
        }

        Enumeration<String> getIds() {
            SESSION_CONTEXT_ID_ENUM
        }
    }

    private static final Enumeration<String> SESSION_CONTEXT_ID_ENUM = new Enumeration<String>() {
        String nextElement() {
            throw new NoSuchElementException()
        }

        boolean hasMoreElements() {
            false
        }
    }

    private static final long serialVersionUID = 42L
    private final long creationTime
    long lastAccessedTime = 0
    private Map<String, Serializable> attributes = [:]

    transient boolean isValid
    transient boolean dirty
    /** digest of 'clean' session, used for dirty checking */
    transient byte[] digest
    transient ServletContext servletContext
    transient private boolean newSession
    transient int maxInactiveInterval

    SerializableSession() {
        this.creationTime = System.currentTimeMillis()
        this.lastAccessedTime = this.creationTime
        this.isValid = true
        this.dirty = false
    }

    SerializableSession(long creationTime, long lastAccessedTime, Map<String, Serializable> attributes) {
        this.creationTime = creationTime
        this.lastAccessedTime = lastAccessedTime
        this.attributes = attributes
        this.isValid = true
        this.dirty = false
        digest = digestOfSession()
    }

    long getCreationTime() {
        creationTime
    }

    String getId() {
        return 'simplesession'
    }

    HttpSessionContext getSessionContext() {
        SESSION_CONTEXT
    }

    Object getAttribute(String name) {
        attributes.get(name)
    }

    Object getValue(String name) {
        getAttribute(name)
    }

    Enumeration getAttributeNames() {
        final Iterator<String> keys = attributes.keySet().iterator()
        final Enumeration names = new Enumeration() {
            boolean hasMoreElements() { return keys.hasNext() }

            Object nextElement() { return keys.next() }
        }

        names
    }

    String[] getValueNames() {
        attributes.keySet() as String[]
    }

    void setAttribute(String name, Object value) {
        dirty = true
        Object oldValue = attributes.put(name, (Serializable) value)
        if (value instanceof HttpSessionBindingListener) {
            try {
                ((HttpSessionBindingListener) value).valueBound(new HttpSessionBindingEvent(this, name))
            }
            catch (Exception excp) {
                log.error("failed to set attribute: ${name}", excp)
            }
        }
        if (oldValue instanceof HttpSessionBindingListener) {
            try {
                ((HttpSessionBindingListener) oldValue).valueUnbound(new HttpSessionBindingEvent(this, name))
            }
            catch (Exception excp) {
                log.error("failed to remove old attribute value: ${name}", excp)
            }
        }
    }

    void putValue(String name, Object value) {
        this.setAttribute(name, value)
    }

    void removeAttribute(String name) {
        Object value = attributes.remove(name)
        if (value != null) {
            dirty = true
        }
        if (value instanceof HttpSessionBindingListener) {
            try {
                ((HttpSessionBindingListener) value).valueUnbound(new HttpSessionBindingEvent(this, name))
            }
            catch (Exception excp) {
                log.error("failed to remove attribute: ${name}", excp)
            }
        }
    }

    void removeValue(String name) {
        this.removeAttribute(name)
    }

    void invalidate() {
        if (isValid) {
            dirty = true
        }
        isValid = false
    }

    protected void setIsNewSession(boolean isNewSession) {
        this.newSession = isNewSession
    }

    boolean isNew() {
        this.newSession
    }

    void setDirty(boolean dirty) {
        if (!dirty && this.dirty) {
            // We're marking the session as no longer dirty, save a digest for comparison later
            digest = digestOfSession()
        }
        this.dirty = dirty
    }

    boolean isDirty() {
        if (dirty) {
            return true
        }
        // dirty checking is difficult because the properties of the attribute values could have changed, we keep a digest
        // of the last time the session was set to clean for comparison
        if (digest != null) {
            if (digest != digestOfSession()) {
                dirty = true
            }
        }
        return dirty
    }

    static final OutputStream NULL_OUTPUTSTREAM = new OutputStream() {
        @Override
        void write(int b) throws IOException { }
        @Override
        void write(byte[] b, int off, int len) throws IOException { }
    }

    /**
     * Compute a digest of the session to be used for dirty checking.
     * @return
     */
    private byte[] digestOfSession() {
        log.trace 'computing digest of session'
        DigestOutputStream stream = new DigestOutputStream(NULL_OUTPUTSTREAM, MessageDigest.getInstance('MD5'))
        ObjectOutputStream output = new ObjectOutputStream(stream)
        output.writeObject(attributes)
        output.close()
        byte[] digest = stream.messageDigest.digest()
        log.trace 'digest of session of session is {}', digest
        digest
    }

    Map<String, Serializable> getAttributes() {
        Collections.unmodifiableMap(attributes)
    }

    @Override
    String toString() {
        attributes.collect { k, v ->
            "${k}=${ReflectionToStringBuilder.toString(v, ToStringStyle.MULTI_LINE_STYLE)}"
        }.join('\n')
    }
}
