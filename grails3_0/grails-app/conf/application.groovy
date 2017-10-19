
grails.plugin.cookiesession.enabled = true
grails.plugin.cookiesession.encryptcookie = true
grails.plugin.cookiesession.cryptoalgorithm = "Blowfish"
grails.plugin.cookiesession.condenseexceptions = true
grails.plugin.cookiesession.cookiecount = 5
grails.plugin.cookiesession.maxcookiesize = 2048
grails.plugin.cookiesession.httponly = true
grails.plugin.cookiesession.serializer = 'kryo'
grails.plugin.cookiesession.springsecuritycompatibility = true
grails.plugin.springsecurity.useSessionFixationPrevention = false

// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'grails3_0.User'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'grails3_0.UserRole'
grails.plugin.springsecurity.authority.className = 'grails3_0.Role'
grails.plugin.springsecurity.controllerAnnotations.staticRules = [
	[pattern: '/',               access: ['permitAll']],
	[pattern: '/error',          access: ['permitAll']],
	[pattern: '/index',          access: ['permitAll']],
	[pattern: '/index.gsp',      access: ['permitAll']],
	[pattern: '/shutdown',       access: ['permitAll']],
	[pattern: '/assets/**',      access: ['permitAll']],
	[pattern: '/**/js/**',       access: ['permitAll']],
	[pattern: '/**/css/**',      access: ['permitAll']],
	[pattern: '/**/images/**',   access: ['permitAll']],
	[pattern: '/**/favicon.ico', access: ['permitAll']]
]

grails.plugin.springsecurity.filterChain.chainMap = [
	[pattern: '/assets/**',      filters: 'none'],
	[pattern: '/**/js/**',       filters: 'none'],
	[pattern: '/**/css/**',      filters: 'none'],
	[pattern: '/**/images/**',   filters: 'none'],
	[pattern: '/**/favicon.ico', filters: 'none'],
	[pattern: '/**',             filters: 'JOINED_FILTERS']
]

