package grails.plugin.cookiesession

import org.grails.web.servlet.GrailsFlashScope
import org.grails.web.util.GrailsApplicationAttributes

/**
 * Helpers for creating and comparing sessions in tests.
 */
trait SessionFixture extends SessionFixtureBase {
    @Override
    SerializableSession flashScopeSession() {
        GrailsFlashScope scope = new GrailsFlashScope()
        scope['flash_key1'] = 'string1'
        scope['flash_key2'] = ['e1','e2']
        def session = emptySession()
        session.setAttribute(GrailsApplicationAttributes.FLASH_SCOPE, scope)
        session
    }

    @Override
    SerializableSession authenticatedSession() {
        throw new UnsupportedOperationException()
    }

    @Override
    SerializableSession preauthWithSavedRequestSession() {
        throw new UnsupportedOperationException()
    }

    @Override
    SerializableSession anonymousAuthenticatedSession() {
        throw new UnsupportedOperationException()
    }
}
