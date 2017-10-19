package grails.plugin.cookiesession

import grails.plugin.springsecurity.authentication.GrailsAnonymousAuthenticationToken
import grails.plugin.springsecurity.userdetails.GrailsUser
import org.grails.web.servlet.GrailsFlashScope
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.authentication.RememberMeAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.security.web.savedrequest.DefaultSavedRequest

/**
 * Helpers for creating and comparing sessions in tests.
 */
trait SessionFixture extends SessionFixtureBase {
    WebAuthenticationDetails createDetails(String remoteAddress, String sessionId = null) {
        def request = new MockHttpServletRequest()
        request.setRemoteAddr(remoteAddress)
        if (sessionId) {
            def session = new MockHttpSession(null, sessionId)
            request.setSession(session)
        }
        new WebAuthenticationDetails(request)
    }

    SerializableSession flashScopeSession() {
        GrailsFlashScope scope = new GrailsFlashScope()
        scope['flash_key1'] = 'string1'
        scope['flash_key2'] = ['e1','e2']
        def session = emptySession()
        session.setAttribute(GrailsApplicationAttributes.FLASH_SCOPE, scope)
        session
    }

    SerializableSession anonymousAuthenticatedSession() {
        SerializableSession session = flashScopeSession()
        SecurityContextImpl securityContext = new SecurityContextImpl()
        def details = createDetails('0:0:0:0:0:0:0:1')
        GrailsAnonymousAuthenticationToken authentication = new GrailsAnonymousAuthenticationToken('key', details)
        securityContext.setAuthentication(authentication)
        session.setAttribute('SPRING_SECURITY_CONTEXT', securityContext)
        session
    }

    SerializableSession authenticatedSession() {
        SerializableSession session = preauthWithSavedRequestSession()

        SecurityContextImpl securityContext = new SecurityContextImpl()
        Collection<GrantedAuthority> authorities = Collections.unmodifiableSet([new SimpleGrantedAuthority('ROLE_ADMIN')] as Set)
        GrailsUser principal = new GrailsUser('admin', 'password', true, true, true, true, authorities, 1)
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities)
        authentication.setDetails(createDetails('0:0:0:0:0:0:0:1', 'simplesession'))
        securityContext.setAuthentication(authentication)
        session.setAttribute('SPRING_SECURITY_CONTEXT', securityContext)

        session
    }

    SerializableSession rememberMeSession() {
        SerializableSession session = preauthWithSavedRequestSession()

        SecurityContextImpl securityContext = new SecurityContextImpl()
        Collection<GrantedAuthority> authorities = Collections.unmodifiableSet([new SimpleGrantedAuthority('ROLE_ADMIN')] as Set)
        GrailsUser principal = new GrailsUser('admin', '', true, true, true, true, Collections.emptySet(), 1)
        RememberMeAuthenticationToken authentication = new RememberMeAuthenticationToken('key', principal, authorities)
        authentication.setAuthenticated(true)
        authentication.setDetails(createDetails('0:0:0:0:0:0:0:1', 'simplesession'))
        securityContext.setAuthentication(authentication)
        session.setAttribute('SPRING_SECURITY_CONTEXT', securityContext)

        session
    }

    SerializableSession preauthWithSavedRequestSession() {
        SerializableSession session = flashScopeSession()
        SecurityContextImpl securityContext = new SecurityContextImpl()
        session.setAttribute('SPRING_SECURITY_CONTEXT', securityContext)

        // request constructed from Chrome 60.0
        def request = new MockHttpServletRequest()
        request.setPreferredLocales([Locale.US, Locale.ENGLISH])
        request.setContextPath('')
        request.setMethod('GET')
        request.setRequestURI('/secure')
        request.setScheme('http')
        request.setServerName('localhost')
        request.setServletPath('/secure')
        request.setServerPort(18032)
        request.addHeader('accept', 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8')
        request.addHeader('accept-encoding', 'gzip, deflate, br')
        request.addHeader('accept-language', 'en-US,en;q=0.8')
        request.addHeader('connection', 'keep-alive')
        request.addHeader('cookie', 'gsession-0=zLXKx/rQyntq+5H/20yBymypimpUXLOypBE8sP2pUTIKNvwnbqh+dvxxvrfx5SqlM/CAyQPMuVyAAjZ9qgX/pF1PZDEkMpF2r+y6DxQ4BTiy00mmRr0YJmDFZWvbnhEJ1kYqIf1GUAMIW9GckKAKj0BjPGJhdEIxOaElnTCPxO3hYirUkmL44EPyh7wgpZ9Ycif3GtjMRITWB/GHOyI/Qw==')
        request.addHeader('host', 'localhost:18032')
        request.addHeader('upgrade-insecure-requests', '1')
        request.addHeader('user-agent', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36')

        DefaultSavedRequest savedRequest = new DefaultSavedRequest(request, { 18032 })
        session.setAttribute('SPRING_SECURITY_SAVED_REQUEST', savedRequest)

        session
    }

}
