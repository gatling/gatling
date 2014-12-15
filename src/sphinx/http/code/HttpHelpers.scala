import io.gatling.core.Predef._
import io.gatling.http.Predef._

class HttpHelpers {

  //#cookie
  exec(addCookie(Cookie("name", "value")))
  //#cookie

  //#flushSessionCookies
  exec(flushSessionCookies)
  //#flushSessionCookies

  //#flushCookieJar
  exec(flushCookieJar)
  //#flushCookieJar

  //#flushHttpCache
  exec(flushHttpCache)
  //#flushHttpCache
}
