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

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.springframework.security.core.GrantedAuthority
import spock.lang.Unroll

@Unroll
class GrantedAuthoritySerializerTest extends AbstractKryoSerializerTest {
    @EqualsAndHashCode
    @ToString
    static class SimpleAuthority implements GrantedAuthority {
        String authority

        SimpleAuthority(String authority) {
            this.authority = authority
        }
    }

    void setup() {
        kryo.register(SimpleAuthority, new GrantedAuthoritySerializer(SimpleAuthority))
    }

    void "serialize and deserialize"() {
        given:
        def input = new SimpleAuthority(authorityStr)

        when:
        def result = serde(input)
        then:
        result.authority == authorityStr

        where:
        authorityStr | _
        'ROLE_ADMIN' | _
        'ROLE_USER'  | _
        ''           | _
    }
}
