package grails.plugin.cookiesession.kryo

import spock.lang.Unroll

import java.time.Instant

@Unroll
class StringValueOfSerializerTest extends AbstractKryoSerializerTest {

    void setup() {
        kryo.register(BigDecimal, new StringValueOfSerializer(BigDecimal))
        kryo.register(Instant, new StringValueOfSerializer(Instant, 'parse'))
    }

    void "BigDecimal #decimal"() {
        given:
        def input = new BigDecimal(decimal as double)

        when:
        def result = serde(input)
        then:
        result == input

        where:
        decimal | _
        1       | _
        2       | _
        1.2     | _
        1.3     | _
    }

    void "Instant #instant"() {
        given:
        def input = Instant.parse(instant)

        when:
        def result = serde(input)
        then:
        result == input

        where:
        instant                | _
        '2017-07-04T12:34:56Z' | _
    }

    void "serialize null"() {
        given:
        def serializer = new StringValueOfSerializer(Instant, 'parse')
        expect:
        serde(Instant, (Instant) null, serializer) == null
    }
}

