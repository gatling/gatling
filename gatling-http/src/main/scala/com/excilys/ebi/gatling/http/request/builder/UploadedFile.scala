/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.http.request.builder

import java.io.File

import scala.tools.nsc.io.Path.string2path

import com.excilys.ebi.gatling.core.config.GatlingFiles
import com.excilys.ebi.gatling.core.session.{ EvaluatableString, Session }
import com.excilys.ebi.gatling.core.util.PathHelper.path2string
import com.ning.http.client.FilePart

class UploadedFile(paramKeyFunction: EvaluatableString, fileNameFunction: EvaluatableString, mimeType: String, charset: String) {
	def filePart(session: Session) = {

		val paramKey = paramKeyFunction(session)
		val fileName = fileNameFunction(session)

		val path = GatlingFiles.requestBodiesDirectory / fileName
		val file = new File(path)

		if (!file.exists) throw new IllegalArgumentException("Uploaded file %s does not exist".format(path))
		if (!file.isFile) throw new IllegalArgumentException("Uploaded file %s is not a real file".format(path))
		if (!file.canRead) throw new IllegalArgumentException("Uploaded file %s can't be read".format(path))

		new FilePart(paramKey, file, mimeType, charset)
	}
}