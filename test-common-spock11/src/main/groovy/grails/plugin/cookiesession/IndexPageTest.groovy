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
import grails.plugin.cookiesession.page.AssignToFlash
import grails.plugin.cookiesession.page.AssignToSession
import grails.plugin.cookiesession.page.DumpFlash
import grails.plugin.cookiesession.page.DumpSession
import grails.plugin.cookiesession.page.IndexSecuredPage
import grails.plugin.cookiesession.page.LoginPage
import grails.plugin.cookiesession.page.Logout
import grails.plugin.cookiesession.page.Reauthenticate
import grails.plugin.cookiesession.page.RedirectTarget
import grails.plugin.cookiesession.page.RedirectTest
import grails.plugin.cookiesession.page.SessionExists
import grails.plugin.cookiesession.page.StoreLargeException
import grails.plugin.cookiesession.page.WhoAmI
import grails.util.Holders
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Requires

class IndexPageTest extends GebSpec {
    static boolean SPRING_SECURITY_PRESENT = false
    static {
        try {
            Class.forName('org.springframework.security.authentication.UsernamePasswordAuthenticationToken')
            SPRING_SECURITY_PRESENT = true
        } catch (ClassNotFoundException e) {
            SPRING_SECURITY_PRESENT = false
        }
    }

    void setup() {
        boolean ssl = Holders.config.server.ssl.enabled as boolean
        if (ssl) {
            browser.baseUrl = browser.baseUrl.replace('http://', 'https://')
        }
    }

    def "data written to the session should be retrievable from the session"() {
        given:
        to AssignToSession, key: "lastname", val: "lucchesi"
        expect:
        at AssignToSession
        when:
        to DumpSession
        then:
        $("#lastname").text() == "lucchesi"
    }

    def "cookie session plugin should work with the flash scope"() {
        when:
        to DumpFlash
        then:
        at DumpFlash
        $("#firstname").text() == null

        when:
        to AssignToFlash, key: "firstname", val: "ben"
        then:
        at AssignToFlash
        $("#key").text() == "firstname"
        $("#val").text() == "ben"

        when:
        to DumpFlash
        then:
        at DumpFlash
        $("#firstname").text() == "ben"

        when:
        to DumpFlash
        then:
        at DumpFlash
        $("#firstname").text() == null

    }

    def "the cookie session should support being invalidated"() {
        given:
        to AssignToSession, key: "lastname", val: "lucchesi"
        to AssignToSession, key: "firstname", val: "benjamin"
        expect:
        at AssignToSession

        when:
        to DumpSession
        then:
        $("#lastname").text() == "lucchesi"
        $("#firstname").text() == "benjamin"

        when:
        go "/index/invalidateSession"
        and:
        to DumpSession
        then:
        $("#lastname").text() == null
        $("#firstname").text() == null

        when:
        to AssignToSession, key: "lastname", val: "lucchesi"
        to AssignToSession, key: "firstname", val: "benjamin"
        to DumpSession
        then:
        $("#lastname").text() == "lucchesi"
        $("#firstname").text() == "benjamin"
    }

    @IgnoreIf({ System.getenv('COOKIE_SESSION_ENABLED') == 'false' })
    def "the cookie session should expire when the max inactive interval is exceeded"() {
        given:
        go "/index/configureSessionRepository?maxInactiveInterval=10"
        to AssignToSession, key: "lastname", val: "lucchesi"
        expect:
        at AssignToSession

        when:
        to DumpSession
        then:
        $("#lastname").text() == "lucchesi"

        when:
        sleep(5000)
        to DumpSession
        then:
        $("#lastname").text() == "lucchesi"

        when:
        sleep(11000)
        to DumpSession
        then:
        $("#lastname").text() == null

        when:
        to AssignToSession, key: "lastname", val: "lucchesi"
        to DumpSession
        then:
        $("#lastname").text() == "lucchesi"
    }

    @Requires({IndexPageTest.SPRING_SECURITY_PRESENT})
    def "the cookie session should be compatible with spring-security"() {
        when:
        to LoginPage
        then:
        at LoginPage

        when:
        username = "testuser"
        password = "password"
        submit.click()
        to WhoAmI
        then:
        username == "testuser"
    }

    @Requires({IndexPageTest.SPRING_SECURITY_PRESENT})
    def "the cookie session should be method compatible with Secured attributes"() {
        when:
        to IndexSecuredPage
        then:
        at LoginPage

        when:
        username = "testuser"
        password = "password"
        submit.click()
        to IndexSecuredPage
        then:
        username == "testuser"
    }

    def "the cookie session should be set and sent with controller redirects"() {
        when:
        to RedirectTest

        then:
        at RedirectTarget
        flashMessage == "this is a flash message"
    }

    @IgnoreIf({ System.getenv('COOKIE_SESSION_ENABLED') == 'false' })
    def "exceptions should only be stored as strings"() {
        when:
        to StoreLargeException
        sleep(2000)
        to DumpSession

        then:
        $("#lastError").text() == "exception from recursive method: 1000"
    }

    @Requires({IndexPageTest.SPRING_SECURITY_PRESENT})
    def "detect Locale serialization problem after multiple refreshes"() {
        when:
        to IndexSecuredPage
        then:
        at LoginPage

        when:
        username = "testuser"
        password = "password"
        submit.click()
        to IndexSecuredPage
        then:
        username == "testuser"

        when:
        to WhoAmI
        then:
        username == "testuser"
    }

    @Requires({IndexPageTest.SPRING_SECURITY_PRESENT})
    def "test reauthenticate security method"() {
        when:
        to Logout
        to WhoAmI
        then:
        username != "testuser"

        when:
        to LoginPage
        username = "admin"
        password = "password"
        submit.click()
        to WhoAmI
        then:
        username == "admin"

        when:
        to Reauthenticate
        to WhoAmI

        then:
        username == "testuser"
    }

    @Ignore('spring security requires the session to be eagerly created')
    def "test delayed session creation"() {
        when:
        to Logout
        to WhoAmI
        then:
        username != "testuser"

        when:
        to SessionExists

        then:
        sessionExists == false

        when:
        to LoginPage
        username = "testuser"
        password = "password"
        submit.click()
        to SessionExists
        then:
        sessionExists == true

        when:
        to Logout
        to SessionExists
        then:
        sessionExists == false
    }
}
