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
import spock.lang.Unroll

@Unroll
class GrailsUserSerializerTest extends AbstractKryoSerializerTest {
    @EqualsAndHashCode(excludes = ['authorities', 'password'])
    @ToString(includeNames = true)
    static class TestGrailsUser extends UserSerializerTest.TestUser {
        def id

        TestGrailsUser(String username, String password, boolean enabled,
                       boolean accountNonExpired, boolean credentialsNonExpired,
                       boolean accountNonLocked, Collection authorities, id) {
            super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities)
            this.id = id
        }
    }

    void setup() {
        kryo.register(TestGrailsUser, new GrailsUserSerializer(TestGrailsUser))
    }

    void "serialize and deserialize"() {
        given:
        def input = new TestGrailsUser(username, password, enabled,
                accountNonExpired, credentialsNonExpired,
                accountNonLocked, [], 'id12345')

        when:
        def result = serde(input)
        then:
        result == input

        where:
        username | password | enabled | accountNonExpired | credentialsNonExpired | accountNonLocked
        'user1'  | 'pass1'  | true    | true              | true                  | true
        'user1'  | 'pass1'  | false   | true              | true                  | true
        'user1'  | 'pass1'  | true    | false             | true                  | true
        'user1'  | 'pass1'  | true    | true              | false                 | true
        'user1'  | 'pass1'  | true    | true              | true                  | false
        'user1'  | ''       | false   | false             | false                 | false
    }

    void "skip authorities property"() {
        given:
        def input = new TestGrailsUser('user1', 'pass1', true, true, true, true, ['ROLE_ADMIN'], 'id12345')

        when:
        def result = serde(input)
        then:
        result.authorities.empty
    }

    void "skip password property"() {
        given:
        def input = new TestGrailsUser('user1', 'pass1', true, true, true, true, ['ROLE_ADMIN'], 'id12345')

        when:
        def result = serde(input)
        then:
        result.password.empty
    }

    void "user id types"() {
        given:
        def input = new TestGrailsUser('user1', 'pass1', true, true, true, true, ['ROLE_ADMIN'], userId)

        when:
        def result = serde(input)
        then:
        result.id == userId

        where:
        userId               | _
        'id123456'           | _
        Long.valueOf(123456) | _
    }
}
