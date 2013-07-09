/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.recorder.ui.swing.util

import java.nio.charset.Charset

import com.typesafe.scalalogging.slf4j.Logging

object CharsetHelper extends Logging {
	type Label = String
	type Name = String

	val labelOrderedPairList: List[(Name, Label)] = List(
		"utf-8" -> "Unicode (UTF-8)",
		"ISO-8859-1" -> "West European Latin-1 (ISO-8859-1)",
		"windows-1252" -> "West European Latin-1 (Windows-1252)",
		"windows-1256" -> "Arabic (Windows-1256)",
		"ISO-8859-2" -> "Central European Latin-2 (ISO-8859-2)",
		"windows-1250" -> "Central European (Windows-1250)",
		"cp852" -> "Central European (CP852)",
		"GB2312" -> "Chinese Simplified (GB2312)",
		"GB18030" -> "Chinese Simplified (GB18030)",
		"big5" -> "Chinese Traditional (Big5)",
		"ISO-8859-5" -> "Cyrillic (ISO-8859-5)",
		"KOI8-R" -> "Cyrillic (KOI8-R)",
		"windows-1251" -> "Cyrillic (Windows-1251)",
		"IBM866" -> "Cyrillic/Russian (CP-866 / IBM866)",
		"ISO-8859-7" -> "Greek (ISO-8859-7)",
		"windows-1255" -> "Hebrew (Windows-1255)",
		"Shift_JIS" -> "Japanese (Shift_JIS)",
		"EUC-JP" -> "Japanese (EUC-JP)",
		"ISO-2022-JP" -> "Japanese (ISO-2022-JP)",
		"EUC-KR" -> "Korean (EUC-KR)",
		"ISO-8859-3" -> "South European Latin-3 (ISO-8859-3)",
		"ISO-8859-9" -> "Turkish Latin-5 (ISO-8859-9)",
		"windows-1254" -> "Turkish (Windows-1254)",
		"windows-1258" -> "Vietnamese (Windows-1258)",
		"ISO-8859-15" -> "West European Latin-9 (ISO-8859-15)")
		.filter {
			case (name, label) =>
				val supported = Charset.isSupported(name)
				if (!supported) logger.warn(s"This JVM doesn't support $name $label")
				supported
		}

	val charsetNameToLabel: Map[Name, Label] = labelOrderedPairList.toMap

	val orderedLabelList: List[Label] = labelOrderedPairList.map { _._2 }

	val labelToCharsetName = charsetNameToLabel.map(_.swap)
}
