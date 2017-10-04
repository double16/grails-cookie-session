/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Ben Lucchesi
 *  benlucchesi@gmail.com
 */
package grails.plugin.cookiesession

import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
class JavaSessionSerializer implements SessionSerializer {
    GrailsApplication grailsApplication

    byte[] serialize(SerializableSession session) {
        log.trace 'serializeSession()'
        ByteArrayOutputStream stream = new ByteArrayOutputStream()
        ObjectOutputStream output = new ObjectOutputStream(stream)
        output.writeObject(session)
        output.close()
        byte[] bytes = stream.toByteArray()
        log.trace 'serialized session. {} bytes.', bytes.length
        return bytes
    }

    SerializableSession deserialize(byte[] serializedSession) {

        log.trace 'deserializeSession()'

        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(serializedSession)) {
            @Override
            Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                //noinspection GroovyUnusedCatchParameter
                try {
                    return grailsApplication.classLoader.loadClass(desc.name)
                } catch (ClassNotFoundException ex) {
                    return Class.forName(desc.name)
                }
            }
        }

        SerializableSession session = (SerializableSession) inputStream.readObject()

        log.trace 'deserialized session.'
        return session
    }
}
