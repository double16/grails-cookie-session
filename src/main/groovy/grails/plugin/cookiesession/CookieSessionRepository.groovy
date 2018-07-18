/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Patrick Double
 *  patrick.double@objectpartners.com or pat@patdouble.com
 */
package grails.plugin.cookiesession

import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j
import org.apache.commons.codec.binary.Base64InputStream
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.xerial.snappy.SnappyInputStream
import org.xerial.snappy.SnappyOutputStream

import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.servlet.ServletContext
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.security.SecureRandom
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * Implementation of a session repository using cookies to keep all of the session information in the client. It
 * delegates session serialization to a SessionSerializer instance and adds compression, encryption and base64 encoding
 * on top of the serialized session. This class also breaks the cookie value into chunks for storage in multiple
 * cookies.
 */
@Slf4j
@CompileStatic
class CookieSessionRepository implements SessionRepository, InitializingBean, ApplicationContextAware {
    public static final String DEFAULT_CRYPTO_ALGORITHM = 'Blowfish'
    public static final String SERVLET_CONTEXT_BEAN = 'servletContext'
    public static final String SETTING_USE_SESSION_COOKIE_CONFIG = 'useSessionCookieConfig'
    public static final String SETTING_SERIALIZER = 'serializer'
    public static final String JAVA_SERIALIZER = 'java'
    public static final String KRYO_SERIALIZER = 'kryo'
    public static final String JAVA_SESSION_SERIALIZER_BEAN_NAME = 'javaSessionSerializer'
    public static final String KRYO_SESSION_SERIALIZER_BEAN_NAME = 'kryoSessionSerializer'

    private static final float DEFAULT_WARN_THRESHOLD = 0.8f
    private static final String CRYPTO_SEPARATOR = '/'

    GrailsApplication grailsApplication
    ApplicationContext applicationContext

    SecretKey cryptoKey
    boolean encryptCookie = true
    String cryptoAlgorithm = DEFAULT_CRYPTO_ALGORITHM
    byte[] cryptoSecret = null

    Boolean useSnappy

    int maxInactiveInterval = 120

    int cookieCount = 5
    int maxCookieSize = 2048
    float warnThreshold = DEFAULT_WARN_THRESHOLD

    String cookieName
    Boolean secure
    Boolean httpOnly
    String path
    String domain
    String comment
    String serializer = JAVA_SERIALIZER
    Boolean useSessionCookieConfig
    Boolean useInitializationVector
    Boolean useGCMmode
    ServletContext servletContext

