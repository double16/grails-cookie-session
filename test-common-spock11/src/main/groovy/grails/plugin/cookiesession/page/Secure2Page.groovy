package grails.plugin.cookiesession.page

import geb.Page

class Secure2Page extends Page {
    static url = '/secure/second'
    static at = { $('p.secured').text()?.trim() == 'Secure access only'}
    static content = {
        logout { $('button.logout') }
    }
}
