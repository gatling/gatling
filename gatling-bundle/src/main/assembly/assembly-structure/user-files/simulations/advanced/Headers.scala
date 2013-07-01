package advanced

object Headers {
	val headers_1 = Map(
		"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
		"Accept-Encoding" -> "gzip,deflate",
		"Accept-Language" -> "fr,en-us;q=0.7,en;q=0.3",
		"User-Agent" -> "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.17) Gecko/20110422 Ubuntu/9.10 (karmic) Firefox/3.6.17")

	val headers_3 = headers_1 ++ Map(
		"Content-Type" -> "application/x-www-form-urlencoded")

	val headers_6 = Map(
		"Accept" -> "application/json, text/javascript, */*; q=0.01",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
		"Accept-Encoding" -> "gzip,deflate",
		"Accept-Language" -> "fr,en-us;q=0.7,en;q=0.3",
		"User-Agent" -> "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.17) Gecko/20110422 Ubuntu/9.10 (karmic) Firefox/3.6.17",
		"X-Requested-With" -> "XMLHttpRequest")

	val headers_8 = Map(
		"Accept" -> "application/json, text/javascript, */*; q=0.01",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
		"Accept-Encoding" -> "gzip,deflate",
		"Accept-Language" -> "fr,en-us;q=0.7,en;q=0.3",
		"User-Agent" -> "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.17) Gecko/20110422 Ubuntu/9.10 (karmic) Firefox/3.6.17",
		"X-Requested-With" -> "XMLHttpRequest")
}