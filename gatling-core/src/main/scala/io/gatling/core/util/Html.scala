/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.util

import java.{ lang => jl }

object Html {
  private val Entities = Map(
    // basic
    "quot" -> "\"",
    "amp" -> "&",
    "lt" -> "<",
    "gt" -> ">",
    // ISO8859-1
    "nbsp" -> "\u00A0", // non-breaking space
    "iexcl" -> "\u00A1", // inverted exclamation mark
    "cent" -> "\u00A2", // cent sign
    "pound" -> "\u00A3", // pound sign
    "curren" -> "\u00A4", // currency sign
    "yen" -> "\u00A5", // yen sign = yuan sign
    "brvbar" -> "\u00A6", // broken bar = broken vertical bar
    "sect" -> "\u00A7", // section sign
    "uml" -> "\u00A8", // diaeresis = spacing diaeresis
    "copy" -> "\u00A9", // © - copyright sign
    "ordf" -> "\u00AA", // feminine ordinal indicator
    "laquo" -> "\u00AB", // left-pointing double angle quotation mark = left pointing guillemet
    "not" -> "\u00AC", // not sign
    "shy" -> "\u00AD", // soft hyphen = discretionary hyphen
    "reg" -> "\u00AE", // ® - registered trademark sign
    "macr" -> "\u00AF", // macron = spacing macron = overline = APL overbar
    "deg" -> "\u00B0", // degree sign
    "plusmn" -> "\u00B1", // plus-minus sign = plus-or-minus sign
    "sup2" -> "\u00B2", // superscript two = superscript digit two = squared
    "sup3" -> "\u00B3", // superscript three = superscript digit three = cubed
    "acute" -> "\u00B4", // acute accent = spacing acute
    "micro" -> "\u00B5", // micro sign
    "para" -> "\u00B6", // pilcrow sign = paragraph sign
    "middot" -> "\u00B7", // middle dot = Georgian comma = Greek middle dot
    "cedil" -> "\u00B8", // cedilla = spacing cedilla
    "sup1" -> "\u00B9", // superscript one = superscript digit one
    "ordm" -> "\u00BA", // masculine ordinal indicator
    "raquo" -> "\u00BB", // right-pointing double angle quotation mark = right pointing guillemet
    "frac14" -> "\u00BC", // vulgar fraction one quarter = fraction one quarter
    "frac12" -> "\u00BD", // vulgar fraction one half = fraction one half
    "frac34" -> "\u00BE", // vulgar fraction three quarters = fraction three quarters
    "iquest" -> "\u00BF", // inverted question mark = turned question mark
    "Agrave" -> "\u00C0", // À - uppercase A, grave accent
    "Aacute" -> "\u00C1", // Á - uppercase A, acute accent
    "Acirc" -> "\u00C2", // Â - uppercase A, circumflex accent
    "Atilde" -> "\u00C3", // Ã - uppercase A, tilde
    "Auml" -> "\u00C4", // Ä - uppercase A, umlaut
    "Aring" -> "\u00C5", // Å - uppercase A, ring
    "AElig" -> "\u00C6", // Æ - uppercase AE
    "Ccedil" -> "\u00C7", // Ç - uppercase C, cedilla
    "Egrave" -> "\u00C8", // È - uppercase E, grave accent
    "Eacute" -> "\u00C9", // É - uppercase E, acute accent
    "Ecirc" -> "\u00CA", // Ê - uppercase E, circumflex accent
    "Euml" -> "\u00CB", // Ë - uppercase E, umlaut
    "Igrave" -> "\u00CC", // Ì - uppercase I, grave accent
    "Iacute" -> "\u00CD", // Í - uppercase I, acute accent
    "Icirc" -> "\u00CE", // Î - uppercase I, circumflex accent
    "Iuml" -> "\u00CF", // Ï - uppercase I, umlaut
    "ETH" -> "\u00D0", // Ð - uppercase Eth, Icelandic
    "Ntilde" -> "\u00D1", // Ñ - uppercase N, tilde
    "Ograve" -> "\u00D2", // Ò - uppercase O, grave accent
    "Oacute" -> "\u00D3", // Ó - uppercase O, acute accent
    "Ocirc" -> "\u00D4", // Ô - uppercase O, circumflex accent
    "Otilde" -> "\u00D5", // Õ - uppercase O, tilde
    "Ouml" -> "\u00D6", // Ö - uppercase O, umlaut
    "times" -> "\u00D7", // multiplication sign
    "Oslash" -> "\u00D8", // Ø - uppercase O, slash
    "Ugrave" -> "\u00D9", // Ù - uppercase U, grave accent
    "Uacute" -> "\u00DA", // Ú - uppercase U, acute accent
    "Ucirc" -> "\u00DB", // Û - uppercase U, circumflex accent
    "Uuml" -> "\u00DC", // Ü - uppercase U, umlaut
    "Yacute" -> "\u00DD", // Ý - uppercase Y, acute accent
    "THORN" -> "\u00DE", // Þ - uppercase THORN, Icelandic
    "szlig" -> "\u00DF", // ß - lowercase sharps, German
    "agrave" -> "\u00E0", // à - lowercase a, grave accent
    "aacute" -> "\u00E1", // á - lowercase a, acute accent
    "acirc" -> "\u00E2", // â - lowercase a, circumflex accent
    "atilde" -> "\u00E3", // ã - lowercase a, tilde
    "auml" -> "\u00E4", // ä - lowercase a, umlaut
    "aring" -> "\u00E5", // å - lowercase a, ring
    "aelig" -> "\u00E6", // æ - lowercase ae
    "ccedil" -> "\u00E7", // ç - lowercase c, cedilla
    "egrave" -> "\u00E8", // è - lowercase e, grave accent
    "eacute" -> "\u00E9", // é - lowercase e, acute accent
    "ecirc" -> "\u00EA", // ê - lowercase e, circumflex accent
    "euml" -> "\u00EB", // ë - lowercase e, umlaut
    "igrave" -> "\u00EC", // ì - lowercase i, grave accent
    "iacute" -> "\u00ED", // í - lowercase i, acute accent
    "icirc" -> "\u00EE", // î - lowercase i, circumflex accent
    "iuml" -> "\u00EF", // ï - lowercase i, umlaut
    "eth" -> "\u00F0", // ð - lowercase eth, Icelandic
    "ntilde" -> "\u00F1", // ñ - lowercase n, tilde
    "ograve" -> "\u00F2", // ò - lowercase o, grave accent
    "oacute" -> "\u00F3", // ó - lowercase o, acute accent
    "ocirc" -> "\u00F4", // ô - lowercase o, circumflex accent
    "otilde" -> "\u00F5", // õ - lowercase o, tilde
    "ouml" -> "\u00F6", // ö - lowercase o, umlaut
    "divide" -> "\u00F7", // division sign
    "oslash" -> "\u00F8", // ø - lowercase o, slash
    "ugrave" -> "\u00F9", // ù - lowercase u, grave accent
    "uacute" -> "\u00FA", // ú - lowercase u, acute accent
    "ucirc" -> "\u00FB", // û - lowercase u, circumflex accent
    "uuml" -> "\u00FC", // ü - lowercase u, umlaut
    "yacute" -> "\u00FD", // ý - lowercase y, acute accent
    "thorn" -> "\u00FE", // þ - lowercase thorn, Icelandic
    "yuml" -> "\u00FF", // ÿ - lowercase y, umlaut
    // HTML/4.0 extended
    // Latin Extended-B
    "fnof" -> "\u0192", // latin small f with hook = function= florin, U+0192 ISOtech -->
    // <!-- Greek -->
    "Alpha" -> "\u0391", // greek capital letter alpha, U+0391 -->
    "Beta" -> "\u0392", // greek capital letter beta, U+0392 -->
    "Gamma" -> "\u0393", // greek capital letter gamma,U+0393 ISOgrk3 -->
    "Delta" -> "\u0394", // greek capital letter delta,U+0394 ISOgrk3 -->
    "Epsilon" -> "\u0395", // greek capital letter epsilon, U+0395 -->
    "Zeta" -> "\u0396", // greek capital letter zeta, U+0396 -->
    "Eta" -> "\u0397", // greek capital letter eta, U+0397 -->
    "Theta" -> "\u0398", // greek capital letter theta,U+0398 ISOgrk3 -->
    "Iota" -> "\u0399", // greek capital letter iota, U+0399 -->
    "Kappa" -> "\u039A", // greek capital letter kappa, U+039A -->
    "Lambda" -> "\u039B", // greek capital letter lambda,U+039B ISOgrk3 -->
    "Mu" -> "\u039C", // greek capital letter mu, U+039C -->
    "Nu" -> "\u039D", // greek capital letter nu, U+039D -->
    "Xi" -> "\u039E", // greek capital letter xi, U+039E ISOgrk3 -->
    "Omicron" -> "\u039F", // greek capital letter omicron, U+039F -->
    "Pi" -> "\u03A0", // greek capital letter pi, U+03A0 ISOgrk3 -->
    "Rho" -> "\u03A1", // greek capital letter rho, U+03A1 -->
    // <!-- there is no Sigmaf, and no U+03A2 character either -->
    "Sigma" -> "\u03A3", // greek capital letter sigma,U+03A3 ISOgrk3 -->
    "Tau" -> "\u03A4", // greek capital letter tau, U+03A4 -->
    "Upsilon" -> "\u03A5", // greek capital letter upsilon,U+03A5 ISOgrk3 -->
    "Phi" -> "\u03A6", // greek capital letter phi,U+03A6 ISOgrk3 -->
    "Chi" -> "\u03A7", // greek capital letter chi, U+03A7 -->
    "Psi" -> "\u03A8", // greek capital letter psi,U+03A8 ISOgrk3 -->
    "Omega" -> "\u03A9", // greek capital letter omega,U+03A9 ISOgrk3 -->
    "alpha" -> "\u03B1", // greek small letter alpha,U+03B1 ISOgrk3 -->
    "beta" -> "\u03B2", // greek small letter beta, U+03B2 ISOgrk3 -->
    "gamma" -> "\u03B3", // greek small letter gamma,U+03B3 ISOgrk3 -->
    "delta" -> "\u03B4", // greek small letter delta,U+03B4 ISOgrk3 -->
    "epsilon" -> "\u03B5", // greek small letter epsilon,U+03B5 ISOgrk3 -->
    "zeta" -> "\u03B6", // greek small letter zeta, U+03B6 ISOgrk3 -->
    "eta" -> "\u03B7", // greek small letter eta, U+03B7 ISOgrk3 -->
    "theta" -> "\u03B8", // greek small letter theta,U+03B8 ISOgrk3 -->
    "iota" -> "\u03B9", // greek small letter iota, U+03B9 ISOgrk3 -->
    "kappa" -> "\u03BA", // greek small letter kappa,U+03BA ISOgrk3 -->
    "lambda" -> "\u03BB", // greek small letter lambda,U+03BB ISOgrk3 -->
    "mu" -> "\u03BC", // greek small letter mu, U+03BC ISOgrk3 -->
    "nu" -> "\u03BD", // greek small letter nu, U+03BD ISOgrk3 -->
    "xi" -> "\u03BE", // greek small letter xi, U+03BE ISOgrk3 -->
    "omicron" -> "\u03BF", // greek small letter omicron, U+03BF NEW -->
    "pi" -> "\u03C0", // greek small letter pi, U+03C0 ISOgrk3 -->
    "rho" -> "\u03C1", // greek small letter rho, U+03C1 ISOgrk3 -->
    "sigmaf" -> "\u03C2", // greek small letter final sigma,U+03C2 ISOgrk3 -->
    "sigma" -> "\u03C3", // greek small letter sigma,U+03C3 ISOgrk3 -->
    "tau" -> "\u03C4", // greek small letter tau, U+03C4 ISOgrk3 -->
    "upsilon" -> "\u03C5", // greek small letter upsilon,U+03C5 ISOgrk3 -->
    "phi" -> "\u03C6", // greek small letter phi, U+03C6 ISOgrk3 -->
    "chi" -> "\u03C7", // greek small letter chi, U+03C7 ISOgrk3 -->
    "psi" -> "\u03C8", // greek small letter psi, U+03C8 ISOgrk3 -->
    "omega" -> "\u03C9", // greek small letter omega,U+03C9 ISOgrk3 -->
    "thetasym" -> "\u03D1", // greek small letter theta symbol,U+03D1 NEW -->
    "upsih" -> "\u03D2", // greek upsilon with hook symbol,U+03D2 NEW -->
    "piv" -> "\u03D6", // greek pi symbol, U+03D6 ISOgrk3 -->
    // <!-- General Punctuation -->
    "bull" -> "\u2022", // bullet = black small circle,U+2022 ISOpub -->
    // <!-- bullet is NOT the same as bullet operator, U+2219 -->
    "hellip" -> "\u2026", // horizontal ellipsis = three dot leader,U+2026 ISOpub -->
    "prime" -> "\u2032", // prime = minutes = feet, U+2032 ISOtech -->
    "Prime" -> "\u2033", // double prime = seconds = inches,U+2033 ISOtech -->
    "oline" -> "\u203E", // overline = spacing overscore,U+203E NEW -->
    "frasl" -> "\u2044", // fraction slash, U+2044 NEW -->
    // <!-- Letterlike Symbols -->
    "weierp" -> "\u2118", // script capital P = power set= Weierstrass p, U+2118 ISOamso -->
    "image" -> "\u2111", // blackletter capital I = imaginary part,U+2111 ISOamso -->
    "real" -> "\u211C", // blackletter capital R = real part symbol,U+211C ISOamso -->
    "trade" -> "\u2122", // trade mark sign, U+2122 ISOnum -->
    "alefsym" -> "\u2135", // alef symbol = first transfinite cardinal,U+2135 NEW -->
    // <!-- alef symbol is NOT the same as hebrew letter alef,U+05D0 although the
    // same glyph could be used to depict both characters -->
    // <!-- Arrows -->
    "larr" -> "\u2190", // leftwards arrow, U+2190 ISOnum -->
    "uarr" -> "\u2191", // upwards arrow, U+2191 ISOnum-->
    "rarr" -> "\u2192", // rightwards arrow, U+2192 ISOnum -->
    "darr" -> "\u2193", // downwards arrow, U+2193 ISOnum -->
    "harr" -> "\u2194", // left right arrow, U+2194 ISOamsa -->
    "crarr" -> "\u21B5", // downwards arrow with corner leftwards= carriage return, U+21B5 NEW -->
    "lArr" -> "\u21D0", // leftwards double arrow, U+21D0 ISOtech -->
    // <!-- ISO 10646 does not say that lArr is the same as the 'is implied by'
    // arrow but also does not have any other character for that function.
    // So ? lArr canbe used for 'is implied by' as ISOtech suggests -->
    "uArr" -> "\u21D1", // upwards double arrow, U+21D1 ISOamsa -->
    "rArr" -> "\u21D2", // rightwards double arrow,U+21D2 ISOtech -->
    // <!-- ISO 10646 does not say this is the 'implies' character but does not
    // have another character with this function so ?rArr can be used for
    // 'implies' as ISOtech suggests -->
    "dArr" -> "\u21D3", // downwards double arrow, U+21D3 ISOamsa -->
    "hArr" -> "\u21D4", // left right double arrow,U+21D4 ISOamsa -->
    // <!-- Mathematical Operators -->
    "forall" -> "\u2200", // for all, U+2200 ISOtech -->
    "part" -> "\u2202", // partial differential, U+2202 ISOtech -->
    "exist" -> "\u2203", // there exists, U+2203 ISOtech -->
    "empty" -> "\u2205", // empty set = null set = diameter,U+2205 ISOamso -->
    "nabla" -> "\u2207", // nabla = backward difference,U+2207 ISOtech -->
    "isin" -> "\u2208", // element of, U+2208 ISOtech -->
    "notin" -> "\u2209", // not an element of, U+2209 ISOtech -->
    "ni" -> "\u220B", // contains as member, U+220B ISOtech -->
    // <!-- should there be a more memorable name than 'ni'? -->
    "prod" -> "\u220F", // n-ary product = product sign,U+220F ISOamsb -->
    // <!-- prod is NOT the same character as U+03A0 'greek capital letter pi'
    // though the same glyph might be used for both -->
    "sum" -> "\u2211", // n-ary summation, U+2211 ISOamsb -->
    // <!-- sum is NOT the same character as U+03A3 'greek capital letter sigma'
    // though the same glyph might be used for both -->
    "minus" -> "\u2212", // minus sign, U+2212 ISOtech -->
    "lowast" -> "\u2217", // asterisk operator, U+2217 ISOtech -->
    "radic" -> "\u221A", // square root = radical sign,U+221A ISOtech -->
    "prop" -> "\u221D", // proportional to, U+221D ISOtech -->
    "infin" -> "\u221E", // infinity, U+221E ISOtech -->
    "ang" -> "\u2220", // angle, U+2220 ISOamso -->
    "and" -> "\u2227", // logical and = wedge, U+2227 ISOtech -->
    "or" -> "\u2228", // logical or = vee, U+2228 ISOtech -->
    "cap" -> "\u2229", // intersection = cap, U+2229 ISOtech -->
    "cup" -> "\u222A", // union = cup, U+222A ISOtech -->
    "int" -> "\u222B", // integral, U+222B ISOtech -->
    "there4" -> "\u2234", // therefore, U+2234 ISOtech -->
    "sim" -> "\u223C", // tilde operator = varies with = similar to,U+223C ISOtech -->
    // <!-- tilde operator is NOT the same character as the tilde, U+007E,although
    // the same glyph might be used to represent both -->
    "cong" -> "\u2245", // approximately equal to, U+2245 ISOtech -->
    "asymp" -> "\u2248", // almost equal to = asymptotic to,U+2248 ISOamsr -->
    "ne" -> "\u2260", // not equal to, U+2260 ISOtech -->
    "equiv" -> "\u2261", // identical to, U+2261 ISOtech -->
    "le" -> "\u2264", // less-than or equal to, U+2264 ISOtech -->
    "ge" -> "\u2265", // greater-than or equal to,U+2265 ISOtech -->
    "sub" -> "\u2282", // subset of, U+2282 ISOtech -->
    "sup" -> "\u2283", // superset of, U+2283 ISOtech -->
    // <!-- note that nsup, 'not a superset of, U+2283' is not covered by the
    // Symbol font encoding and is not included. Should it be, for symmetry?
    // It is in ISOamsn -->,
    "nsub" -> "\u2284", // not a subset of, U+2284 ISOamsn -->
    "sube" -> "\u2286", // subset of or equal to, U+2286 ISOtech -->
    "supe" -> "\u2287", // superset of or equal to,U+2287 ISOtech -->
    "oplus" -> "\u2295", // circled plus = direct sum,U+2295 ISOamsb -->
    "otimes" -> "\u2297", // circled times = vector product,U+2297 ISOamsb -->
    "perp" -> "\u22A5", // up tack = orthogonal to = perpendicular,U+22A5 ISOtech -->
    "sdot" -> "\u22C5", // dot operator, U+22C5 ISOamsb -->
    // <!-- dot operator is NOT the same character as U+00B7 middle dot -->
    // <!-- Miscellaneous Technical -->
    "lceil" -> "\u2308", // left ceiling = apl upstile,U+2308 ISOamsc -->
    "rceil" -> "\u2309", // right ceiling, U+2309 ISOamsc -->
    "lfloor" -> "\u230A", // left floor = apl downstile,U+230A ISOamsc -->
    "rfloor" -> "\u230B", // right floor, U+230B ISOamsc -->
    "lang" -> "\u2329", // left-pointing angle bracket = bra,U+2329 ISOtech -->
    // <!-- lang is NOT the same character as U+003C 'less than' or U+2039 'single left-pointing angle quotation
    // mark' -->
    "rang" -> "\u232A", // right-pointing angle bracket = ket,U+232A ISOtech -->
    // <!-- rang is NOT the same character as U+003E 'greater than' or U+203A
    // 'single right-pointing angle quotation mark' -->
    // <!-- Geometric Shapes -->
    "loz" -> "\u25CA", // lozenge, U+25CA ISOpub -->
    // <!-- Miscellaneous Symbols -->
    "spades" -> "\u2660", // black spade suit, U+2660 ISOpub -->
    // <!-- black here seems to mean filled as opposed to hollow -->
    "clubs" -> "\u2663", // black club suit = shamrock,U+2663 ISOpub -->
    "hearts" -> "\u2665", // black heart suit = valentine,U+2665 ISOpub -->
    "diams" -> "\u2666", // black diamond suit, U+2666 ISOpub -->
    // <!-- Latin Extended-A -->
    "OElig" -> "\u0152", // -- latin capital ligature OE,U+0152 ISOlat2 -->
    "oelig" -> "\u0153", // -- latin small ligature oe, U+0153 ISOlat2 -->
    // <!-- ligature is a misnomer, this is a separate character in some languages -->
    "Scaron" -> "\u0160", // -- latin capital letter S with caron,U+0160 ISOlat2 -->
    "scaron" -> "\u0161", // -- latin small letter s with caron,U+0161 ISOlat2 -->
    "Yuml" -> "\u0178", // -- latin capital letter Y with diaeresis,U+0178 ISOlat2 -->
    // <!-- Spacing Modifier Letters -->
    "circ" -> "\u02C6", // -- modifier letter circumflex accent,U+02C6 ISOpub -->
    "tilde" -> "\u02DC", // small tilde, U+02DC ISOdia -->
    // <!-- General Punctuation -->
    "ensp" -> "\u2002", // en space, U+2002 ISOpub -->
    "emsp" -> "\u2003", // em space, U+2003 ISOpub -->
    "thinsp" -> "\u2009", // thin space, U+2009 ISOpub -->
    "zwnj" -> "\u200C", // zero width non-joiner,U+200C NEW RFC 2070 -->
    "zwj" -> "\u200D", // zero width joiner, U+200D NEW RFC 2070 -->
    "lrm" -> "\u200E", // left-to-right mark, U+200E NEW RFC 2070 -->
    "rlm" -> "\u200F", // right-to-left mark, U+200F NEW RFC 2070 -->
    "ndash" -> "\u2013", // en dash, U+2013 ISOpub -->
    "mdash" -> "\u2014", // em dash, U+2014 ISOpub -->
    "lsquo" -> "\u2018", // left single quotation mark,U+2018 ISOnum -->
    "rsquo" -> "\u2019", // right single quotation mark,U+2019 ISOnum -->
    "sbquo" -> "\u201A", // single low-9 quotation mark, U+201A NEW -->
    "ldquo" -> "\u201C", // left double quotation mark,U+201C ISOnum -->
    "rdquo" -> "\u201D", // right double quotation mark,U+201D ISOnum -->
    "bdquo" -> "\u201E", // double low-9 quotation mark, U+201E NEW -->
    "dagger" -> "\u2020", // dagger, U+2020 ISOpub -->
    "Dagger" -> "\u2021", // double dagger, U+2021 ISOpub -->
    "permil" -> "\u2030", // per mille sign, U+2030 ISOtech -->
    "lsaquo" -> "\u2039", // single left-pointing angle quotation mark,U+2039 ISO proposed -->
    // <!-- lsaquo is proposed but not yet ISO standardized -->
    "rsaquo" -> "\u203A", // single right-pointing angle quotation mark,U+203A ISO proposed -->
    // <!-- rsaquo is proposed but not yet ISO standardized -->
    "euro" -> "\u20AC" // -- euro sign, U+20AC NEW -->
  )

