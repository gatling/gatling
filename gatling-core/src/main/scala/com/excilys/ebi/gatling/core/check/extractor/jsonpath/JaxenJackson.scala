package com.excilys.ebi.gatling.core.check.extractor.jsonpath
import org.jaxen.BaseXPath

class JaxenJackson(expression: String) extends BaseXPath(expression, new JacksonNavigator)