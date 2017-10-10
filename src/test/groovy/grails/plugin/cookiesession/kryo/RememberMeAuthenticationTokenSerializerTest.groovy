package grails.plugin.cookiesession.kryo

import org.springframework.security.authentication.RememberMeAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class RememberMeAuthenticationTokenSerializerTest extends AbstractKryoSerializerTest {
    Collection<? extends GrantedAuthority> authorities
    GrailsUserSerializerTest.TestGrailsUser user

    void setup() {
        kryo.register(RememberMeAuthenticationToken, new RememberMeAuthenticationTokenSerializer(RememberMeAuthenticationToken))
        authorities = [new GrantedAuthoritySerializerTest.SimpleAuthority('ROLE_USER')]
        user = new GrailsUserSerializerTest.TestGrailsUser('user1', 'pass1', true, true, true, true, authorities, 123456L)
    }

    void "serialize and deserialize"() {
        given:
        def token = new RememberMeAuthenticationToken('temporary', user, authorities)
        token.details = createDetails()

        when:
        def token2 = serde(token)
        then:
        token2.keyHash == token.keyHash
        token2.principal == token.principal
        token2.details == token.details
        token2.authorities == token.authorities
    }
}
