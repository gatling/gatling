package advanced

object Headers {
	val headers_1 = Map(
		"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
		"Accept-Encoding" -> "gzip,deflate",
		"Accept-Language" -> "fr,en-us;q=0.7,en;q=0.3",
		"Host" -> "excilys-bank-web.cloudfoundry.com",
		"Keep-Alive" -> "115",
		"User-Agent" -> "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.17) Gecko/20110422 Ubuntu/9.10 (karmic) Firefox/3.6.17")

	val headers_3 = headers_1 ++ Map(
		"Content-Length" -> "33",
		"Content-Type" -> "application/x-www-form-urlencoded",
		"Referer" -> "http://excilys-bank-web.cloudfoundry.com/public/login.html")

	val headers_6 = Map(
		"Accept" -> "application/json, text/javascript, */*; q=0.01",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
		"Accept-Encoding" -> "gzip,deflate",
		"Accept-Language" -> "fr,en-us;q=0.7,en;q=0.3",
		"Host" -> "excilys-bank-web.cloudfoundry.com",
		"Keep-Alive" -> "115",
		"Referer" -> "http://excilys-bank-web.cloudfoundry.com/private/bank/account/ACC4/operations.html",
		"User-Agent" -> "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.17) Gecko/20110422 Ubuntu/9.10 (karmic) Firefox/3.6.17",
		"X-Requested-With" -> "XMLHttpRequest")

	val headers_8 = Map(
		"Accept" -> "application/json, text/javascript, */*; q=0.01",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
		"Accept-Encoding" -> "gzip,deflate",
		"Accept-Language" -> "fr,en-us;q=0.7,en;q=0.3",
		"Host" -> "excilys-bank-web.cloudfoundry.com",
		"Keep-Alive" -> "115",
		"Referer" -> "http://excilys-bank-web.cloudfoundry.com/private/bank/account/ACC4/year/2011/month/11/operations.html",
		"User-Agent" -> "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.17) Gecko/20110422 Ubuntu/9.10 (karmic) Firefox/3.6.17",
		"X-Requested-With" -> "XMLHttpRequest")
}