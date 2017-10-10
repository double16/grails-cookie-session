package grails.plugin.cookiesession.kryo

import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import org.grails.web.servlet.GrailsFlashScope

@TestMixin(ControllerUnitTestMixin)
class GrailsFlashScopeSerializerTest extends AbstractKryoSerializerTest {
    void setup() {
        kryo.register(GrailsFlashScope, new GrailsFlashScopeSerializer())
    }

    void "serialize and deserialize"() {
        given:
        GrailsFlashScope flashScope = new GrailsFlashScope()

        when:
        flashScope.put('attr1', 'value1')
        flashScope.put('attr2', ['value2a', 'value2b'])
        GrailsFlashScope serde1 = serde(flashScope)
        then:
        serde1.get('attr1') == 'value1'
        serde1.get('attr2') == ['value2a', 'value2b']

        when:
        serde1.next()
        then:
        serde1.get('attr1') == 'value1'
        serde1.get('attr2') == ['value2a', 'value2b']

        when:
        serde1.next()
        then:
        serde1.get('attr1') == null
        serde1.get('attr2') == null
    }
}
