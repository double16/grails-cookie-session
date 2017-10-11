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

/**
 * Session serializer using serialization built-in to the JDK. This method is the most compatible but
 * is also slow and produces large results.
 */
@CompileStatic
@Slf4j
class JavaSessionSerializer implements SessionSerializer {
    GrailsApplication grailsApplication

    void serialize(SerializableSession session, OutputStream outputStream) {
        log.trace 'serializeSession()'
        ObjectOutputStream output = new ObjectOutputStream(outputStream)
        output.writeObject(session)
        output.close()
    }

    SerializableSession deserialize(InputStream serializedSession) {

        log.trace 'deserializeSession()'

        ObjectInputStream inputStream = new ObjectInputStream(serializedSession) {
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
