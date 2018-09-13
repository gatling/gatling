/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

//
// Copyright (c) 2018 AsyncHttpClient Project. All rights reserved.
//
// This program is licensed to you under the Apache License Version 2.0,
// and you may not use this file except in compliance with the Apache License Version 2.0.
// You may obtain a copy of the Apache License Version 2.0 at
//     http://www.apache.org/licenses/LICENSE-2.0.
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the Apache License Version 2.0 is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
//

package io.gatling.http.client.ahc.oauth;

import io.gatling.netty.util.ahc.Utf8UrlEncoder;

/**
 * Value class for OAuth consumer keys.
 */
public class ConsumerKey {
  private final String key;
  private final String secret;
  private final String percentEncodedKey;

  public ConsumerKey(String key, String secret) {
    this.key = key;
    this.secret = secret;
    this.percentEncodedKey = Utf8UrlEncoder.percentEncodeQueryElement(key);
  }

  public String getKey() {
    return key;
  }

  public String getSecret() {
    return secret;
  }

  public String getPercentEncodedKey() {
    return percentEncodedKey;
  }
}
