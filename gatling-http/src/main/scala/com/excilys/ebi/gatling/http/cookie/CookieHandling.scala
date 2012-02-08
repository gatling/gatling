package com.excilys.ebi.gatling.http.cookie
import java.net.URI

import scala.collection.mutable.HashMap

import com.excilys.ebi.gatling.core.session.Session
import com.ning.http.client.Cookie

trait CookieHandling {

	val COOKIES_CONTEXT_KEY = "gatling.http.cookies"

	def getStoredCookies(session: Session, url: String) = {
		session.getAttributeAsOption[HashMap[CookieKey, Cookie]](COOKIES_CONTEXT_KEY) match {
			case Some(storedCookies) => {
				if (!storedCookies.isEmpty) {
					val uri = URI.create(url)
					val uriHost = uri.getHost
					val uriPath = uri.getPath
					val list = storedCookies.filter(entry => uriHost.endsWith(entry._1.domain) && uriPath.startsWith(entry._1.path)).map(_._2).toList
					list
				} else {
					Nil
				}
			}
			case None => Nil
		}
	}

	def storeCookies(session: Session, url: String, cookies: Seq[Cookie]) = {
		if (!cookies.isEmpty) {
			val storedCookies = session.getAttributeAsOption[HashMap[CookieKey, Cookie]](COOKIES_CONTEXT_KEY).getOrElse(new HashMap[CookieKey, Cookie])
			val uri = URI.create(url)
			val uriHost = uri.getHost
			val uriPath = uri.getPath
			for (cookie <- cookies) {
				val cookieDomain = if (cookie.getDomain != null) cookie.getDomain else uriHost
				val cookiePath = if (cookie.getPath != null) cookie.getPath else uriPath
				storedCookies.put(CookieKey(cookieDomain, cookiePath, cookie.getName), cookie)
			}
			
			session.setAttribute(COOKIES_CONTEXT_KEY, storedCookies)
		} else {
			session
		}
	}
}

private case class CookieKey(domain: String, path: String, name: String)