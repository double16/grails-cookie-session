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

package grails.plugin.cookiesession

import geb.spock.GebSpec
import grails.plugin.cookiesession.page.HomePage
import grails.plugin.cookiesession.page.LoginPage
import grails.plugin.cookiesession.page.Secure2Page
import grails.plugin.cookiesession.page.SecurePage
import grails.util.Holders

class SecuredPageTest extends GebSpec {

    void setup() {
        boolean ssl = Holders.config.server.ssl.enabled as boolean
        if (ssl) {
            browser.baseUrl = browser.baseUrl.replace('http://', 'https://')
        }
    }

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
