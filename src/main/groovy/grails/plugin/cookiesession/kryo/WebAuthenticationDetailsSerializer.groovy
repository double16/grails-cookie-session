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
import groovy.transform.CompileStatic
import org.springframework.security.web.authentication.WebAuthenticationDetails

import java.lang.reflect.Field

/**
 * Kryo serializer for WebAuthenticationDetails.
 */
@CompileStatic
class WebAuthenticationDetailsSerializer extends Serializer<WebAuthenticationDetails> {
    static final Field REMOTEADDR_FIELD = WebAuthenticationDetails.getDeclaredField('remoteAddress')
    static final Field SESSIONID_FIELD = WebAuthenticationDetails.getDeclaredField('sessionId')
    static {
        REMOTEADDR_FIELD.accessible = true
        SESSIONID_FIELD.accessible = true
    }

    @Override
    void write(Kryo kryo, Output output, WebAuthenticationDetails details) {
        output.writeString(REMOTEADDR_FIELD.get(details) as String)
        output.writeString(SESSIONID_FIELD.get(details) as String)
    }

    @Override
    WebAuthenticationDetails read(Kryo kryo, Input input, Class<WebAuthenticationDetails> type) {
        String remoteAddr = input.readString()
        String sessionId = input.readString()
        WebAuthenticationDetails details = kryo.newInstance(WebAuthenticationDetails)
        REMOTEADDR_FIELD.set(details, remoteAddr)
        SESSIONID_FIELD.set(details, sessionId)
        details
    }
}
