/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.bundle

import scala.util.control.NoStackTrace

import io.gatling.plugin.{ BatchEnterprisePlugin, BatchEnterprisePluginClient, InteractiveEnterprisePlugin, InteractiveEnterprisePluginClient }
import io.gatling.plugin.client.EnterpriseClient
import io.gatling.plugin.client.http.HttpEnterpriseClient
import io.gatling.plugin.exceptions.UnsupportedClientException

object EnterpriseBundlePlugin {
  val ApiTokenProperty = "gatling.enterprise.apiToken"
  val ApiTokenEnv = "GATLING_ENTERPRISE_API_TOKEN"
  val ClientName = "gatling-bundle"

  private[bundle] def getClient(config: CommandArguments): EnterpriseClient = {
    val validApiToken = config.apiToken match {
      case Some(token) => token
      case _ =>
        val propertiesApiToken = sys.props.getOrElse(ApiTokenProperty, sys.env.get(ApiTokenEnv).orNull)
        if (propertiesApiToken == null) {
          throw new IllegalArgumentException(
            s"""
               |An API token is required to call the Gatling Enterprise server.
               |See https://gatling.io/docs/enterprise/cloud/reference/admin/api_tokens/ and create a token with the role 'Configure'.
               |Set your API token, by either:
               | * environment variable '$ApiTokenEnv'
               | * system properties '-D$ApiTokenProperty=<API_TOKEN>'
               | * command line argument '--${CommandLineConstants.ApiToken.full} <API_TOKEN>'
               |""".stripMargin
          ) with NoStackTrace
        }
        propertiesApiToken
    }

    try {
      new HttpEnterpriseClient(
        config.url,
        validApiToken,
        ClientName,
        getClass.getPackage.getImplementationVersion,
        config.controlPlaneUrl.orNull
      )
    } catch {
      case e: UnsupportedClientException =>
        throw new IllegalArgumentException(
          "Please update the Gatling bundle to the latest version for compatibility with Gatling Enterprise. See https://gatling.io/docs/gatling/reference/current/core/configuration/#cli-options for more information about this plugin.",
          e
        )
    }
  }

  private[bundle] def getBatchEnterprisePlugin(client: EnterpriseClient): BatchEnterprisePlugin =
    new BatchEnterprisePluginClient(client, BundleIO.getLogger)

  private[bundle] def getInteractiveEnterprisePlugin(client: EnterpriseClient): InteractiveEnterprisePlugin =
    new InteractiveEnterprisePluginClient(client, BundleIO)
}
