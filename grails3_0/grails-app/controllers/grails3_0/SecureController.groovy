package grails3_0

import grails.plugin.springsecurity.annotation.Secured

class SecureController {
    @Secured('ROLE_ADMIN')
    def index() {
    }

    @Secured('ROLE_ADMIN')
    def second() {
        render view: 'index'
    }
}
