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

import org.springframework.security.authentication.RememberMeAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class RememberMeAuthenticationTokenSerializerTest extends AbstractKryoSerializerTest {
    Collection<? extends GrantedAuthority> authorities
    GrailsUserSerializerTest.TestGrailsUser user

    void setup() {
        kryo.register(RememberMeAuthenticationToken, new RememberMeAuthenticationTokenSerializer(RememberMeAuthenticationToken))
        authorities = [new GrantedAuthoritySerializerTest.SimpleAuthority('ROLE_USER')]
        user = new GrailsUserSerializerTest.TestGrailsUser('user1', 'pass1', true, true, true, true, authorities, 123456L)
    }

    void "serialize and deserialize"() {
        given:
        def token = new RememberMeAuthenticationToken('temporary', user, authorities)
        token.details = createDetails()

        when:
        def token2 = serde(token)
        then:
        token2.keyHash == token.keyHash
        token2.principal == token.principal
        token2.details == token.details
        token2.authorities == token.authorities
    }
}
