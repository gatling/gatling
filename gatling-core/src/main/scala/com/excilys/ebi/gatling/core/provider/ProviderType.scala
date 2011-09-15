package com.excilys.ebi.gatling.core.provider

object ProviderType extends Enumeration {
  type ProviderType = Value
  val REGEXP_PROVIDER, XPATH_PROVIDER, HTTP_HEADERS_PROVIDER, HTTP_STATUS_PROVIDER = Value
}