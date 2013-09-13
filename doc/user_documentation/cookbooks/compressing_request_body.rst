########################
Compressing Request Body
########################

Compressing Request Body is something that should be supported out of the box by async-http-client, but it's sadly currently `broken <https://github.com/AsyncHttpClient/async-http-client/issues/93>`_.

Have faith, Gatling user, there's a work around! Let's compress the body manually and pass it to ``byteArrayBody``.

The exemple below use `JZlib <https://github.com/ymnk/jzlib>`_ in order to compress the outcome of a Scalate ssp template. The lib has to be provided in the classpath (add to the lib directory if running from CLI). Of course, one can use the regular yet less efficient ``java.util.zip.GZIPOutputStream``.

::

	// note: pass parameters as Strings so we can transparently set up extra bindings
	// otherwise, explicitly define all the bindings in the ssp file
	def generateGzippedBytesFromSsp(templateName: String, attributes: Map[String, String]) = {

	  import java.io.{ ByteArrayOutputStream, PrintWriter }
	  import org.fusesource.scalate.{ Binding, DefaultRenderContext }
	  import com.excilys.ebi.gatling.http.request.builder.AbstractHttpRequestWithBodyBuilder
	  import com.excilys.ebi.gatling.core.util.IOHelper
	  import com.jcraft.jzlib.GZIPOutputStream

	  val out = new ByteArrayOutputStream

	  IOHelper.use(new PrintWriter(new GZIPOutputStream(out))) { pw => // just a borrow pattern
	    val extraBindings = attributes.keySet.map(Binding(_, "String"))
	    val renderContext = new DefaultRenderContext("templateName", AbstractHttpRequestWithBodyBuilder.TEMPLATE_ENGINE, pw)
	    for ((key, value) <- attributes) { renderContext.attributes(key) = value }

	    AbstractHttpRequestWithBodyBuilder.TEMPLATE_ENGINE.layout(templateName, renderContext, extraBindings)
	  }
	  
	  out.toByteArray
	}

	val users = scenario("RequestCompression")
	  .exec(
	    http("Gzip")
	      .post("postUrl")
	      // important: properly set header so the server understand the request is compressed
	      .header("Content-Encoding", "gzip")
	      .byteArrayBody { session =>
	        // assumes there's a "foo" attribute in Session
	        val sspParams = Map("foo" -> session.getAttribute("foo").toString)
	        generateGzippedBytesFromSsp("template.ssp", sspParams)
	      })
