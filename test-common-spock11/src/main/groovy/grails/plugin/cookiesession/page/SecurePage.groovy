package grails.plugin.cookiesession.page

import geb.Page

class SecurePage extends Page {
    static url = '/secure'
    static at = { $('p.secured').text()?.trim() == 'Secure access only'}
}
