/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.http.client.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.UUID;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class TestUtils {

  public static final String TEXT_HTML_CONTENT_TYPE_WITH_UTF_8_CHARSET = "text/html;charset=UTF-8";
  public static final String TEXT_HTML_CONTENT_TYPE_WITH_ISO_8859_1_CHARSET =
      "text/html;charset=ISO-8859-1";
  public static final File TMP_DIR =
      new File(
          System.getProperty("java.io.tmpdir"),
          "ahc-tests-" + UUID.randomUUID().toString().substring(0, 8));

  public static ServerConnector addHttpConnector(Server server) {
    ServerConnector connector = new ServerConnector(server);
    server.addConnector(connector);
    return connector;
  }

  public static ServerConnector addHttpsConnector(Server server)
      throws IOException, URISyntaxException {
    String keyStoreFile = resourceAsFile("ssltest-keystore.jks").getAbsolutePath();
    SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
    sslContextFactory.setKeyStorePath(keyStoreFile);
    sslContextFactory.setKeyStorePassword("changeit");

    String trustStoreFile = resourceAsFile("ssltest-cacerts.jks").getAbsolutePath();
    sslContextFactory.setTrustStorePath(trustStoreFile);
    sslContextFactory.setTrustStorePassword("changeit");

    HttpConfiguration httpsConfig = new HttpConfiguration();
    httpsConfig.setSecureScheme("https");
    httpsConfig.addCustomizer(new SecureRequestCustomizer());

    ServerConnector connector =
        new ServerConnector(
            server,
            new SslConnectionFactory(sslContextFactory, "http/1.1"),
            new HttpConnectionFactory(httpsConfig));

    server.addConnector(connector);

    return connector;
  }

  public static File resourceAsFile(String path) throws URISyntaxException, IOException {
    ClassLoader cl = TestUtils.class.getClassLoader();
    URI uri = cl.getResource(path).toURI();
    if (uri.isAbsolute() && !uri.isOpaque()) {
      return new File(uri);
    } else {
      File tmpFile = File.createTempFile("tmpfile-", ".data", TMP_DIR);
      tmpFile.deleteOnExit();
      try (InputStream is = cl.getResourceAsStream(path)) {
        Files.copy(is, tmpFile.toPath());
        return tmpFile;
      }
    }
  }

  public static void assertContentTypesEquals(String actual, String expected) {
    assertEquals(
        expected.replace("; ", "").toLowerCase(Locale.ROOT),
        actual.replace("; ", "").toLowerCase(Locale.ENGLISH),
        "Unexpected content-type");
  }

  @FunctionalInterface
  public interface TriConsumer<T, U, V> {
    void accept(T t, U u, V v) throws Exception;

    default TriConsumer<T, U, V> andThen(TriConsumer<? super T, ? super U, ? super V> after) {
      return (l, r, v) -> {
        accept(l, r, v);
        after.accept(l, r, v);
      };
    }
  }
}
