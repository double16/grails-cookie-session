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

grails.plugin.cookiesession.enabled = Boolean.valueOf((System.getenv('COOKIE_SESSION_ENABLED') ?: 'true'))
grails.plugin.cookiesession.encryptcookie = true
grails.plugin.cookiesession.condenseexceptions = true
grails.plugin.cookiesession.cookiecount = 5
grails.plugin.cookiesession.maxcookiesize = 2048
grails.plugin.cookiesession.httponly = true
grails.plugin.cookiesession.serializer = 'kryo'
grails.plugin.cookiesession.setsecure = Boolean.getBoolean('server.ssl.enabled')

