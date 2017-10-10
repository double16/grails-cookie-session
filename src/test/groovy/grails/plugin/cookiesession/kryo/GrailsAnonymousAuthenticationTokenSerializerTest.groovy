package grails.plugin.cookiesession.kryo

import grails.plugin.springsecurity.authentication.GrailsAnonymousAuthenticationToken

class GrailsAnonymousAuthenticationTokenSerializerTest extends AbstractKryoSerializerTest {
    void setup() {
        kryo.register(GrailsAnonymousAuthenticationToken, new GrailsAnonymousAuthenticationTokenSerializer())
    }

    void "serialize and deserialize"() {
        given:
        def token = new GrailsAnonymousAuthenticationToken('anonymous_key', createDetails())

        when:
        def token2 = serde(token)

        then:
        token.keyHash == token2.keyHash
        token.details == token2.details
    }
}
