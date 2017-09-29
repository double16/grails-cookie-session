package grails.plugin.cookiesession

import geb.spock.GebSpec
import grails.plugin.cookiesession.page.LoginPage
import grails.plugin.cookiesession.page.SecurePage

class SecuredPageTest extends GebSpec {

    void "login to secured page"() {
        when:
        via SecurePage
        then:
        at LoginPage
        when:
        page.username = 'admin'
        page.password = 'password'
        page.submit.click()
        then:
        at SecurePage
    }
}
