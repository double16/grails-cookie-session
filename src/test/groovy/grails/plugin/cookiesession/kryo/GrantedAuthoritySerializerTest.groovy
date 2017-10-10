package grails.plugin.cookiesession.kryo

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.springframework.security.core.GrantedAuthority
import spock.lang.Unroll

@Unroll
class GrantedAuthoritySerializerTest extends AbstractKryoSerializerTest {
    @EqualsAndHashCode
    @ToString
    static class SimpleAuthority implements GrantedAuthority {
        String authority

        SimpleAuthority(String authority) {
            this.authority = authority
        }
    }

    void setup() {
        kryo.register(SimpleAuthority, new GrantedAuthoritySerializer(SimpleAuthority))
    }

    void "serialize and deserialize"() {
        given:
        def input = new SimpleAuthority(authorityStr)

        when:
        def result = serde(input)
        then:
        result.authority == authorityStr

        where:
        authorityStr | _
        'ROLE_ADMIN' | _
        'ROLE_USER'  | _
        ''           | _
    }
}
