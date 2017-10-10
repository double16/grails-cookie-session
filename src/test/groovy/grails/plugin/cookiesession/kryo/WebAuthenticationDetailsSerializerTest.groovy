package grails.plugin.cookiesession.kryo

import org.springframework.security.web.authentication.WebAuthenticationDetails
import spock.lang.Unroll

@Unroll
class WebAuthenticationDetailsSerializerTest extends AbstractKryoSerializerTest {
    void setup() {
        kryo.register(WebAuthenticationDetails, new WebAuthenticationDetailsSerializer())
    }

    void "serialize and deserialize"() {
        given:
        def details = createDetails(remoteAddress, sessionId)

        when:
        def result = serde(details)
        then:
        result.remoteAddress == remoteAddress
        result.sessionId == sessionId

        where:
        remoteAddress     | sessionId
        '169.254.169.254' | null
        '169.254.169.254' | 'simplesession'
    }
}
