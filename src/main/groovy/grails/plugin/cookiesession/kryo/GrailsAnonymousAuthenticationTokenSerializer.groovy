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
import grails.plugin.springsecurity.authentication.GrailsAnonymousAuthenticationToken
import groovy.transform.CompileStatic
import org.springframework.security.authentication.AnonymousAuthenticationToken

import java.lang.reflect.Field

/**
 * Kryo serializer for GrailsAnonymousAuthenticationToken.
 */
@CompileStatic
class GrailsAnonymousAuthenticationTokenSerializer extends Serializer<GrailsAnonymousAuthenticationToken> {
    static final Field KEY_HASH_FIELD = AnonymousAuthenticationToken.getDeclaredField('keyHash')
    static {
        KEY_HASH_FIELD.accessible = true
    }

    @Override
    void write(Kryo kryo, Output output, GrailsAnonymousAuthenticationToken object) {
        output.writeInt(object.keyHash)
        kryo.writeClassAndObject(output, object.details)
    }

    @Override
    GrailsAnonymousAuthenticationToken read(Kryo kryo, Input input, Class<GrailsAnonymousAuthenticationToken> type) {
        int keyHash = input.readInt()
        def details = kryo.readClassAndObject(input)
        GrailsAnonymousAuthenticationToken token = new GrailsAnonymousAuthenticationToken('temporary', details)
        KEY_HASH_FIELD.setInt(token, keyHash)
        return token
    }
}
