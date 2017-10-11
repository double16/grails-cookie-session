package grails.plugin.cookiesession

import spock.lang.Specification

class ExceptionCondenserTest extends Specification {
    ExceptionCondenser condenser

    void setup() {
        condenser = new ExceptionCondenser()
    }

    void "afterSessionRestored has no side effects"() {
        when:
        condenser.afterSessionRestored(new SerializableSession())
        then:
        notThrown(Exception)
    }

    void "beforeSessionSaved"() {
        given:
        SerializableSession session = new SerializableSession()
        session.setAttribute('attr1', 'value1')
        session.setAttribute('attr2', new IOException('I/O error'))
        when:
        condenser.beforeSessionSaved(session)
        then:
        session.getAttribute('attr1') == 'value1'
        session.getAttribute('attr2') == 'I/O error'
    }
}
