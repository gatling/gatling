/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.http.client.ahc.uri;

import static io.gatling.http.client.ahc.util.MiscUtils.*;

import io.gatling.http.client.Param;
import io.gatling.netty.util.ahc.StringBuilderPool;
import io.gatling.netty.util.ahc.Utf8UrlEncoder;

import java.util.List;

public enum UriEncoder {

  FIXING {
    public String encodePath(String path) {
      return Utf8UrlEncoder.encodePath(path);
    }

    private void encodeAndAppendQueryParam(final StringBuilder sb, final CharSequence name, final CharSequence value) {
      Utf8UrlEncoder.encodeAndAppendQueryElement(sb, name);
      if (value != null) {
        sb.append('=');
        Utf8UrlEncoder.encodeAndAppendQueryElement(sb, value);
      }
      sb.append('&');
    }

    private void encodeAndAppendQueryParams(final StringBuilder sb, final List<Param> queryParams) {
      for (Param param : queryParams)
        encodeAndAppendQueryParam(sb, param.getName(), param.getValue());
    }

    protected String withQueryWithParams(final String query, final List<Param> queryParams) {
      // concatenate encoded query + encoded query params
      StringBuilder sb = StringBuilderPool.DEFAULT.get();
      Utf8UrlEncoder.encodeAndAppendQuery(sb, query);
      sb.append('&');
      encodeAndAppendQueryParams(sb, queryParams);
      sb.setLength(sb.length() - 1);
      return sb.toString();
    }

    protected String withQueryWithoutParams(final String query) {
      // encode query
      StringBuilder sb = StringBuilderPool.DEFAULT.get();
      Utf8UrlEncoder.encodeAndAppendQuery(sb, query);
      return sb.toString();
    }

    protected String withoutQueryWithParams(final List<Param> queryParams) {
      // concatenate encoded query params
      StringBuilder sb = StringBuilderPool.DEFAULT.get();
      encodeAndAppendQueryParams(sb, queryParams);
      sb.setLength(sb.length() - 1);
      return sb.toString();
    }
  },

  RAW {
    public String encodePath(String path) {
      return path;
    }

    private void appendRawQueryParam(StringBuilder sb, String name, String value) {
      sb.append(name);
      if (value != null)
        sb.append('=').append(value);
      sb.append('&');
    }

    private void appendRawQueryParams(final StringBuilder sb, final List<Param> queryParams) {
      for (Param param : queryParams)
        appendRawQueryParam(sb, param.getName(), param.getValue());
    }

    protected String withQueryWithParams(final String query, final List<Param> queryParams) {
      // concatenate raw query + raw query params
      StringBuilder sb = StringBuilderPool.DEFAULT.get();
      sb.append(query);
      appendRawQueryParams(sb, queryParams);
      sb.setLength(sb.length() - 1);
      return sb.toString();
    }

    protected String withQueryWithoutParams(final String query) {
      // return raw query as is
      return query;
    }

    protected String withoutQueryWithParams(final List<Param> queryParams) {
      // concatenate raw queryParams
      StringBuilder sb = StringBuilderPool.DEFAULT.get();
      appendRawQueryParams(sb, queryParams);
      sb.setLength(sb.length() - 1);
      return sb.toString();
    }
  };

  public static UriEncoder uriEncoder(boolean fixUrlEncoding) {
    return fixUrlEncoding ? FIXING : RAW;
  }

  protected abstract String withQueryWithParams(final String query, final List<Param> queryParams);

  protected abstract String withQueryWithoutParams(final String query);

  protected abstract String withoutQueryWithParams(final List<Param> queryParams);

  private String withQuery(final String query, final List<Param> queryParams) {
    return isNonEmpty(queryParams) ? withQueryWithParams(query, queryParams) : withQueryWithoutParams(query);
  }

  private String withoutQuery(final List<Param> queryParams) {
    return isNonEmpty(queryParams) ? withoutQueryWithParams(queryParams) : null;
  }

  public Uri encode(Uri uri, List<Param> queryParams) {
    String newPath = encodePath(uri.getPath());
    String newQuery = encodeQuery(uri.getQuery(), queryParams);
    return new Uri(uri.getScheme(),
            uri.getUserInfo(),
            uri.getHost(),
            uri.getPort(),
            newPath,
            newQuery);
  }

  protected abstract String encodePath(String path);

  private String encodeQuery(final String query, final List<Param> queryParams) {
    return isNonEmpty(query) ? withQuery(query, queryParams) : withoutQuery(queryParams);
  }
}