  private def lookupEntity(input: String, begin: Int, end: Int): String =
    if (input.charAt(begin) == '#') {
      // numeric
      val hexFlag = input.charAt(begin + 1)
      try {
        val entityValue =
          if (hexFlag == 'x' || hexFlag == 'X') {
            Integer.parseInt(input.substring(begin + 2, end), 16)
          } else {
            Integer.parseInt(input.substring(begin + 1, end))
          }

        if (entityValue > 0xffff) {
          new String(Character.toChars(entityValue))
        } else {
          String.valueOf(entityValue.toChar)
        }
      } catch {
        case _: NumberFormatException => null
      }
    } else {
      Entities.getOrElse(input.substring(begin, end), null)
    }

  def unescape(input: String): String = {
    var pos = 0
    var sb: jl.StringBuilder = null
    var nextFlushedPos = 0
    val length = input.length

    while (pos < length) {
      val ampersandPos = input.indexOf('&', pos)
      if (ampersandPos == -1) {
        // no more entities
        pos = length
      } else {
        val semiColonPos = input.indexOf(';', ampersandPos + 1)
        if (semiColonPos == -1) {
          // no more entities
          pos = length
        } else {
          val entity = lookupEntity(input, ampersandPos + 1, semiColonPos)
          if (entity == null) {
            // not a valid entity, moving forward
            // note: we don't directly jump to semiColonPos + 1 because we could have "&foo &bar;"
            pos = ampersandPos + 1
          } else {
            // flush gap since last flushedIndex
            if (sb == null) {
              sb = new jl.StringBuilder(length)
            }
            sb.append(input, nextFlushedPos, ampersandPos).append(entity)
            nextFlushedPos = semiColonPos + 1
            pos = semiColonPos + 1
          }
        }
      }
    }

    if (sb == null) {
      input
    } else {
      if (nextFlushedPos < length) {
        sb.append(input, nextFlushedPos, length)
      }
      sb.toString
    }
  }
}
