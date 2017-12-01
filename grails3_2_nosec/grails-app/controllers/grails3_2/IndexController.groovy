/*
 * Copyright 2012-2017 the original author or authors.
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
package grails3_2

import grails.converters.JSON

class IndexController {

    def sessionRepository

    def assignToSession() {
        session."${params.key}" = params.val
        render model: [key: params.key, val: params.val], view: "assignToSession"
    }

    def dumpSession() {
        def s = session
        withFormat {
            html {
                render model: [sessionData: s.attributeNames.toList().collectEntries {
                    [it, s.getAttribute(it)]
                }], view: "dumpSession"
            }
            json {
                render s.attributeNames.toList().collect { [(it): s.getAttribute(it)] } as JSON
            }
        }
    }

    def assignToFlash() {
        flash."${params.key}" = params.val
        // reusing the assignToSession view on purpose
        render model: [key: params.key, val: params.val], view: "assignToSession"
    }

    def dumpFlash() {
        def f = flash
        withFormat {
            html {
                render model: [flashData: f.keySet().toList().collectEntries { [it, f.get(it)] }], view: "dumpFlash"
            }
            json {
                render f as JSON
            }
        }
    }

    def redirectTest() {
        flash.message = "this is a flash message"
        redirect action: 'redirectTarget'
    }

    def redirectTarget() {
    }

    def invalidateSession() {
        session.invalidate()
        render status: 200
    }

    def maxRecurseCount = 0
    def recurseCount = 0

    private def recurse() {
        recurseCount++
        if (recurseCount == maxRecurseCount)
            throw new Exception("exception from recursive method: ${recurseCount}")
        else
            recurse()
    }

    def storeLargeException() {
        try {
            maxRecurseCount = 1000
            recurse()
        }
        catch (excp) {
            session.lastError = excp
        }

        def stackTrace = session.lastError.stackTrace.collect { it.toString() }
        render text: "stored exception: ${stackTrace}"
    }

    def throwException() {
        throw new Exception("this is an exception")
    }

    def reauthenticate() {
        println "Session before reauthenticate: ${session}"
        springSecurityService.reauthenticate "testuser", "password"
        println "Session after reauthenticate: ${session}"
        render "reauthenticated"
    }

    def assignSecurityContextNull() {
        session.SPRING_SECURITY_CONTEXT = "test"
        render text: "assigned null to SPRING_SECURITY_CONTEXT"
    }

    def sessionExists() {
        render text: "<html><body>${request.getSession(false) != null}</body></html>"
    }

    def configureSessionRepository() {
        if (params.maxInactiveInterval) {
            sessionRepository.maxInactiveInterval = params.maxInactiveInterval as int
        }
        if (params.cookieCount) {
            sessionRepository.cookieCount = params.cookieCount as int
        }
        if (params.maxCookieSize) {
            sessionRepository.maxCookieSize = params.maxCookieSize as int
        }
        if (params.cookieName) {
            sessionRepository.cookieName = params.cookieName
        }

        [maxInactiveInterval: sessionRepository.maxInactiveInterval,
         cookieCount        : sessionRepository.cookieCount,
         maxCookieSize      : sessionRepository.maxCookieSize,
         cookieName         : sessionRepository.cookieName]
    }
}
