package grails.plugin.cookiesession.kryo

import spock.lang.Unroll

@Unroll
class ByteArrayValueOfSerializerTest extends AbstractKryoSerializerTest {

    void setup() {
        kryo.register(BigInteger, new ByteArrayValueOfSerializer(BigInteger))
    }

    void "BigInteger #value"() {
        given:
        def input = new BigInteger(value as int)

        when:
        def result = serde(input)
        then:
        result == input

        where:
        value | _
        1     | _
        2     | _
        1.2   | _
        1.3   | _
    }

    void "serialize null"() {
        given:
        def serializer = new ByteArrayValueOfSerializer(BigInteger)
        expect:
        serde(BigInteger, (BigInteger) null, serializer) == null
    }
}
