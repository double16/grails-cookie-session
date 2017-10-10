package grails.plugin.cookiesession.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import de.javakaffee.kryoserializers.ArraysAsListSerializer
import de.javakaffee.kryoserializers.JdkProxySerializer
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer
import org.objenesis.strategy.StdInstantiatorStrategy
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockServletContext
import org.springframework.security.web.authentication.WebAuthenticationDetails
import spock.lang.Specification

import java.lang.reflect.InvocationHandler

abstract class AbstractKryoSerializerTest extends Specification {
    Kryo kryo

    void setup() {
        kryo = new Kryo()
        kryo.instantiatorStrategy = new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy())
        kryo.fieldSerializerConfig.useAsm = true
        kryo.fieldSerializerConfig.optimizedGenerics = true

        kryo.register(Arrays.asList('').getClass(), new ArraysAsListSerializer())
        kryo.register(InvocationHandler.class, new JdkProxySerializer())
        UnmodifiableCollectionsSerializer.registerSerializers(kryo)
        SynchronizedCollectionsSerializer.registerSerializers(kryo)
    }

    def <T> T serde(T object) {
        ByteArrayOutputStream byteout = new ByteArrayOutputStream(4096)
        Output output = new Output(byteout)
        kryo.writeClassAndObject(output, object)
        output.close()

        ByteArrayInputStream bytein = new ByteArrayInputStream(byteout.toByteArray())
        Input input = new Input(bytein)
        T result = (T) kryo.readClassAndObject(input)
        input.close()
        return result
    }

    def <T> T serde(Class<T> clazz, T object, Serializer<T> serializer) {
        ByteArrayOutputStream byteout = new ByteArrayOutputStream(4096)
        Output output = new Output(byteout)
        serializer.write(kryo, output, object)
        output.close()

        ByteArrayInputStream bytein = new ByteArrayInputStream(byteout.toByteArray())
        Input input = new Input(bytein)
        T result = serializer.read(kryo, input, clazz)
        input.close()
        return result
    }

    WebAuthenticationDetails createDetails(String remoteAddress = '127.0.0.1', String sessionId = null) {
        MockHttpServletRequest request = new MockHttpServletRequest(remoteAddr: remoteAddress)
        if (sessionId != null) {
            request.setSession(new MockHttpSession(new MockServletContext(), sessionId))
        }
        new WebAuthenticationDetails(request)
    }
}