    Closure attributeSerializer = { Map<String, Serializable> attributes, OutputStream stream ->
        getSessionSerializer().serialize(attributes, stream)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    void configureCookieSessionRepository() {

        log.info 'configuring CookieSessionRepository'

        if (applicationContext.containsBean(SERVLET_CONTEXT_BEAN)) {
            servletContext = (ServletContext) applicationContext.getBean(SERVLET_CONTEXT_BEAN)
        }

        assignSettingFromConfig(SETTING_USE_SESSION_COOKIE_CONFIG, false, Boolean, SETTING_USE_SESSION_COOKIE_CONFIG)
        if (useSessionCookieConfig) {
            if (servletContext == null) {
                useSessionCookieConfig = false
                log.warn '{} was enabled in the config file, but has been disabled because the servlet context is not available.', SETTING_USE_SESSION_COOKIE_CONFIG
            }

            if (servletContext?.majorVersion < 3) {
                useSessionCookieConfig = false
                log.warn '{} was enabled in the config file, but has been disabled because the servlet does not support SessionCookieConfig.', SETTING_USE_SESSION_COOKIE_CONFIG
            }
        }

        if (useSessionCookieConfig) {
            interceptSessionCookieConfig(servletContext)
        }

        assignSettingFromConfig('encryptcookie', false, Boolean, 'encryptCookie')
        assignSettingFromConfig('cryptoalgorithm', DEFAULT_CRYPTO_ALGORITHM, String, 'cryptoAlgorithm')

        def cryptoSecretConfig = grailsApplication.config.grails.plugin.cookiesession.find { k, v -> k.equalsIgnoreCase('secret') }
        if (cryptoSecretConfig) {
            if (cryptoSecretConfig.value instanceof byte[]) {
                cryptoSecret = cryptoSecretConfig.value
                log.trace 'grails.plugin.cookiesession.secret set with byte[]'
            } else if (cryptoSecretConfig.value instanceof String) {
                cryptoSecret = cryptoSecretConfig.value.bytes
                log.trace 'grails.plugin.cookiesession.secret set with String.bytes'
            } else if (cryptoSecretConfig.value instanceof ArrayList) {
                cryptoSecret = cryptoSecretConfig.value as byte[]
                log.trace 'grails.plugin.cookiesession.secret set with ArrayList as byte[]'
            }
        }

        assignSettingFromConfig('cookiecount', 5, Integer, 'cookieCount')

        assignSettingFromConfig(SETTING_SERIALIZER, JAVA_SERIALIZER, String, SETTING_SERIALIZER)
        if (serializer == JAVA_SERIALIZER) {
            serializer = JAVA_SESSION_SERIALIZER_BEAN_NAME
        } else if (serializer == KRYO_SERIALIZER) {
            serializer = KRYO_SESSION_SERIALIZER_BEAN_NAME
        } else if (!(applicationContext.containsBean(serializer) && SessionSerializer.isAssignableFrom(applicationContext.getType(serializer)))) {
            log.error 'no valid serializer configured. defaulting to java'
            serializer = JAVA_SESSION_SERIALIZER_BEAN_NAME
        }

        assignSettingFromConfig('maxcookiesize', 2048, Integer, 'maxCookieSize')
        if (maxCookieSize < 1024 || maxCookieSize > 4096) {
            maxCookieSize = 2048
            log.info 'grails.plugin.cookiesession.maxCookieSize must be between 1024 and 4096. defaulting to 2048'
        } else {
            log.info 'grails.plugin.cookiesession.maxCookieSize set: {}', maxCookieSize
        }

        if (maxCookieSize * cookieCount > 6114) {
            log.warn 'the maxcookiesize and cookiecount settings will allow for a max session size of {} bytes. Make sure you increase the max http header size in order to support this configuration. see the help file for this plugin for instructions.', (maxCookieSize * cookieCount)
        }

        assignSettingFromConfig('cookiename', 'gsession', String, 'cookieName')
        assignSettingFromConfig('setsecure', false, Boolean, 'secure')
        assignSettingFromConfig('httponly', false, Boolean, 'httpOnly')
        assignSettingFromConfig('path', CRYPTO_SEPARATOR, String, 'path')
        assignSettingFromConfig('domain', null, String, 'domain')
        assignSettingFromConfig('comment', null, String, 'comment')
        assignSettingFromConfig('sessiontimeout', -1, Long, 'maxInactiveInterval')
        assignSettingFromConfig( 'warnthreshold', DEFAULT_WARN_THRESHOLD, Float, 'warnThreshold')

        if (useSessionCookieConfig) {
            this.cookieName = servletContext.sessionCookieConfig.name ?: cookieName
            this.httpOnly = servletContext.sessionCookieConfig.httpOnly ?: httpOnly
            this.secure = servletContext.sessionCookieConfig.secure ?: secure
            this.path = servletContext.sessionCookieConfig.path ?: path
            this.domain = servletContext.sessionCookieConfig.domain ?: domain
            this.comment = servletContext.sessionCookieConfig.comment ?: comment
            this.maxInactiveInterval = servletContext.sessionCookieConfig.maxAge ?: maxInactiveInterval
            log.trace "processed sessionCookieConfig. cookie settings are: [name: ${cookieName}, httpOnly: ${httpOnly}, secure: ${secure}, path: ${path}, domain: ${domain}, comment: ${comment}, maxAge: ${maxInactiveInterval}]"
        }

        if (grailsApplication.config.grails.plugin.cookiesession.containsKey('springsecuritycompatibility')) {
            log.info "grails.plugin.cookiesession.springsecuritycompatibility set: ${grailsApplication.config.grails.plugin.cookiesession['springsecuritycompatibility']}"
        } else {
            log.info 'grails.plugin.cookiesession.springsecuritycompatibility not set. defaulting to false'
        }

        warnForDeprecatedConfig()

        final String CIPHER = cryptoAlgorithm.split(CRYPTO_SEPARATOR)[0]
        final String CIPHER_MODE = cryptoAlgorithm.indexOf(CRYPTO_SEPARATOR) < 0 ? null : cryptoAlgorithm.split(CRYPTO_SEPARATOR)[1].toUpperCase()

        // initialize the crypto key
        if (cryptoSecret == null) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(CIPHER)
            SecureRandom secureRandom = new SecureRandom()
            keyGenerator.init(secureRandom)
            cryptoKey = keyGenerator.generateKey()
        } else {
            cryptoKey = new SecretKeySpec(cryptoSecret, CIPHER)
        }

        useInitializationVector = (CIPHER_MODE && CIPHER_MODE != 'ECB')
        useGCMmode = (CIPHER_MODE == 'GCM')

        if (grailsApplication.config.grails.plugin.springsecurity.useSessionFixationPrevention == true) {
            log.error 'grails.plugin.springsecurity.useSessionFixationPrevention == true. Spring Security Session Fixation Prevention is incompatible with cookie session plugin. Your application will experience unexpected behavior.'
        }

        checkForSnappy()
    }

