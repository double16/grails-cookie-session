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

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.pool.KryoFactory
import com.esotericsoftware.kryo.pool.KryoPool
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers
import com.esotericsoftware.kryo.serializers.DefaultSerializers
import de.javakaffee.kryoserializers.*
import grails.core.GrailsApplication
import grails.plugin.cookiesession.kryo.*
import grails.plugin.springsecurity.authentication.GrailsAnonymousAuthenticationToken
import groovy.util.logging.Slf4j
import org.grails.web.servlet.GrailsFlashScope
import org.objenesis.strategy.StdInstantiatorStrategy
import org.springframework.beans.factory.InitializingBean
import org.springframework.security.authentication.RememberMeAuthenticationToken
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.security.web.savedrequest.DefaultSavedRequest

import java.lang.reflect.InvocationHandler

@Slf4j
class KryoSessionSerializer implements SessionSerializer, InitializingBean {
    GrailsApplication grailsApplication
    KryoPool kryoPool

    boolean springSecurityCompatibility = false
    String springSecurityPluginVersion

    void afterPropertiesSet() {
        log.trace 'bean properties set, performing bean configuring bean'

        if (grailsApplication.config.grails.plugin.cookiesession.containsKey('springsecuritycompatibility')) {
            springSecurityCompatibility = grailsApplication.config.grails.plugin.cookiesession['springsecuritycompatibility'] ? true : false
            springSecurityPluginVersion = grailsApplication.mainContext.getBean('pluginManager').allPlugins.find {
                it.name == 'springSecurityCore'
            }?.version
        }

        log.trace 'Kryo serializer configured for spring security compatibility: {}', springSecurityCompatibility
        if (springSecurityCompatibility) {
            log.trace 'Kryo serializer detected spring security plugin version: {}', springSecurityPluginVersion
        }

        kryoPool = new KryoPool.Builder({ getConfiguredKryoSerializer() } as KryoFactory).softReferences().build()
    }

    void serialize(SerializableSession session, OutputStream outputStream) {
        kryoPool.run({ Kryo kryo ->
            log.trace 'starting serialize session'
            Output output = new Output(outputStream)
            kryo.writeObject(output, session)
            output.close()
        })
    }

    SerializableSession deserialize(InputStream serializedSession) {
        kryoPool.run({ Kryo kryo ->
            log.trace 'starting deserializing session'
            Input input = new Input(serializedSession)
            SerializableSession session = kryo.readObject(input, SerializableSession)
            log.trace 'finished deserializing session: {}', session
            return session
        })
    }

    private Kryo getConfiguredKryoSerializer() {
        log.trace 'configuring kryo serializer'

        Kryo kryo = new Kryo()
        kryo.instantiatorStrategy = new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy())
        kryo.fieldSerializerConfig.useAsm = true
        kryo.fieldSerializerConfig.optimizedGenerics = true
        //kryo.registrationRequired = true

        kryo.register(SerializableSession, new SerializableSessionSerializer())

        kryo.register(GrailsFlashScope, new GrailsFlashScopeSerializer())
        log.trace 'registered FlashScopeSerializer'

        kryo.register(Locale, new DefaultSerializers.LocaleSerializer())

        if (springSecurityCompatibility) {

            Class grailsUserClass

            Class usernamePasswordAuthenticationTokenClass = grailsApplication.classLoader.loadClass('org.springframework.security.authentication.UsernamePasswordAuthenticationToken')

            if (springSecurityPluginVersion[0].toInteger() >= 2) {
                grailsUserClass = grailsApplication.classLoader.loadClass('grails.plugin.springsecurity.userdetails.GrailsUser')

                Class simpleGrantedAuthorityClass = grailsApplication.classLoader.loadClass('org.springframework.security.core.authority.SimpleGrantedAuthority')
                kryo.register(simpleGrantedAuthorityClass, new GrantedAuthoritySerializer(simpleGrantedAuthorityClass))
                log.trace 'registered SimpleGrantedAuthority serializer'
            } else {
                grailsUserClass = grailsApplication.classLoader.loadClass('org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser')
            }

            Class userClass = grailsApplication.classLoader.loadClass('org.springframework.security.core.userdetails.User')

            try {
                Class grantedAuthorityImplClass = grailsApplication.classLoader.loadClass('org.springframework.security.core.authority.GrantedAuthorityImpl')
                kryo.register(grantedAuthorityImplClass, new GrantedAuthoritySerializer(grantedAuthorityImplClass))
                log.trace 'registered GrantedAuthorityImpl serializer'
            } catch (ClassNotFoundException e) {
                log.trace 'GrantedAuthorityImpl not found, no serializer registered', e
            }

            kryo.register(userClass, new UserSerializer(userClass))
            log.trace 'registered User serializer'

            kryo.register(grailsUserClass, new GrailsUserSerializer(grailsUserClass))
            log.trace 'registered GrailsUser serializer'

            kryo.register(usernamePasswordAuthenticationTokenClass, new UsernamePasswordAuthenticationTokenSerializer(usernamePasswordAuthenticationTokenClass))
            log.trace 'registered UsernamePasswordAuthenticationToken serializer'

            kryo.register(GrailsAnonymousAuthenticationToken, new GrailsAnonymousAuthenticationTokenSerializer())
            log.trace 'registered GrailsAnonymousAuthenticationToken serializer'

            kryo.register(SecurityContextImpl)
            kryo.register(RememberMeAuthenticationToken)
            kryo.register(WebAuthenticationDetails, new WebAuthenticationDetailsSerializer())
            kryo.register(DefaultSavedRequest, new DefaultSavedRequestSerializer(kryo))
        }

        UnmodifiableCollectionsSerializer.registerSerializers(kryo)
        kryo.setClassLoader(grailsApplication.classLoader)
        log.trace 'grailsApplication.classLoader assigned to kryo.classLoader'

        kryo.register(Arrays.asList('').getClass(), new ArraysAsListSerializer())
        kryo.register(HashMap)
        kryo.register(TreeMap, new DefaultSerializers.TreeMapSerializer())
        kryo.register(String[], new DefaultArraySerializers.StringArraySerializer())
        kryo.register(Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer())
        kryo.register(Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer())
        kryo.register(Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer())
        kryo.register(Collections.singletonList('').getClass(), new CollectionsSingletonListSerializer())
        kryo.register(Collections.singleton('').getClass(), new CollectionsSingletonSetSerializer())
        kryo.register(Collections.singletonMap('', '').getClass(), new CollectionsSingletonMapSerializer())
        kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer())
        kryo.register(InvocationHandler.class, new JdkProxySerializer())

        SynchronizedCollectionsSerializer.registerSerializers(kryo)
        log.trace 'configured kryo\'s standard serializers'

        return kryo
    }
}
