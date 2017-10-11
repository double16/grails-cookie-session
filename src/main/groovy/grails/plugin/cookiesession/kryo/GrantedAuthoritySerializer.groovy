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

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import groovy.util.logging.Slf4j

import java.lang.reflect.Constructor

/**
 * Serializer for GrantedAuthority objects that hold a single String. The target class
 * is expected to have a property named 'authority'.
 */
@Slf4j
class GrantedAuthoritySerializer extends Serializer<Object> {
    final Class targetClass
    final Constructor constructor

    GrantedAuthoritySerializer(Class targetClass) {
        this.targetClass = targetClass
        constructor = targetClass.getConstructor(String)
    }

    @Override
    void write(Kryo kryo, Output output, Object grantedAuth) {
        log.trace 'started writing GrantedAuthority {}', grantedAuth
        output.writeString(grantedAuth.authority as String)
        log.trace 'finished writing GrantedAuthority {}', grantedAuth
    }

    @Override
    Object read(Kryo kryo, Input input, Class<Object> type) {
        log.trace 'starting reading GrantedAuthority'
        def role = input.readString()
        def ga = constructor.newInstance(role)
        log.trace 'finished reading GrantedAuthority: {}', ga
        return ga
    }
}
