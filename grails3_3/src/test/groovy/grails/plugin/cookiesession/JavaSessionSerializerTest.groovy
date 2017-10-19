package grails.plugin.cookiesession

import grails.testing.web.controllers.ControllerUnitTest
import grails3_3.SecureController

class JavaSessionSerializerTest extends SessionTests implements SessionFixture, ControllerUnitTest<SecureController> {
    def setup() {
        setupCookieSession('java')
    }
}
