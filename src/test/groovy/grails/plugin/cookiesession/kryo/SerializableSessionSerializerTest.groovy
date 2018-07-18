/*
 * Copyright 2012-2018 the original author or authors.
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

import grails.plugin.cookiesession.SerializableSession

class SerializableSessionSerializerTest extends AbstractKryoSerializerTest {
    void setup() {
        kryo.register(SerializableSession, new SerializableSessionSerializer())
    }

    void "serialize and deserialize"() {
        given:
        SerializableSession session = new SerializableSession().defaultSerializer()

        when:
        SerializableSession session2 = serde(session)
        then:
        session2.creationTime == session.creationTime
        session2.lastAccessedTime == session.lastAccessedTime
        session2.attributes == session.attributes
    }
}
