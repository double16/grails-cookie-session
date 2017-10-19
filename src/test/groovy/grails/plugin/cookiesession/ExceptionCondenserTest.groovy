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

class ExceptionCondenserTest extends Specification {
    ExceptionCondenser condenser

    void setup() {
        condenser = new ExceptionCondenser()
    }

    void "afterSessionRestored has no side effects"() {
        when:
        condenser.afterSessionRestored(new SerializableSession())
        then:
        notThrown(Exception)
    }

    void "beforeSessionSaved"() {
        given:
        SerializableSession session = new SerializableSession()
        session.setAttribute('attr1', 'value1')
        session.setAttribute('attr2', new IOException('I/O error'))
        when:
        condenser.beforeSessionSaved(session)
        then:
        session.getAttribute('attr1') == 'value1'
        session.getAttribute('attr2') == 'I/O error'
    }
}
