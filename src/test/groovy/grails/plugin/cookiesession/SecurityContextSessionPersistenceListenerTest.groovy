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

import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import spock.lang.Specification

class SecurityContextSessionPersistenceListenerTest extends Specification {
    SecurityContextSessionPersistenceListener listener

    void setup() {
        SecurityContext securityContext = new SecurityContextImpl()
        SecurityContextHolder.setContext(securityContext)
        listener = new SecurityContextSessionPersistenceListener()
        listener.securityContextHolder = SecurityContextHolder

    }

    void "SavedRequest cookies are removed"() {
        expect: false
    }

    void "SavedRequest cookies are removed from legacy key"() {
        expect: false

    }

    void "SPRING_SECURITY_CONTEXT is skipped if not present"() {
        expect: false

    }

    void "SPRING_SECURITY_CONTEXT is replaced if different"() {
        expect: false

    }

    void "SPRING_SECURITY_CONTEXT is not replaced if already the correct instance"() {
        expect: false

    }
}
