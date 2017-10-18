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
 * Kryo serializer for UsernamePasswordAuthenticationToken. This class is not specific to a class, but
 * does expect properties consistent with UsernamePasswordAuthenticationToken.
 */
@Slf4j
class UsernamePasswordAuthenticationTokenSerializer extends Serializer<Object> {
    final Class targetClass
    final Constructor constructor

    UsernamePasswordAuthenticationTokenSerializer(Class targetClass) {
        this.targetClass = targetClass
        constructor = targetClass.getConstructor(Object, Object, Collection)
    }

    @Override
    void write(Kryo kryo, Output output, Object token) {
        log.trace 'starting writing {}', token
        kryo.writeClassAndObject(output, token.principal)
        kryo.writeClassAndObject(output, token.credentials)
        kryo.writeClassAndObject(output, token.authorities)
        log.trace 'writing authorities: {} - {}', token.authorities.class.name, token.authorities
        kryo.writeClassAndObject(output, token.details)
        log.trace 'finished writing {}', token
    }

    @Override
    Object read(Kryo kryo, Input input, Class<Object> type) {
        log.trace 'starting reading UsernamePasswordAuthenticationToken'
        def principal = kryo.readClassAndObject(input)
        def credentials = kryo.readClassAndObject(input)
        def authorities = kryo.readClassAndObject(input)
        log.trace 'Authorities: {}', authorities
        if (authorities) {
            authorities.each { log.trace('{}, {}', it.class.name, it) }
        }
        def details = kryo.readClassAndObject(input)

        def token = constructor.newInstance(principal, credentials, authorities)
        token.details = details

        log.trace 'finished reading UsernamePasswordAuthenticationToken {}', token

        return token
    }
}
