package grails.plugin.cookiesession

import spock.lang.Specification

class SerializableSessionTest extends Specification {
    SerializableSession session

    def setup() {
        session = new SerializableSession()
    }

    void "setAttribute"() {
        when:
        session.setAttribute('attr1', 'value1')
        then:
        session.getAttribute('attr1') == 'value1'
        session.getValue('attr1') == 'value1'
    }
}
