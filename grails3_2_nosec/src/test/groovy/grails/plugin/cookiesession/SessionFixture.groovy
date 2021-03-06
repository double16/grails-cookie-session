package grails.plugin.cookiesession

import org.grails.web.servlet.GrailsFlashScope
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession

/**
 * Helpers for creating and comparing sessions in tests.
 */
trait SessionFixture extends SessionFixtureBase {
    SerializableSession flashScopeSession() {
        GrailsFlashScope scope = new GrailsFlashScope()
        scope['flash_key1'] = 'string1'
        scope['flash_key2'] = ['e1','e2']
        def session = emptySession()
        session.setAttribute(GrailsApplicationAttributes.FLASH_SCOPE, scope)
        session
    }

    SerializableSession anonymousAuthenticatedSession() {
        emptySession()
    }

    SerializableSession authenticatedSession() {
        emptySession()
    }

    SerializableSession rememberMeSession() {
        emptySession()
    }

    SerializableSession preauthWithSavedRequestSession() {
        emptySession()
    }

}
