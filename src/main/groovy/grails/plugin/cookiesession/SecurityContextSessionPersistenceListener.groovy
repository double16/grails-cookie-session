/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Ben Lucchesi
 *  ben@granicus.com or benlucchesi@gmail.com
 */

package grails.plugin.cookiesession;

import org.springframework.beans.factory.InitializingBean

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityContextSessionPersistenceListener implements SessionPersistenceListener, InitializingBean {

    final static Logger log = LoggerFactory.getLogger(SecurityContextSessionPersistenceListener.class.getName());

    def grailsApplication
    def securityContextHolder

    String cookieName = "gsession"
    
    void afterPropertiesSet(){
      log.trace "afterPropertiesSet()"
      if( grailsApplication.config.grails.plugin.cookiesession.containsKey('cookiename') ){
        cookieName = grailsApplication.config.grails.plugin.cookiesession.cookiename
      }

      securityContextHolder = grailsApplication.classLoader.loadClass("org.springframework.security.core.context.SecurityContextHolder")
    }

    public void afterSessionRestored( SerializableSession session ){
    }

    public void beforeSessionSaved( SerializableSession session ){
      log.trace "beforeSessionSaved()"
      
      // needed for backwards compatibility
      if( session.SPRING_SECURITY_SAVED_REQUEST_KEY){
        def sessionCookies = session.SPRING_SECURITY_SAVED_REQUEST_KEY.@cookies.findAll{ it.name =~ cookieName }
        sessionCookies.each{
          session.SPRING_SECURITY_SAVED_REQUEST_KEY.@cookies.remove(it)
        }
        log.trace "removed cookies from saved request in SPRING_SECURITY_SAVED_REQUEST_KEY" 
      }
      
      if( session.SPRING_SECURITY_SAVED_REQUEST ){
        def sessionCookies = session.SPRING_SECURITY_SAVED_REQUEST.@cookies.findAll{ it.name =~ cookieName }
        sessionCookies.each{
          session.SPRING_SECURITY_SAVED_REQUEST.@cookies.remove(it)
        }
        log.trace "removed cookies from saved request in SPRING_SECURITY_SAVED_REQUEST" 
      }

      // FIXME: remove 'cookie' header, or @headers altogether

      if( session.SPRING_SECURITY_CONTEXT != securityContextHolder.getContext() ){
        log.info "persisting security context to session"
        session.SPRING_SECURITY_CONTEXT = securityContextHolder.getContext()
      }
      else{
        log.trace "not persisting security context"
      }
    }
}
