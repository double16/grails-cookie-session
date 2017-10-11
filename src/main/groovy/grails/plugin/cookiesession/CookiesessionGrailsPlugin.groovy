package grails.plugin.cookiesession

import grails.plugins.Plugin
import org.springframework.core.Ordered

/**
 * Grails plugin implementation for cookie-session.
 */
class CookiesessionGrailsPlugin extends Plugin {

    def grailsVersion = '3.0.0 > *'

    def loadAfter = ['controllers']

    def title = 'Cookie Session Plugin'
    def author = 'Patrick Double'
    def authorEmail = 'pat@patdouble.com'
    def description = '''
The Cookie Session plugin enables grails applications to store session data in http cookies between requests instead of in memory on the server. This allows application deployments to be more stateless which supports simplified scaling architectures and fault tolerance.'''
    def profiles = ['web']

    def documentation = 'https://github.com/double16/grails-cookie-session'

    def license = 'APACHE'

    def developers = [[name: 'Ben Lucchesi', email: 'benlucchesi@gmail.com']]

    def issueManagement = [system: 'GitHub', url: 'https://github.com/double16/grails-cookie-session/issues']

    def scm = [url: 'https://github.com/double16/grails-cookie-session.git']

    Closure doWithSpring() {
        { ->

            if (!config.grails.plugin.cookiesession.enabled) {
                return
            }

            sessionRepository(CookieSessionRepository) { bean ->
                bean.autowire = 'byName'
            }

            if (config.grails.plugin.cookiesession.containsKey('condenseexceptions')
                    && config.grails.plugin.cookiesession['condenseexceptions'] == true) {
                exceptionCondenser(ExceptionCondenser)
            }

            // ALWAYS CONFIGURED!
            javaSessionSerializer(JavaSessionSerializer) { bean ->
                bean.autowire = 'byName'
            }

            if (config.grails.plugin.cookiesession.containsKey('serializer')
                    && config.grails.plugin.cookiesession['serializer'] == 'kryo') {
                kryoSessionSerializer(KryoSessionSerializer) { bean ->
                    bean.autowire = 'byName'
                }
            }

            if (config.grails.plugin.cookiesession.containsKey('springsecuritycompatibility')
                    && config.grails.plugin.cookiesession['springsecuritycompatibility'] == true) {
                securityContextSessionPersistenceListener(SecurityContextSessionPersistenceListener) { bean ->
                    bean.autowire = 'byName'
                }
            }

            Class filterRegistrationBeanClass
            try {
                filterRegistrationBeanClass = Class.forName('org.springframework.boot.web.servlet.FilterRegistrationBean')
            } catch (ClassNotFoundException e) {
                filterRegistrationBeanClass = Class.forName('org.springframework.boot.context.embedded.FilterRegistrationBean')
            }
            cookieSessionFilter(filterRegistrationBeanClass) {
                filter = bean(CookieSessionFilter) {
                    sessionRepository = ref('sessionRepository')
                }
                order = Ordered.HIGHEST_PRECEDENCE + 25
            }

        }
    }
}
