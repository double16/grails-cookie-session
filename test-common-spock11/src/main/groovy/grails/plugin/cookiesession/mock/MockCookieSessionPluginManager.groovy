package grails.plugin.cookiesession.mock

import grails.plugins.GrailsPlugin
import grails.plugins.exceptions.PluginException
import groovy.transform.InheritConstructors
import org.grails.plugins.MockGrailsPluginManager

@InheritConstructors
class MockCookieSessionPluginManager extends MockGrailsPluginManager {
    @Override
    void loadPlugins() throws PluginException {
        try {
            getClass().classLoader.loadClass('org.springframework.security.authentication.UsernamePasswordAuthenticationToken')

            registerMockPlugin([
                    getVersion: {'3.1.0'},
                    getName: { 'springSecurityCore' },
                    name: 'springSecurityCore',
                    setApplicationContext: { },
            ] as GrailsPlugin)

        } catch (ClassNotFoundException e) {
            // this is ok if we're not testing with spring-security
        }
        super.loadPlugins()
    }
}

