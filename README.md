# Cookie Session Grails Plugin

[ ![Download](https://api.bintray.com/packages/double16/plugins/cookie-session/images/download.svg) ](https://bintray.com/double16/plugins/cookie-session/_latestVersion) [![CircleCI](https://circleci.com/gh/double16/grails-cookie-session.svg?style=svg&circle-token=aa34d542a6c31d626c75fa8d946858a32132c63e)](https://circleci.com/gh/double16/grails-cookie-session)

The Cookie Session plugin enables grails applications to store session data in http cookies between requests instead of in memory on the server. Client sessions are transmitted from the browser to the application with each request and transmitted back with each response. This allows application deployments to be more stateless. Benefits of managing sessions this way include:

## Simplified Scaling

Because a client's session is passed with every request, the deployment architecture need not be concerned with scaling strategies that account for sessions stored on the server, such as session replication or sticky sessions. Simply add application instances and route requests to them. Also, because the session data is stored with client, the server doesn't expend memory or disk space storing sessions that are open for long periods of time.

## Fault Tolerance

When sessions are stored in memory on the server, if an application crashes or becomes inaccessible, clients' sessions are usually lost which can result in unexpected logout, redirects, or loss of data. When sessions are stored cookies,the applications can be much more tolerant to server-side commission failures. In a single-instance deployment scenario, the server or application can be recycled and clients can continue working when the application becomes available and with their session fully intact. In a multi-instance deployment scenario, any instance of the application can service a clients request. A beneficial side effect of cookie-sessions is that applications can be upgraded or restarted without logging out users.

* Known incompatibility
  - spring webflow

# Installation

Edit `build.gradle` add the following line under the plugins closure
```groovy
  compile 'org.grails.plugins:cookie-session:4.0.2'
```

# Configuration

The following parameters are supported directly by the cookie-session plugin. Note, additional configuration is needed for large session support. See additional instructions below.

## Parameters

| name | default | description |
| ---- | ------- | ----------- |
| grails.plugin.cookiesession.enabled                     | false          | enables or disables the cookie session. |
| grails.plugin.cookiesession.encryptcookie               | true           | enable or disable encrypting session data stored in cookies. |
| grails.plugin.cookiesession.cryptoalgorithm             | Blowfish       | The cryptographic algorithm used to encrypt session data (i.e. Blowfish, DES, DESEde, AES). NOTE: the secret must be compatible with the crypto algorithm. Version 2.0.12 supports non-ECB cipher modes, such as 'Blowfish/CBC/PKCS5Padding', that require an initialization vector |
| grails.plugin.cookiesession.secret                      | **generated**  | The secret key used to encrypt session data. If not set, a random key will be created at runtime. Set this parameter if deploying multiple instances of the application or if sessions need to survive a server crash or restart. sessions to be recovered after a server crash or restart. |
| grails.plugin.cookiesession.cookiecount                 | 5              | The maximum number of cookies that are created to store the session in |
| grails.plugin.cookiesession.maxcookiesize               | 2048           | The max size for each cookie expressed in bytes. |
| grails.plugin.cookiesession.warnthreshold               | 0.8            | Threshold for logging a warning when the cookie size becomes equal to or greater than the total maximum cookie size (cookieCount * maxCookieSize) |
| grails.plugin.cookiesession.sessiontimeout              | 0              | The length of time a session can be inactive for expressed in seconds. -1 indicates that a session will be active for as long as the browser is open. |
| grails.plugin.cookiesession.cookiename                  | gsession-X     | X number of cookies will be written per the cookiecount parameter. Each cookie is suffixed with the integer index of the cookie. |
| grails.plugin.cookiesession.condenseexceptions          | false          | replaces instances of Exceptions objects in the session with the Exception.getMessage() in the session (see SessionPersistanceListener for further details) |
| grails.plugin.cookiesession.serializer                  | 'java'         | specify serializer used to serialize session objects. valid values are: 'java', 'kryo', or the name of a spring bean that implement SessionSerializer. See section on Serializers below. | 
| grails.plugin.cookiesession.usesessioncookieconfig      | false          | version 2.0.12+ uses the ServletContext.SessionCookieConfig to configure cookies used to store the session. values from SessionCookieConfig override config parameters setsecure, sethttp, path, domain, comment, sessiontimeout, and cookiename. See notes below on use of this config option. |
| grails.plugin.cookiesession.springsecuritycompatibility | false          | true to configure enhanced compatibility with spring security, false to disable. |
| grails.plugin.cookiesession.setsecure                   | false          | calls Cookie.setSecure on cookie-session cookies. This flag indicates to browsers whether cookies should only be sent over secure connections. |
| grails.plugin.cookiesession.httponly                    | false          | calls Cookie.setHttpOnly on cookie-session cookies. This flag indicates to browsers that the cookie should not be made available to scripts. |
| grails.plugin.cookiesession.path                        | /              | calls Cookie.setPath on cookie-session cookies. This limits the paths for which the browser should send the cookie. |
| grails.plugin.cookiesession.domain                      | -unset-        | calls Cookie.setDomain on cookie-session cookies if set. This tells the browsers which domains the cookie is valid for; if unset, then the cookie is valid for the current host only. |
| grails.plugin.cookiesession.comment                     | -unset-        | calls Cookie.setComment on cookie-session cookies. |
| grails.plugin.cookiesession.id                          | **deprecated** | deprecated. use the 'grails.plugin.cookiesession.cookiename' setting. |
| grails.plugin.cookiesession.timeout                     | **deprecated** | deprecated. use the 'grails.plugin.cookiesession.sessiontimeout' setting. |
| grails.plugin.cookiesession.hmac.secret                 | **deprecated** | deprecated. use the 'grails.plugin.cookiesession.secret' setting. |
| grails.plugin.cookiesession.hmac.id                     | **deprecated** | deprecated. no equivalent setting is present in this version of the plugin. |
| grails.plugin.cookiesession.hmac.algorithm              | **deprecated** | deprecated. use the 'grails.plugin.cookiesession.cryptoalgorithm' settings. |

## Example

application.yml
```yaml
grails:
  plugin:
    cookiesession:
      enabled: true
      encryptcookie: true
      cryptoalgorithm: "Blowfish"
      secret: "This is my secret."
      cookiecount: 10
      maxcookiesize: 2048  // 2kb
      sessiontimeout: 3600 // one hour
      cookiename: 'gsession'
      condenseexceptions: false
      setsecure: true
      httponly: false
      path: '/'
      comment: 'Acme Session Info'
      serializer: 'kryo'
      springsecuritycompatibility: true
```

## Understanding cookiecount and maxcookiesize

The maximum session size stored by this plugin is calculated by (cookiecount * maxcookiesize). The reason for these two parameters is that through experimentation, some browsers didn't reliably set large cookies set before the subsequent request. To solve this issue, this plugin supports configuring the max size of each cookie stored and the number of cookies to span the session over. The default values are conservative. If sessions exceed the max session size as configured, first increase the cookiecount and then the maxcookiesize parameters.

## Enabling large session
To enable large sessions, increase the max http header size for the servlet container you are using. 

Due to the potentially large amount of data that may be stored, consider setting it to something large, such as 262144 ( 256kb ).

*the following are for grails 2.x, needs to be updated for 3.x - investigating*

### Tomcat
Edit the server.xml and set the connector's maxHttpHeaderSize parameter. 

When developing in grails, configure the embedded tomcat server with the tomcat configuration event:

1.  create the file scripts/_Events.groovy in your project directory
2.  add the following code:

        eventConfigureTomcat = {tomcat ->
          tomcat.connector.setAttribute("maxHttpHeaderSize",262144)
        }

### Jetty (2.0.5+)
Edit the jetty.xml or web.xml and set the connector's requestHeaderSize and responseHeaderSize parameters.

1.  create the file scripts/_Events.groovy in your project directory
2.  add the following code:

        eventConfigureJetty = {jetty ->
          jetty.connectors[0].requestHeaderSize = 262144
          jetty.connectors[0].responseHeaderSize = 262144
        }

## SessionPersistenceListener

SessionPersistenceListener is an interface used inspect the session just after its been deserialized from persistent storage and just before being serialized and persisted. 

SessionPersistenceListener defines the following methods:
```groovy
    void afterSessionRestored( SerializableSession session )
    void beforeSessionSaved( SerializableSession session )
```

To use, write a class that implements this interface and define the object in the application's spring application context (grails-app/conf/spring/resources.groovy). The CookieSession plugin will scan the application context and retrieve references to all classes that implement SessionPersistenceListener. The order that the SessionPersistenceListeners are called is unspecified. For an example of how to implement a SessionPersistenceListener, see the ExceptionCondenser class which is part of the cookie-session plugin.

The ExceptionCondenser uses beforeSessionSaved() to replace instances of Exceptions the exception's message. This is useful because some libraries, notably the spring-security, store exceptions in the session, which can cause the cookie-session storage to overflow. The ExceptionCondenser can be installed by either adding it in the application context or by enabling it with the convenience settings `grails.plugin.cookiesession.condenseexceptions = true`.

## Configuring Serialization
The grails.plugin.cookiesession.serializer config setting is used to pick which serializer the cookie-session plugin will use to serialize sessions. Currently, only two options are supported: 'java' and 'kryo'. 'java' is used to pick the java.io API serializer. This serializer has proven to be reliable and works 'out of the box'. 'kryo' is used to pick the Kryo serializer (https://github.com/EsotericSoftware/kryo). The Kryo serializer has many benefits over the Java serializer, primarily serialized results are significantly smaller which reduces the size of the session cookies. However, the Kryo serializer requires configuration to work correctly with some grails and spring objects. By default the kryo serializer is configured to serialize GrailsFlashScope and other basic grails objects. If the application uses spring-security, you must enable `springsecuritycompatibility` for the cookie-session plugin. Additionally you should verify that the serializer is successfully serializing all objects that will be stored in the session. Configure info level logging for `grails.plugins.cookiesession.CookieSessionRepository` for test and development environments to monitor the serialization and deserialization process. If objects fail to serialize, please report an issue to this github project; a best effort will be made to make the kryo serializer as compatible as possible. If the kryo serializer doesn't work for your application, consider falling back to the java serializer or implementing your own SessionSerializer as described below.

## Spring Security Compatibility
Spring Security Compatibility, configured with the `springsecuritycompatibility` setting, directs the cookie-session plugin to adjust its behavior to be more compatible with the spring-security-core plugin. 

The primary issue addressed in this mode relates to when the spring-security core's SecurityContextPersistenceFilter writes the current security context to the SecurityContextRepository. In most cases, the SecurityContextPersistenceFilter stores the current security context after the current web response has been written. This is a problem for the cookie-session plugin because the session is stored in cookies in the web response. As a result, the current security context is never saved in the session, in effect losing the security context after each request. To work around this issue, spring security compatibility mode causes the cookie-session plugin to write the current security context to the session just before the session is serialized and saved in cookies. The security context is stored under the key that the SecurityContextRepository expects to find the security context. 

The next issue that Spring Security Compatibility addresses involves cookies saved in the DefaultSavedRequest. DefaultSavedRequest is created by spring security core and stored in the session during redirects, such as after authentication. Spring Security Compatibility causes the cookie-session plugin to detect the presence of a DefaultSavedRequest in the session and remove any cookie-session cookies it may be storing. This ensures that old session information doesn't replace more current session information when following a redirect. This also reduces the size of the the serialized session because the DefaultSavedRequest is storing an old copy of a session in the current session.

Finally, Spring Security Compatibility adds custom kryo serializers (when kryo serialization is enabled) to successfully serialize objects that kryo isn't capable of serializing by default.

When compatibility with Spring Security is enabled the plugin may enforce new session creation if none exists yet. The reason is that without a session present the Security Context would not be persisted and propagated between requests. This would manifest as an intermittent issue depending whether or not the application uses session for some other purpose or not (e.g. flash scope).

## use of SessionCookieConfig
SessionCookieConfig is a interface introduced in Servlet 3.0 and is used to specify configuration parameters of session cookies. If you enable the `grails.plugin.cookiesession.usesessioncookieconfig` parameter, then this interface is used to configure the cookie session cookies. In order for this option to work, the servlet context must be servlet context version 3.0 or great. 

The following is an example of how to use the SessionCookieConfig to configure cookies in the init closure in BootStrap.groovy.
```groovy
        if( servletContext.majorVersion >= 3 ){
          servletContext.sessionCookieConfig.name = 'sugar2'
          servletContext.sessionCookieConfig.secure = false
          servletContext.sessionCookieConfig.maxAge = 3600
        }
```

## WARNING on updating cookie specifications and general recommendations
Be warned, updating the cookie specification can cause unexpected results and cause your application to fail. In general, you should not update the cookie specification once an application is in production. If you do, multiple cookies with the same name will be saved back to the browser and will be sent back to your application, which will undoubtedly cause deserialization of the session cookie to fail. Here are some general recommendations on how to configure cookie session cookie, either with the plugin's configuration options or with the SessionCookieConfig:

  *   Always configure a reasonable timeout so that in the event that duplicate cookies are created, they'll be cleared from the browser automatically
  *   Use a unique cookie name for each application. If you run multiple applications under the same domain, this will ensure that cookies between applications don't conflict with one-another, unless you intend to share sessions between applications. In that case, make sure cookies are configured EXACTLY the same.
  *   Avoid changing the cookie configuration after an application is production. Be aware that any browser storing cookie session cookies that haven't expired will like get duplicate cookie names and experience errors the next time they access the site. This can be avoided by having a short session timeout or by renaming the cookie, in which case ALL existing sessions will be lost.

## Logging

The following logback keys are configurable:

  *   grails.plugin.cookiesession.CookieSessionFilter
  *   grails.plugin.cookiesession.SessionRepositoryRequestWrapper
  *   grails.plugin.cookiesession.SessionRepositoryResponseWrapper
  *   grails.plugin.cookiesession.CookieSessionRepository
  *   grails.plugin.cookiesession.JavaSessionSerializer
  *   grails.plugin.cookiesession.KryoSessionSerializer
  *   grails.plugin.cookiesession.SecurityContextSessionPersistenceListener
  *   grails.plugin.cookiesession.SerializableSession

## Configured Beans

  *   cookieSessionFilter - the plugin filter
  *   sessionRepository - an implementation of SessionRepository
  *   javaSessionSerializer or kryoSessionSerializer - serializer configured with 'serializer' config setting.

## How this plugin works

  This plugin consists of the following components:
  
  *   CookieSessionFilter - a servlet filter installed in the first position of the filter chain which is responsible for wrapping the request and response object with the SessionRepositoryRequestWrapper and SessionRepositoryResponseWrapper objects.
  *   SessionRepositoryRequestWrapper - an implementation of HttpServletRequestWrapper which delegates responsibility for retrieving session data from persistence storage to an instance of a SessionRepository object and for managing an instance of SerializableSession.
  *   SessionRepositoryResponseWrapper - an implementation of HttpServletResponseWrapper which delegates saving sessions to persistent storage using an instance of SessionRepository.
  *   SerializableSession - an implementation of HttpSession that can be serialized 
  *   SessionRepository - an interface that describes a class that can save and restore a session from a persistent location
  *   CookieSessionRepository - an implementation of SerializableSession that is responsible for the mechanics of storing session data in cookies and retrieve session data from cookies.

### Execution sequence outline

When a request is received by the server, the CookieSessionFilter is called in the filter chain and performs the following:

  1.    retrieves an instance of a SessionRepository bean from the application context 
  2.    creates an instance of the SessionRepositoryRequestWrapper, assigning it the SessionRepository instance and the current request
  3.    uses SessionRepositoryRequestWrapper instance to restore the session
  4.    uses the SessionRepositoryInstance to get the current session
  5.    creates an instance of the SessionRepositoryResponseWrapper, assigning it the current session, the SessionRepository instance and the current response object.
  6.    calls the next filter in the chain

Throughout the remainder of the request, the SessionRepositoryRequestWrapper is only responsible for returning the stored instances of the SerializableSession.

As the request processing comes to a conclusion the SessionRepositoryResponseWrapper is used to intercept calls that would cause the response to be committed (i.e. written back to the client). When it intercepts these calls, it uses a SessionRepository object to persist the Session object.

The CookieSession object is a spring bean that implements the SessionRepository interface. This object is injected injected into the application so that it can be replaced with alternative implementations that can store the session to different locations such as database, shared in-memory store, shared filesystem, etc.

## How to contribute
If you want to contribute a bug fix, please work from the 'develop' branch. Additionally, before submitting a pull request please confirm that all of the tests in test suite pass. The test suite can be run using `gradlew check testAll integrationTestAll`

