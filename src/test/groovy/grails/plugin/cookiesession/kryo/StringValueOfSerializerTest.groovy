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

import spock.lang.Unroll

import java.time.Instant

@Unroll
class StringValueOfSerializerTest extends AbstractKryoSerializerTest {

    void setup() {
        kryo.register(BigDecimal, new StringValueOfSerializer(BigDecimal))
        kryo.register(Instant, new StringValueOfSerializer(Instant, 'parse'))
    }

    void "BigDecimal #decimal"() {
        given:
        def input = new BigDecimal(decimal as double)

        when:
        def result = serde(input)
        then:
        result == input

        where:
        decimal | _
        1       | _
        2       | _
        1.2     | _
        1.3     | _
    }

    void "Instant #instant"() {
        given:
        def input = Instant.parse(instant)

        when:
        def result = serde(input)
        then:
        result == input

        where:
        instant                | _
        '2017-07-04T12:34:56Z' | _
    }

    void "serialize null"() {
        given:
        def serializer = new StringValueOfSerializer(Instant, 'parse')
        expect:
        serde(Instant, (Instant) null, serializer) == null
    }
}

