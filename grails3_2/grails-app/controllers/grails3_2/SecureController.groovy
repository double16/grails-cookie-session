package grails3_2

import grails.plugin.springsecurity.annotation.Secured

class SecureController {
    @Secured('ROLE_ADMIN')
    def index() {
    }
}
