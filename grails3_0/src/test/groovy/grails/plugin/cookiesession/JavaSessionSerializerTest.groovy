package grails.plugin.cookiesession

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.mixin.web.ControllerUnitTestMixin

@TestMixin([GrailsUnitTestMixin, ControllerUnitTestMixin])
class JavaSessionSerializerTest extends SessionTests implements SessionFixture {
    def setup() {
        setupCookieSession('java')
    }
}
