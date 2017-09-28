package grails.plugin.cookiesession

import grails.testing.web.controllers.ControllerUnitTest
import grails3_3.SecureController

class KryoSessionSerializerTest extends SessionTests implements SessionFixture, ControllerUnitTest<SecureController> {
    def setup() {
        setupCookieSession('kryo')
    }
}
