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
import org.apache.commons.lang.builder.ReflectionToStringBuilder
import org.apache.commons.lang.builder.ToStringStyle

import javax.servlet.ServletContext
import javax.servlet.http.HttpSession
import javax.servlet.http.HttpSessionBindingEvent
import javax.servlet.http.HttpSessionBindingListener
import javax.servlet.http.HttpSessionContext

@CompileStatic
@Slf4j
class SerializableSession implements HttpSession, Serializable {
    // borrowed from other cookie session plugin to fake a session context
    @SuppressWarnings('deprecation')
    /* ServletAPI */
    private
    static final HttpSessionContext SESSION_CONTEXT = new HttpSessionContext() {
        HttpSession getSession(String sessionId) {
            return null
        }

        Enumeration<String> getIds() {
            return SESSION_CONTEXT_ID_ENUM
        }
    }

    private static final Enumeration<String> SESSION_CONTEXT_ID_ENUM = new Enumeration<String>() {
        String nextElement() {
            return null
        }

        boolean hasMoreElements() {
            return false
        }
    }

    private static final long serialVersionUID = 42L
    private long creationTime = 0
    long lastAccessedTime = 0
    private Map<String, Serializable> attributes

    transient Boolean isValid = true
    transient ServletContext servletContext
    transient private boolean newSession
    transient int maxInactiveInterval

    SerializableSession() {
        this.creationTime = System.currentTimeMillis()
        this.lastAccessedTime = this.creationTime
        this.attributes = new HashMap<String, Serializable>()
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
        isValid = false
    }

    protected void setIsNewSession(boolean isNewSession) {
        this.newSession = isNewSession
    }

    boolean isNew() {
        this.newSession
    }

    @Override
    String toString() {
        attributes.collect { k, v -> "${k}=${ReflectionToStringBuilder.toString(v, ToStringStyle.MULTI_LINE_STYLE)}" }.join('\n')
    }
}
