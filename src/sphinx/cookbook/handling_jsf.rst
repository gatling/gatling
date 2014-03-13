############
Handling JSF
############

Basic JSF
=========

JSF requires a parameter named ``javax.faces.ViewState`` to be captured on every page and be passed in every POST request.

Adding a check for capturing the value and a param on very request would be very cumbersome.
Hopefully, we can mutualize these operations.

Define factory methods for building JSF requests that would automatically perform those operations::

	import com.excilys.ebi.gatling.core.session.EvaluatableString

	val jsfViewStateCheck = regex("""="javax.faces.ViewState" value="([^"]*)"""")
	  .saveAs("viewState")
	def jsfGet(name: String, url: EvaluatableString) = http(name).get(url)
	  .check(jsfViewStateCheck)
	def jsfPost(name: String, url: EvaluatableString) = http(name).post(url)
	  .param("javax.faces.ViewState", "${viewState}")
	  .check(jsfViewStateCheck)

You can then build your requests just like you're used to::

	val scn = scenario("Scenario Name")
	  .exec(jsfGet("request_1", "/showcase-labs/ui/pprUpdate.jsf").headers(headers_1))
	  .pause(80 milliseconds)
	  .exec(
	    jsfPost("request_2", "/showcase-labs/ui/pprUpdate.jsf")
	        .param("javax.faces.partial.ajax", "true")
	        .param("javax.faces.source", "form:btn")
	        .param("javax.faces.partial.execute", "@all")
	        .param("javax.faces.partial.render", "form:display")
	        .param("form:btn", "form:btn")
	        .param("form", "form")
	        .param("form:name", "foo"))

.. note:: The sample above is taken from the `Primefaces demo <http://www.primefaces.org/showcase-labs>`_

Trinidad
========

Trinidad's ``_afPfm`` query parameter can be handled in a similar fashion::

	val jsfPageFlowCheck = regex("""\?_afPfm=([^"]*)"""").saveAs("afPfm")
	val jsfViewStateCheck = regex("""="javax.faces.ViewState" value="([^"]*)"""")
	  .saveAs("viewState")

	def jsfGet(name: String, url: EvaluatableString) = http(name).get(url)
	  .check(jsfViewStateCheck)
	def jsfPost(name: String, url: EvaluatableString) = http(name).post(url)
	  .param("javax.faces.ViewState", "${viewState}")
	  .check(jsfViewStateCheck).check(jsfPageFlowCheck)

	def trinidadPost(name: String, url: EvaluatableString) = http(name).post(url)
	  .param("javax.faces.ViewState", "${viewState}")
	  .queryParam("_afPfm", "${afPfm}")
	  .check(jsfViewStateCheck)
	  .check(jsfPageFlowCheck)
	def trinidadDownload(name: String, url: EvaluatableString) = http(name).post(url)
	  .param("javax.faces.ViewState", "${viewState}")
	  .queryParam("_afPfm", "${afPfm}")
