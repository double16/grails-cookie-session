package grails.plugin.cookiesession.kryo

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import spock.lang.Unroll

@Unroll
class UserSerializerTest extends AbstractKryoSerializerTest {
    @EqualsAndHashCode(excludes = ['authorities', 'password'])
    @ToString(includeNames = true)
    static class TestUser {
        String password
        String username
        Set authorities = new HashSet()
        boolean accountNonExpired
        boolean accountNonLocked
        boolean credentialsNonExpired
        boolean enabled

        TestUser(String username, String password, boolean enabled,
                 boolean accountNonExpired, boolean credentialsNonExpired,
                 boolean accountNonLocked, Collection authorities) {
            this.username = username
            this.password = password
            this.enabled = enabled
            this.accountNonExpired = accountNonExpired
            this.credentialsNonExpired = credentialsNonExpired
            this.accountNonLocked = accountNonLocked
            this.authorities.addAll(authorities)
        }
    }

    void setup() {
        kryo.register(TestUser, new UserSerializer(TestUser))
    }

    void "serialize and deserialize"() {
        given:
        def input = new TestUser(username, password, enabled,
                accountNonExpired, credentialsNonExpired,
                accountNonLocked, [])

        when:
        def result = serde(input)
        then:
        result == input

        where:
        username | password | enabled | accountNonExpired | credentialsNonExpired | accountNonLocked
        'user1'  | 'pass1'  | true    | true              | true                  | true
        'user1'  | 'pass1'  | false   | true              | true                  | true
        'user1'  | 'pass1'  | true    | false             | true                  | true
        'user1'  | 'pass1'  | true    | true              | false                 | true
        'user1'  | 'pass1'  | true    | true              | true                  | false
        'user1'  | ''       | false   | false             | false                 | false
    }

    void "skip authorities property"() {
        given:
        def input = new TestUser('user1', 'pass1', true, true, true, true, ['ROLE_ADMIN'])

        when:
        def result = serde(input)
        then:
        result.authorities.empty
    }

    void "skip password property"() {
        given:
        def input = new TestUser('user1', 'pass1', true, true, true, true, ['ROLE_ADMIN'])

        when:
        def result = serde(input)
        then:
        result.password.empty
    }
}
