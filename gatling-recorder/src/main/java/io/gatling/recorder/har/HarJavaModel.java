/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.recorder.har;

import java.util.List;

public class HarJavaModel {

  public static class HarHttpArchive {
    private HarLog log;

    public HarLog getLog() {
      return log;
    }
  }

  public static class HarLog {
    private List<HarEntry> entries;

    public List<HarEntry> getEntries() {
      return entries;
    }
  }

  public static class HarEntry {
    private String startedDateTime;
    private Double  time;
    private HarTimings timings;
    private HarRequest request;
    private HarResponse response;

    public String getStartedDateTime() {
      return startedDateTime;
    }

    public Double getTime() {
      return time;
    }

    public HarTimings getTimings() {
      return timings;
    }

    public HarRequest getRequest() {
      return request;
    }

    public HarResponse getResponse() {
      return response;
    }
  }

  public static class HarRequest {
    private String httpVersion;
    private String method;
    private String url;
    private List<HarHeader> headers;
    private HarRequestPostData postData;

    public String getHttpVersion() {
      return httpVersion;
    }

    public String getMethod() {
      return method;
    }

    public String getUrl() {
      return url;
    }

    public List<HarHeader> getHeaders() {
      return headers;
    }

    public HarRequestPostData getPostData() {
      return postData;
    }
  }

  public static class HarHeader {
    private String name;
    private String value;

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }
  }

  public static class HarRequestPostData {
    private String text;
    private List<HarRequestPostParam> params;

    public String getText() {
      return text;
    }

    public List<HarRequestPostParam> getParams() {
      return params;
    }
  }

  public static class HarRequestPostParam {
    private String name;
    private String value;

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }
  }

  public static class HarResponse {
    private int status;
    private List<HarHeader> headers;
    private String statusText;
    private HarResponseContent content;

    public int getStatus() {
      return status;
    }

    public List<HarHeader> getHeaders() {
      return headers;
    }

    public String getStatusText() {
      return statusText;
    }

    public HarResponseContent getContent() {
      return content;
    }
  }

  public static class HarResponseContent {
    private String mimeType;
    private String encoding;
    private String text;
    private String comment;

    public String getMimeType() {
      return mimeType;
    }

    public String getEncoding() {
      return encoding;
    }

    public String getText() {
      return text;
    }

    public String getComment() {
      return comment;
    }
  }
  public static class HarTimings {
    private double blocked;
    private double dns;
    private double connect;
    private double ssl;
    private double send;
    private double wait;
    private double receive;

    public double getBlocked() {
      return blocked;
    }

    public double getDns() {
      return dns;
    }

    public double getConnect() {
      return connect;
    }

    public double getSsl() {
      return ssl;
    }

    public double getSend() {
      return send;
    }

    public double getWait() {
      return wait;
    }

    public double getReceive() {
      return receive;
    }
  }
}
