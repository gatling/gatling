/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.javaapi.http.internal;

import io.gatling.core.check.regex.GroupExtractor;
import io.gatling.http.check.header.HttpHeaderRegexCheckType;
import io.gatling.http.check.url.CurrentLocationRegexCheckType;
import io.gatling.http.response.Response;
import io.gatling.javaapi.core.CheckBuilder;

public final class HttpCheckBuilder {
  private HttpCheckBuilder() {}

  public static final class CurrentLocationRegex
      extends CheckBuilder.CaptureGroupCheckBuilder.Default<CurrentLocationRegexCheckType, String> {

    public CurrentLocationRegex(
        io.gatling.core.check.CheckBuilder.MultipleFind<
                CurrentLocationRegexCheckType, String, String>
            wrapped) {
      super(wrapped, HttpCheckType.CurrentLocationRegex);
    }

    @Override
    protected <X>
        io.gatling.core.check.CheckBuilder.MultipleFind<CurrentLocationRegexCheckType, String, X>
            extract(GroupExtractor<X> groupExtractor) {
      io.gatling.http.check.url.CurrentLocationRegexCheckBuilder<String> actual =
          (io.gatling.http.check.url.CurrentLocationRegexCheckBuilder<String>) wrapped;
      return new io.gatling.http.check.url.CurrentLocationRegexCheckBuilder<>(
          actual.pattern(), actual.patterns(), groupExtractor);
    }
  }

  public static final class HeaderRegexCheck
      extends CheckBuilder.CaptureGroupCheckBuilder.Default<HttpHeaderRegexCheckType, Response> {

    public HeaderRegexCheck(
        io.gatling.core.check.CheckBuilder.MultipleFind<HttpHeaderRegexCheckType, Response, String>
            wrapped) {
      super(wrapped, HttpCheckType.HeaderRegex);
    }

    @Override
    protected <X>
        io.gatling.core.check.CheckBuilder.MultipleFind<HttpHeaderRegexCheckType, Response, X>
            extract(GroupExtractor<X> groupExtractor) {
      io.gatling.http.check.header.HttpHeaderRegexCheckBuilder<String> actual =
          (io.gatling.http.check.header.HttpHeaderRegexCheckBuilder<String>) wrapped;
      return new io.gatling.http.check.header.HttpHeaderRegexCheckBuilder<>(
          actual.headerName(), actual.pattern(), actual.patterns(), groupExtractor);
    }
  }
}
