package grails.plugin.cookiesession

import javax.servlet.http.HttpSession

trait SessionFixtureBase {
    /** The desired maximum session size. */
    int maxSessionSize() { /*cookies*/ 5 * /*bytes per cookie*/ 2048 }

    SerializableSession emptySession() {
        new SerializableSession()
    }

    boolean equals(HttpSession session1, HttpSession session2) {
        Set<String> attributeNames = session1.attributeNames.toSet()
        if (!attributeNames.equals(session2.attributeNames.toSet())) {
            return false
        }
        attributeNames.find { session1.getAttribute(it) != session2.getAttribute(it) } == null
    }
}
