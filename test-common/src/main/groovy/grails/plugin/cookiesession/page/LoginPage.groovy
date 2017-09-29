package grails.plugin.cookiesession.page

import geb.Page

class LoginPage extends Page {
    static at = { title.trim() == 'Login' }
    static content = {
        username { $('#username') }
        password { $('#password') }
        rememberMe { $('#remeber_me') }
        submit { $('#submit') }
    }
}
