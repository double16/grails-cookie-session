package grails.plugin.cookiesession

import geb.spock.GebSpec
import grails.plugin.cookiesession.page.HomePage
import grails.plugin.cookiesession.page.LoginPage
import grails.plugin.cookiesession.page.Secure2Page
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
        when:
        go Secure2Page.url
        then:
        at Secure2Page
        when:
        page.logout.click()
        then:
        at HomePage
    }

    void "remember me authentication"() {
        
    }
}