    private void checkForSnappy() {
        try {
            SnappyOutputStream out = new SnappyOutputStream(new ByteArrayOutputStream())
            out.write('test'.getBytes())
            out.close()
            useSnappy = true
            log.info 'Using snappy compression'
        } finally {
            if (useSnappy == null) {
                useSnappy = false
                log.info 'Could not load snappy compression, using deflate'
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void warnForDeprecatedConfig() {
        if (grailsApplication.config.grails.plugin.cookiesession.containsKey('id')) {
            log.warn 'the grails.plugin.cookiesession.id setting is deprecated! Use the grails.plugin.cookiesession.cookiename setting instead!'
        }

        if (grailsApplication.config.grails.plugin.cookiesession.containsKey('timeout')) {
            log.warn 'the grails.plugin.cookiesession.timeout setting is deprecated! Use the grails.plugin.cookiesession.sessiontimeout setting instead!'
        }

        if (grailsApplication.config.grails.plugin.cookiesession.hmac.containsKey('secret')) {
            log.warn 'the grails.plugin.cookiesession.hmac.secret setting is deprecated! Use the grails.plugin.cookiesession.secret setting instead!'
        }

        if (grailsApplication.config.grails.plugin.cookiesession.hmac.containsKey('id')) {
            log.warn 'the grails.plugin.cookiesession.hmac.id setting is deprecated!'
        }

        if (grailsApplication.config.grails.plugin.cookiesession.hmac.containsKey('algorithm')) {
            log.warn 'the grails.plugin.cookiesession.hmac.algorithm is deprecated! Use the grails.plugin.cookiesession.cryptoalgorithm setting instead!'
        }
    }

    /**
     * if useSessionCookieConfig, then attach to invokeMethod and setProperty so that the values can be
     * intercepted and assigned to local variables
     * @param servletContext
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    private void interceptSessionCookieConfig(ServletContext servletContext) {
        servletContext.sessionCookieConfig.class.metaClass.invokeMethod = { String method, Object[] args ->
            switch (method) {
                case 'setName':
                    this.cookieName = args[0]
                    break
                case 'setHttpOnly':
                    this.httpOnly = args[0]
                    break
                case 'setSecure':
                    this.secure = args[0]
                    break
                case 'setPath':
                    this.path = args[0]
                    break
                case 'setDomain':
                    this.domain = args[0]
                    break
                case 'setComment':
                    this.comment = args[0]
                    break
                case 'setMaxAge':
                    this.maxInactiveInterval = args[0] as int
                    break
            }

            servletContext.sessionCookieConfig.metaClass.methods.find {
                it.name == method
            }?.invoke(servletContext.sessionCookieConfig, args)
            log.trace 'detected sessionCookieConfig.{} -> {}', method, args
        }

        servletContext.sessionCookieConfig.class.metaClass.setProperty = { String property, value ->
            switch (property) {
                case 'name':
                    this.cookieName = value
                    break
                case 'httpOnly':
                    this.httpOnly = value
                    break
                case 'secure':
                    this.secure = value
                    break
                case 'path':
                    this.path = value
                    break
                case 'domain':
                    this.domain = value
                    break
                case 'comment':
                    this.comment = value
                    break
                case 'maxAge':
                    this.maxInactiveInterval = value
                    break
            }

            servletContext.sessionCookieConfig.metaClass.properties.find {
                it.name == property
            }.setProperty(servletContext.sessionCookieConfig, value)
            log.trace 'detected sessionCookieConfig.{} -> {}', property, value
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private boolean assignSettingFromConfig(String settingName, defaultValue, Class t, String targetPropertyName) {
        boolean assignedSetting
        try {
            def configKey = grailsApplication.config.grails.plugin.cookiesession.find { k, v -> k.equalsIgnoreCase(settingName) }
            // targetPropertyName.toString() is necessary to keep Groovy from taking 'targetPropertyName' literally
            if (configKey) {
                this.(targetPropertyName.toString()) = configKey.value.asType(t)
                log.info "grails.plugin.cookiesession.${configKey.key} set: \'${this.(targetPropertyName.toString())}\'"
                assignedSetting = true
            } else {
                this.(targetPropertyName.toString()) = defaultValue
                log.info "configuring ${settingName} to default value: ${defaultValue}"
                return false
            }
        }
        catch (excp) {
            log.error "error configuring settting '${settingName}'", excp
            assignedSetting = false
        }

        return assignedSetting
    }

    void afterPropertiesSet() {
        log.trace 'afterPropertiesSet()'
        configureCookieSessionRepository()
    }

    SerializableSession restoreSession(HttpServletRequest request) {
        log.trace 'restoreSession()'

        SerializableSession session = null

        try {
            // - get the data from the cookie
            // - deserialize the session (handles compression and encryption)
            // - check to see if the session is expired
            // - return the session

            String serializedSession = getDataFromCookie(request)

            if (serializedSession) {
                session = deserializeSession(serializedSession, request)
            }

            long maxInactiveIntervalMillis = maxInactiveInterval * 1000
            long currentTime = System.currentTimeMillis()
            long lastAccessedTime = (session == null) ? 0 : session.lastAccessedTime
            long inactiveInterval = currentTime - lastAccessedTime

            if (session) {
                if (!session.isValid) {
                    log.info 'retrieved invalidated session from cookie. lastAccessedTime: {}.', new Date(lastAccessedTime)
                    session = null
                } else if (maxInactiveInterval == -1 || inactiveInterval <= maxInactiveIntervalMillis) {
                    log.info 'retrieved valid session from cookie. lastAccessedTime: {}', new Date(lastAccessedTime)
                    session.isNewSession = false
                    session.lastAccessedTime = System.currentTimeMillis()
                    session.servletContext = request.servletContext
                    session.dirty = false
                } else {
                    log.info 'retrieved expired session from cookie. lastAccessedTime: {}. expired by {} ms.', new Date(lastAccessedTime), inactiveInterval
                    session = null
                }
            } else {
                log.info 'no session retrieved from cookie.'
            }

        }
        catch (excp) {
            log.error 'An error occurred while restoring session from cookies. A null session will be returned and a new SerializableSession will be created.', excp
            session = null
        }

        return session
    }

    void saveSession(SerializableSession session, HttpServletResponse response) {
        log.trace 'saveSession()'

        if (session.isValid) {
            String serializedSession = serializeSession(session)

            float currentPercentage = serializedSession.length() / (maxCookieSize * cookieCount)
            if (currentPercentage >= warnThreshold) {
                log.warn 'cookie approaching maximum size. maxCookieSize: {}, currentSize: {}, percent: {}', maxCookieSize * cookieCount, serializedSession.length(), Math.round(currentPercentage * 100)
            }

            putDataInCookie(response, serializedSession)
        } else {
            deleteCookie(response)
        }
    }

    String serializeSession(SerializableSession session) {
        log.trace 'serializeSession()'

        log.trace 'getting sessionSerializer: {}', serializer
        SessionSerializer sessionSerializer = getSessionSerializer()

        final int maxSessionSize = maxCookieSize * cookieCount
        ByteArrayOutputStream result = new ByteArrayOutputStream(maxSessionSize)
        OutputStream stream = result

        Cipher cipher = null
        if (encryptCookie) {
            log.trace 'encrypting serialized session'
            cipher = Cipher.getInstance(cryptoAlgorithm)
            cipher.init(Cipher.ENCRYPT_MODE, cryptoKey)
            stream = new CipherOutputStream(stream, cipher)
        }

        if (useSnappy) {
            stream = new SnappyOutputStream(stream)
        } else {
            stream = new DeflaterOutputStream(stream, new Deflater(Deflater.BEST_SPEED), maxSessionSize)
        }

        log.trace 'serializing session'
        sessionSerializer.serialize(session, stream)
        stream.close()
        byte[] bytes = result.toByteArray()

        if (encryptCookie && useInitializationVector) {
            log.trace 'prepending cipher IV to serialized session'
            byte[] iv = cipher.IV
            byte[] output = new byte[1 + iv.length + bytes.length]
            output[0] = iv.length as byte
            System.arraycopy(iv, 0, output, 1, iv.length)
            System.arraycopy(bytes, 0, output, 1 + iv.length, bytes.length)
            bytes = output
        }

        log.trace 'base64 encoding serialized session from {} bytes', bytes.length
        String serializedSession = bytes.encodeBase64().toString()

        log.info 'serialized session: {} bytes', serializedSession.size()
        return serializedSession
    }

    SessionSerializer getSessionSerializer() {
        (SessionSerializer) applicationContext.getBean(serializer)
    }

    SerializableSession deserializeSession(String serializedSession, HttpServletRequest request) {
        log.trace 'deserializeSession()'

        SerializableSession session
        final int maxSessionSize = maxCookieSize * cookieCount

        try {
            log.trace 'decodeBase64 serialized session from {} bytes.', serializedSession.size()
            @SuppressWarnings('Deprecated') // we want only the lower 8-bits that StringBufferInputStream provides
            final InputStream input = new Base64InputStream(new StringBufferInputStream(serializedSession))
            InputStream stream = input

            if (encryptCookie) {
                log.trace 'decrypting serialized session from {} bytes.', serializedSession.size()
                Cipher cipher = Cipher.getInstance(cryptoAlgorithm)

                if (useInitializationVector) {
                    int ivLen = input.read()
                    byte[] ivBytes = new byte[ivLen]
                    input.read(ivBytes)
                    if (useGCMmode) {
                        GCMParameterSpec ivSpec = new GCMParameterSpec(128, ivBytes, 0, ivLen)
                        cipher.init(Cipher.DECRYPT_MODE, cryptoKey, ivSpec)
                    } else {
                        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes, 0, ivLen)
                        cipher.init(Cipher.DECRYPT_MODE, cryptoKey, ivSpec)
                    }
                } else {
                    cipher.init(Cipher.DECRYPT_MODE, cryptoKey)
                }

                stream = new CipherInputStream(input, cipher)
            }

            log.trace 'decompressing serialized session from {} bytes', serializedSession.size()
            if (useSnappy) {
                stream = new SnappyInputStream(stream)
            } else {
                stream = new InflaterInputStream(stream, new Inflater(), maxSessionSize)
            }

            SessionSerializer sessionSerializer = getSessionSerializer()
            session = sessionSerializer.deserialize(stream)
            session.isValid = true
        }
        catch (excp) {
            log.error 'An error occurred while deserializing a session.', excp
            if (log.isDebugEnabled()) {
                log.debug "Serialized-session: '$serializedSession'\n" +
                        "request-uri: ${request.requestURI}" +
                        request.headerNames.toList().inject('') { str, name ->
                            request.getHeaders(name).inject(str) { str2, val -> "$str2\n$name: $val" }
                        }
            }
            session = null
        }

        log.debug 'deserialized session: {}', (session != null)

        return session
    }

    protected String[] splitString(String input) {
        log.trace 'splitString()'

        String[] list = new String[cookieCount]

        if (!input) {
            log.trace 'input empty or null.'
            return list
        }

        int inputLength = input.size()

        int partitions = (int) Math.ceil((double) inputLength / maxCookieSize)
        log.trace 'splitting input of size {} string into {} partitions', input.size(), partitions

        for (int i = 0; i < partitions; i++) {
            int start = i * maxCookieSize
            int end = Math.min(start + maxCookieSize - 1, inputLength - 1)
            list[i] = input[start..end]
        }

        return list
    }

    String getDataFromCookie(HttpServletRequest request) {
        log.trace 'getDataFromCookie()'

        if (request.cookies == null) {
            return null
        }

        Collection<String> values = request.cookies.findAll { Cookie c ->
            c.name.startsWith(cookieName)
        }.sort { Cookie c ->
            c.name.substring(cookieName.length() + 1).toInteger()
        }.collect { Cookie c ->
            c.value
        }

        if (values == null) {
            return null
        }

        String data = values.join('')
        log.debug 'retrieved {} bytes of data from {} session cookies.', data.size(), values.size()

        return data
    }

    void putDataInCookie(HttpServletResponse response, String value) {
        log.trace 'putDataInCookie() - {}', value.size()

        // the cookie's maxAge will either be -1 or the number of seconds it should live for
        // - sessiontimeout config value overrides maxage

        if (value.length() > maxCookieSize * cookieCount) {
            log.error "Serialized session exceeds maximum session size that can be stored in cookies. Max size: ${maxCookieSize * cookieCount}, Requested Session Size: ${value.length()}."
            throw new MaxSessionSizeException("Serialized session exceeded max size, ${value.length()} > ${maxCookieSize * cookieCount}")
        }

        String[] partitions = splitString(value)
        partitions.eachWithIndex { String it, int i ->
            if (it) {
                Cookie c = createCookie(i, it, maxInactiveInterval)
                // if the value of the cookie will be empty, then delete it..
                response.addCookie(c)
                log.trace 'added session {}-{} to response', cookieName, i
            } else {
                // create a delete cookie
                Cookie c = createCookie(i, '', 0)
                response.addCookie(c)
                log.trace 'added delete {}-{} to response', cookieName, i
            }
        }

        log.debug 'added {} session cookies to response.', partitions.size()
    }

    void deleteCookie(HttpServletResponse response) {
        log.trace 'deleteCookie()'
        (0..cookieCount).eachWithIndex { it, i ->
            Cookie c = createCookie(i, '', 0)
            response.addCookie(c)
            log.trace 'added {}-{} to response with maxAge == 0', cookieName, i
        }
    }

    private Cookie createCookie(int i, String value, int m) {

        Cookie c = new Cookie("${cookieName}-${i}".toString(), value)

        c.maxAge = m // maxage overrides class scope variable
        c.secure = secure
        c.path = path

        if (servletContext?.majorVersion >= 3) {
            c.httpOnly = httpOnly
        }

        if (domain) {
            c.domain = domain
        }
        if (comment) {
            c.comment = comment
        }

        if (log.isTraceEnabled()) {
            if (servletContext?.majorVersion >= 3) {
                log.trace "created cookie name=${c.name}, maxAge=${c.maxAge}, secure=${c.secure}, path=${c.path}, httpOnly=${c.httpOnly}, domain=${c.domain}, comment=${c.comment}"
            } else {
                log.trace "created cookie name=${c.name}, maxAge=${c.maxAge}, secure=${c.secure}, path=${c.path}, domain=${c.domain}, comment=${c.comment}"
            }
        }

        return c
    }

    boolean isSessionIdValid(String sessionId) {
        log.trace 'isSessionIdValid() : {}', sessionId
        return true
    }
}
