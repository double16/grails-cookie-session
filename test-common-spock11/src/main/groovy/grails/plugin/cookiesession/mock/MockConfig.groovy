package grails.plugin.cookiesession.mock

import grails.config.Config
import org.grails.config.PropertySourcesConfig
import org.springframework.beans.factory.FactoryBean

class MockConfig implements FactoryBean<Config> {
    String serializer = 'java'

    @Override
    Config getObject() throws Exception {
        new PropertySourcesConfig().merge(new ConfigSlurper('test').parse("""
grails.plugin.cookiesession.enabled = true
grails.plugin.cookiesession.encryptcookie = true
grails.plugin.cookiesession.cryptoalgorithm = "Blowfish"
grails.plugin.cookiesession.condenseexceptions = true
grails.plugin.cookiesession.cookiecount = 5
grails.plugin.cookiesession.maxcookiesize = 2048
grails.plugin.cookiesession.sethttponly = true
grails.plugin.cookiesession.serializer = '${serializer}'
grails.plugin.cookiesession.springsecuritycompatibility = true
grails.plugin.springsecurity.useSessionFixationPrevention == false
"""))
    }

    @Override
    Class<Config> getObjectType() {
        Config
    }

    @Override
    boolean isSingleton() {
        true
    }
}
