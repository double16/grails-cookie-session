## 4.0.2

SERIALIZATION COMPATIBILITY:

- Compatible with 4.0.0 - 4.0.1

IMPROVEMENTS:

- Performance improvements
- Compile using Groovy indy (JDK 1.7+ invoke dynamic)
- Build and code quality improvements
 
## 4.0.1

SERIALIZATION COMPATIBILITY:

- Compatible with 4.0.0

IMPROVEMENTS:

- Upgrade Kyro from 4.0.1 to 4.0.2
- Build and code quality improvements
 
## 4.0.0

SERIALIZATION COMPATIBILITY:

- Incompatible with previous versions

FEATURES:

- Project transferred to Patrick Double, https://github.com/double16
- Cookie size threshold logging [PR-35](https://github.com/benlucchesi/grails-cookie-session/pull/35) (@robzienert)

IMPROVEMENTS:

- Upgrade to Kryo 4.0.1
- Use Snappy compression if native library is found (fallback to deflate)
- Performance improvements
- Add (deep) dirty checking support and only set cookies if changed, fixes [GH-48](https://github.com/benlucchesi/grails-cookie-session/issues/48) (@exell-christopher, @double16)
- Throw MaxSizeExceededException instead of raw exception when session max size is exceeded, [PR-71](https://github.com/benlucchesi/grails-cookie-session/pull/71) (@sanmibuh)

BUG FIXES:

- Documentation fix for `httponly` config [GH-72](https://github.com/benlucchesi/grails-cookie-session/issues/72)
- SimpleGrantedAuthority cannot be serialized by Kryo [GH-54](https://github.com/benlucchesi/grails-cookie-session/issues/54)
- When using Spring Security the session gets larger and larger [GH-53](https://github.com/benlucchesi/grails-cookie-session/issues/53)
- Cannot cast com.esotericsoftware.shaded...StdInstantiatorStrategy to InstantiatorStrategy [GH-52](https://github.com/benlucchesi/grails-cookie-session/issues/52)
- Merge tests into the main repository [GH-38](https://github.com/benlucchesi/grails-cookie-session/issues/38)
