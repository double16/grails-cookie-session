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

import spock.lang.Specification

import javax.servlet.http.HttpSessionBindingEvent
import javax.servlet.http.HttpSessionBindingListener

class SerializableSessionTest extends Specification {
    static class BindingListener implements HttpSessionBindingListener, Serializable {
        Object value
        int boundCount, unboundCount

        BindingListener(Object value) {
            this.value = value
        }
        @Override
        void valueBound(HttpSessionBindingEvent event) {
            boundCount++
        }

        @Override
        void valueUnbound(HttpSessionBindingEvent event) {
            unboundCount++
        }
    }

    SerializableSession session

    def setup() {
        session = new SerializableSession()
    }

    void "empty session"() {
        expect:
        session.getAttributeNames().toList() == []
        session.getValueNames() as List == []
        !session.dirty
        !session.isNew()
        session.creationTime == session.lastAccessedTime
        session.id
    }

    void "setAttribute"() {
        when:
        session.setAttribute('attr1', 'value1')
        then:
        session.getAttribute('attr1') == 'value1'
        session.getValue('attr1') == 'value1'
        and:
        session.getAttributeNames().toList() == ['attr1']
        session.getValueNames() as List == ['attr1']
    }

    void "putValue"() {
        when:
        session.putValue('attr1', 'value1')
        then:
        session.getAttribute('attr1') == 'value1'
        session.getValue('attr1') == 'value1'
        and:
        session.getAttributeNames().toList() == ['attr1']
        session.getValueNames() as List == ['attr1']
    }

    void "removeAttribute"() {
        given:
        session.setAttribute('attr1', 'value1')
        session.setAttribute('attr2', 'value2')
        when:
        session.removeAttribute('attr1')
        then:
        session.getAttribute('attr1') == null
        session.getAttribute('attr2') == 'value2'
        and:
        session.getAttributeNames().toList() == ['attr2']
        session.getValueNames() as List == ['attr2']
    }

    void "removeValue"() {
        given:
        session.putValue('attr1', 'value1')
        session.putValue('attr2', 'value2')
        when:
        session.removeValue('attr1')
        then:
        session.getValue('attr1') == null
        session.getValue('attr2') == 'value2'
        and:
        session.getAttributeNames().toList() == ['attr2']
        session.getValueNames() as List == ['attr2']
    }

    void "HttpSessionBindingListener bind new attribute"() {
        given:
        def attribute = new BindingListener('abc')
        when:
        session.setAttribute('attr1', attribute)
        then:
        attribute.boundCount == 1
        attribute.unboundCount == 0
    }

    void "HttpSessionBindingListener unbind"() {
        given:
        def attribute = new BindingListener('abc')
        session.setAttribute('attr1', attribute)
        when:
        session.removeAttribute('attr1')
        then:
        attribute.boundCount == 1
        attribute.unboundCount == 1
    }

    void "HttpSessionBindingListener bind/unbind attribute replace"() {
        given:
        def attr1 = new BindingListener('abc')
        def attr2 = new BindingListener('xyz')
        session.setAttribute('attr1', attr1)
        when:
        session.setAttribute('attr1', attr2)
        then:
        attr1.boundCount == 1
        attr1.unboundCount == 1
        attr2.boundCount == 1
        attr2.unboundCount == 0
    }

    void "dirty flag holds value on unchanged session"() {
        given:
        session.setAttribute('attr1', 'value1')
        session.setAttribute('attr2', 'value2')
        when:
        session.dirty = false
        then:
        !session.dirty
        !session.dirty
        when:
        session.dirty = true
        then:
        session.dirty
        session.dirty
        when:
        session.dirty = false
        then:
        !session.dirty
        !session.dirty
        when:
        session.dirty = true
        then:
        session.dirty
        session.dirty
    }

    void "dirty flag set when attributes added"() {
        when:
        session.setAttribute('attr1', 'value1')
        then:
        session.dirty
        when:
        session.setAttribute('attr2', 'value2')
        then:
        session.dirty
    }

    void "dirty flag set when attributes replaced"() {
        given:
        session.setAttribute('attr1', 'value1')
        session.dirty = false
        when:
        session.setAttribute('attr1', 'value2')
        then:
        session.dirty
    }

    void "dirty flag set when attributes removed"() {
        given:
        session.setAttribute('attr1', 'value1')
        session.dirty = false
        when:
        session.removeAttribute('attr1')
        then:
        session.dirty
    }

    void "dirty flag set when attribute properties changed"() {
        given:
        def attr1 = new BindingListener('abc')
        session.setAttribute('attr1', attr1)
        session.dirty = false
        when:
        attr1.value = 'xyz'
        then:
        session.getAttribute('attr1').value == 'xyz'
        session.dirty
    }

    void "removal of missing attribute does not dirty session"() {
        given:
        def attr1 = new BindingListener('abc')
        session.setAttribute('attr1', attr1)
        session.dirty = false
        when:
        session.removeAttribute('attr2')
        then:
        !session.dirty
    }

    void "as string"() {
        given:
        def attr1 = new BindingListener('abc')
        def attr2 = new BindingListener('xyz')
        session.setAttribute('attr1', attr1)
        session.setAttribute('attr2', attr2)
        when:
        String str = session.toString()
        then:
        str.contains('attr1')
        str.contains('abc')
        str.contains('attr2')
        str.contains('xyz')
    }

    void "HttpSessionContext is empty"() {
        expect:
        session.sessionContext.getSession(session.id) == null
        !session.sessionContext.getIds().hasMoreElements()
        when:
        !session.sessionContext.getIds().nextElement()
        then:
        thrown(NoSuchElementException)
    }

    void "invalidate"() {
        when:
        session.invalidate()
        then:
        !session.isValid
    }
}
