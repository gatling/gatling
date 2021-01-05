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

package io.gatling.http.client.ssl;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class Tls {

  private Tls() {
  }

  public static Set<String> extractSubjectAlternativeNames(SSLEngine sslEngine) throws SSLPeerUnverifiedException, CertificateParsingException {
    Set<String> sans = new HashSet<>();
    for (Certificate certificate : sslEngine.getSession().getPeerCertificates()) {
      X509Certificate cert = (X509Certificate) certificate;
      Collection<List<?>> subjectAlternativeNames = cert.getSubjectAlternativeNames();
      if (subjectAlternativeNames != null) {
        for (List<?> certificateSans : subjectAlternativeNames) {
          if (certificateSans != null) {
            for (Object san : certificateSans) {
              if (san instanceof String) {
                sans.add((String) san);
              }
            }
          }
        }
      }
    }
    return sans;
  }

  private static final Pattern CERT_PATTERN = Pattern.compile("\\.");

  public static boolean isCertificateAuthoritative(String san, String domain) {
    String[] sanSplit = CERT_PATTERN.split(san);
    String[] domainSplit = CERT_PATTERN.split(domain);

    if (sanSplit.length != domainSplit.length)
      return false;

    String firstDomainChunk = domainSplit[0];

    String firstLabel = sanSplit[0];
    int firstLabelLength = firstLabel.length();

    int wildCardPosition = firstLabel.indexOf('*');

    boolean isFirstLabelValid;

    if (wildCardPosition == -1) {
      isFirstLabelValid = false;
    } else if (wildCardPosition == 0 && firstLabelLength == 1) {
      isFirstLabelValid = true;
    } else if (wildCardPosition == 0) {
      String after = firstLabel.substring(wildCardPosition + 1);
      isFirstLabelValid = firstDomainChunk.endsWith(after);
    } else if (wildCardPosition == firstLabelLength - 1) {
      String before = firstLabel.substring(0, wildCardPosition);
      isFirstLabelValid = firstDomainChunk.startsWith(before);
    } else {
      String after = firstLabel.substring(wildCardPosition + 1);
      String before = firstLabel.substring(0, wildCardPosition);
      isFirstLabelValid = firstDomainChunk.endsWith(after) && firstDomainChunk.startsWith(before);
    }

    boolean isCertificateAuthoritative = true;

    for (int i = isFirstLabelValid ? 1 : 0; i < domainSplit.length && isCertificateAuthoritative; i++) {
      isCertificateAuthoritative = sanSplit[i].equals(domainSplit[i]);
    }

    return isCertificateAuthoritative;
  }

  public static String domain(String hostname) {
    int fqdnLength = hostname.length() - 1;
    return hostname.charAt(fqdnLength) == '.' ?
      hostname.substring(0, fqdnLength) :
      hostname;
  }
}
