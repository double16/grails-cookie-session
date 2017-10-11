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

package grails.plugin.cookiesession.kryo

import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import org.grails.web.servlet.GrailsFlashScope

@TestMixin(ControllerUnitTestMixin)
class GrailsFlashScopeSerializerTest extends AbstractKryoSerializerTest {
    void setup() {
        kryo.register(GrailsFlashScope, new GrailsFlashScopeSerializer())
    }

    void "serialize and deserialize"() {
        given:
        GrailsFlashScope flashScope = new GrailsFlashScope()

        when:
        flashScope.put('attr1', 'value1')
        flashScope.put('attr2', ['value2a', 'value2b'])
        GrailsFlashScope serde1 = serde(flashScope)
        then:
        serde1.get('attr1') == 'value1'
        serde1.get('attr2') == ['value2a', 'value2b']

        when:
        serde1.next()
        then:
        serde1.get('attr1') == 'value1'
        serde1.get('attr2') == ['value2a', 'value2b']

        when:
        serde1.next()
        then:
        serde1.get('attr1') == null
        serde1.get('attr2') == null
    }
}
