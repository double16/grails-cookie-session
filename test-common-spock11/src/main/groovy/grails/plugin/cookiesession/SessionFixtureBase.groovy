package grails.plugin.cookiesession

import grails.config.Config
import grails.plugin.cookiesession.mock.MockConfig
import grails.plugin.cookiesession.mock.MockCookieSessionPluginManager
import grails.plugin.cookiesession.mock.MockGrailsApplication
import org.grails.spring.GrailsApplicationContext
import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.mock.web.MockHttpServletRequest

import javax.servlet.http.HttpSession

trait SessionFixtureBase {
    CookieSessionRepository cookieSessionRepository

    /** The desired maximum session size. */
    int maxSessionSize() {
        cookieSessionRepository.cookieCount * cookieSessionRepository.maxCookieSize
    }

    void setupCookieSession(String serializer = 'kryo') {
        GrailsApplicationContext mainContext = new GrailsApplicationContext()
        mainContext.registerSingleton('config', MockConfig, new MutablePropertyValues([serializer: serializer]))
        mainContext.registerBeanDefinition('javaSessionSerializer', new GenericBeanDefinition(beanClass:  JavaSessionSerializer, autowireMode: GenericBeanDefinition.AUTOWIRE_BY_NAME))
        mainContext.registerBeanDefinition('kryoSessionSerializer', new GenericBeanDefinition(beanClass:  KryoSessionSerializer, autowireMode: GenericBeanDefinition.AUTOWIRE_BY_NAME))
        mainContext.registerBeanDefinition('sessionRepository', new GenericBeanDefinition(beanClass:  CookieSessionRepository, autowireMode: GenericBeanDefinition.AUTOWIRE_BY_NAME))
        mainContext.registerBeanDefinition('grailsApplication', new GenericBeanDefinition(beanClass:  MockGrailsApplication, autowireMode: GenericBeanDefinition.AUTOWIRE_BY_NAME))
        mainContext.registerBeanDefinition('pluginManager', new GenericBeanDefinition(beanClass:  MockCookieSessionPluginManager, autowireMode: GenericBeanDefinition.AUTOWIRE_BY_NAME))
        mainContext.refresh()
        mainContext.start()

        MockGrailsApplication grailsApplication = (MockGrailsApplication) mainContext.getBean('grailsApplication')
        grailsApplication.setConfig((Config) mainContext.getBean('config'))

        cookieSessionRepository = (CookieSessionRepository) mainContext.getBean('sessionRepository')
    }

    boolean equals(HttpSession session1, HttpSession session2) {
        Set<String> attributeNames = session1.attributeNames.toSet()
        if (!attributeNames.equals(session2.attributeNames.toSet())) {
            return false
        }
        attributeNames.find { session1.getAttribute(it) != session2.getAttribute(it) } == null
    }

    CookieSessionRepository getCookieSessionRepository() {
        assert cookieSessionRepository : 'setupCookieSession(...) has not been called'
        cookieSessionRepository
    }

    String serializeSession(SerializableSession session) {
        getCookieSessionRepository().serializeSession(session)
    }

    SerializableSession deserializeSession(String serialized) {
        getCookieSessionRepository().deserializeSession(serialized, new MockHttpServletRequest())
    }

    SerializableSession emptySession() {
        new SerializableSession()
    }

    abstract SerializableSession flashScopeSession()

    abstract SerializableSession anonymousAuthenticatedSession()

    abstract SerializableSession authenticatedSession()

    abstract SerializableSession preauthWithSavedRequestSession()
}
