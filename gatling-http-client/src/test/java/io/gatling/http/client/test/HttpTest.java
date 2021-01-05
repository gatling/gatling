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

package io.gatling.http.client.test;

import io.gatling.http.client.HttpClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HttpTest {

    public static final int TIMEOUT_SECONDS = 5;
    protected static final String COMPLETED_EVENT = "Completed";
    protected static final String STATUS_RECEIVED_EVENT = "StatusReceived";
    protected static final String HEADERS_RECEIVED_EVENT = "HeadersReceived";
    protected static final String HEADERS_WRITTEN_EVENT = "HeadersWritten";
    protected static final String CONNECTION_OPEN_EVENT = "ConnectionOpen";
    protected static final String HOSTNAME_RESOLUTION_EVENT = "HostnameResolution";
    protected static final String HOSTNAME_RESOLUTION_SUCCESS_EVENT = "HostnameResolutionSuccess";
    protected static final String CONNECTION_SUCCESS_EVENT = "ConnectionSuccess";
    protected static final String TLS_HANDSHAKE_EVENT = "TlsHandshake";
    protected static final String TLS_HANDSHAKE_SUCCESS_EVENT = "TlsHandshakeSuccess";
    protected static final String CONNECTION_POOL_EVENT = "ConnectionPool";
    protected static final String CONNECTION_OFFER_EVENT = "ConnectionOffer";
    protected static final String REQUEST_SEND_EVENT = "RequestSend";
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected ClientTestBody withClient() {
        return withClient(new HttpClientConfig());
    }

    protected ClientTestBody withClient(HttpClientConfig config) {
        return new ClientTestBody(config);
    }

    protected ServerTestBody withServer(TestServer server) {
        return new ServerTestBody(server);
    }

    @FunctionalInterface
    protected interface ClientFunction {
        void apply(TestClient client) throws Throwable;
    }

    @FunctionalInterface
    protected interface ServerFunction {
        void apply(TestServer server) throws Throwable;
    }

    protected static class ClientTestBody {

        private final HttpClientConfig config;

        private ClientTestBody(HttpClientConfig config) {
            this.config = config;
        }

        public void run(ClientFunction f) throws Throwable {
            try (TestClient client = new TestClient(config)) {
                f.apply(client);
            }
        }
    }

    protected static class ServerTestBody {

        private final TestServer server;

        private ServerTestBody(TestServer server) {
            this.server = server;
        }

        public void run(ServerFunction f) throws Throwable {
            try {
                f.apply(server);
            } finally {
                server.reset();
            }
        }
    }
}

