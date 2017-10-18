package grails.plugin.cookiesession.page

import geb.Page

class HomePage extends Page {
    static url = '/'
    static at = { title.trim() == 'Welcome to Grails' }
}
