/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.util

import java.nio.ByteBuffer

import org.apache.commons.io.IOUtils
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import io.gatling.core.test.ValidationSpecification

@RunWith(classOf[JUnitRunner])
class ByteBufferInputStreamSpec extends ValidationSpecification {

	val short = """<?xml version="1.0" encoding="UTF-8"?>
<response>
<lst name="responseHeader"><int name="status">0</int><int name="QTime">3</int></lst>
</response>"""

	val long = """Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque egestas ligula nec ligula cursus mattis. Integer laoreet vel nisi quis posuere. Fusce consectetur tortor nulla, eu semper nisl pellentesque nec. Quisque vel orci eleifend, dignissim sem ut, facilisis arcu. Nulla sollicitudin mollis aliquam. Proin sed pellentesque nisl. Etiam quis nisl quis nisi rhoncus porttitor vel sit amet urna. Nam feugiat ultrices quam, ut aliquam est. Curabitur vitae elit tellus. Ut tempor laoreet mollis. Aenean vitae mauris ac nibh sodales lacinia eget vitae nisi.

Sed vel molestie urna. Suspendisse commodo dolor sed augue porttitor, eget lobortis mauris sollicitudin. Pellentesque sed condimentum velit. Sed pulvinar erat vitae felis euismod placerat. Proin vitae varius erat, ac pulvinar enim. Phasellus laoreet placerat lectus eu ultricies. Suspendisse et tortor eu lorem pharetra facilisis vitae eu risus. Phasellus pellentesque orci nec lacinia dapibus. Curabitur ullamcorper, ipsum eget fermentum pharetra, nibh magna vehicula nulla, vitae pretium sem tortor a augue. Cras pulvinar consectetur quam et pretium. In fringilla est id risus faucibus auctor. Aliquam erat volutpat. Integer dapibus placerat risus, vel volutpat lacus egestas vel. Quisque eget dui nisl. Quisque hendrerit ligula quis metus rhoncus facilisis.

Suspendisse id metus euismod, facilisis eros ac, ornare ipsum. In quis turpis ligula. Vestibulum ut nibh vitae orci posuere gravida. Cras eget ipsum vel dolor dictum volutpat. Suspendisse sodales eros vel egestas vehicula. Nullam sapien ligula, iaculis ultrices euismod at, fermentum ut elit. Suspendisse venenatis placerat justo, at pellentesque odio. Maecenas in malesuada justo. Vestibulum risus mauris, faucibus fermentum dui vitae, luctus molestie purus. Fusce venenatis id purus in gravida. Quisque vehicula tellus velit, non faucibus est rutrum id.

Cras mattis ligula eget arcu vulputate tincidunt. In id felis eu eros hendrerit fermentum. In rutrum sodales velit, non aliquam sem euismod a. Praesent posuere, lectus blandit ultricies blandit, purus neque tristique turpis, et accumsan leo risus et orci. Pellentesque congue odio id quam faucibus facilisis. Donec vel dolor vitae turpis ultricies fermentum. Praesent mattis bibendum sagittis. Praesent id mauris ut eros mattis tristique. Integer vitae blandit leo, ultrices lobortis mi. Integer quis leo lectus. Donec pellentesque condimentum turpis nec euismod. Cras blandit elementum augue, quis bibendum est euismod vitae. Donec tincidunt risus a urna tincidunt, non fringilla nibh pulvinar. Pellentesque vestibulum malesuada purus vitae placerat.

Aliquam ullamcorper, tellus et accumsan fermentum, massa quam dictum nunc, sed porttitor diam tellus ac nulla. Integer ultrices ante a venenatis consectetur. Cras sed nunc eget neque aliquam iaculis luctus vel quam. In aliquam erat eu ultricies iaculis. Sed eu placerat dui. Maecenas viverra consequat congue. Vestibulum quis tortor id metus dapibus viverra. Sed id lorem tempus, pharetra erat sed, dapibus turpis. Cras consequat mi justo, vel auctor orci iaculis in. Morbi dignissim dictum rutrum. Aliquam volutpat eros sit amet eros adipiscing interdum. Nulla vel ligula ut tellus pellentesque iaculis in id lectus. Sed aliquet lobortis ante ac imperdiet. Nulla non pellentesque libero, ut vehicula augue."""

	"bytes to buffer to stream and back" should {

		def string2ByteBufferInputStreamAndBack(text: String) = {
			IOHelper.withCloseable(new ByteBufferInputStream(ByteBuffer.wrap(text.getBytes("UTF-8")))) { is =>
				IOUtils.toString(is, "UTF-8")
			}
		}

		"work with short text" in {
			string2ByteBufferInputStreamAndBack(short) must beEqualTo(short)
		}

		"work with long text" in {
			string2ByteBufferInputStreamAndBack(long) must beEqualTo(long)
		}
	}
}
