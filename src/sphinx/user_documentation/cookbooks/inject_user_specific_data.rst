#########################
Inject User Specific Data
#########################

As explained in the documentation, a Feeder is shared amongst all users.

So, what if one want to inject user specific data?

Rest assured, this can be achieve programmatically, with the support of Gatling built-ins:

* ``csv`` and the likes are actually parsers that produce Map[String, String] (they are implicitly or explicitly converted into Feeders with the ``.queue`` method and the likes).
* ``foreach`` can be used to looping onto a Sequence session attribute

In the following example, we assume that:

* the ``credential.csv`` file contains 2 columns named ``username`` and ``password``
* the ``documents.csv`` file contains document records indexed by a column named ``username``. This way, we intend to have every user picks its specific records.
* the ``documents.csv`` file contains at least one record per user

::

	// use the default/implicit queue strategy
	val credentials = csv("credentials.csv")

	// group the document records by username
	val documents = csv("documents.csv").groupBy(_("username"))

	// inject into the Session the documents specific to the user
	val injectUserDocuments = (session: Session) => {
	  val username: String = session.getTypedAttribute("username")
	  session.setAttribute("documents", documents(username).toSeq)
	}

	// inject the record as separate attributes into the Session
	val injectDocumentData = (session: Session) => {
	  val documentData: Map[String, String] = session.getTypedAttribute("document")
	  session.setAttributes(documentData)
	}

	val scn = scenario("Sample")
	  // inject credentials into the user Session
	  .feed(credentials)
	  .exec(injectUserDocuments)
	  // loop on the documents and store the current document record
	  .foreach("documents", "document") {
	    exec(injectDocumentData)
	    ... // do work with the document data
	  }
