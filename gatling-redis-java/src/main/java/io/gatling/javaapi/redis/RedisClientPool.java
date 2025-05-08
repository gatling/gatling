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

package io.gatling.javaapi.redis;

import javax.net.ssl.SSLContext;
import scala.Option;

public final class RedisClientPool {

  private final String host;
  private final int port;
  private final int maxIdle;
  private final int database;
  private final Object secret;
  private final int timeout;
  private final int maxConnections;
  private final long poolWaitTimeout;
  private final SSLContext sslContext;
  private final boolean batch;

  private com.redis.RedisClientPool scalaInstance;

  private synchronized void loadScalaInstance() {
    if (scalaInstance == null) {
      scalaInstance =
          new com.redis.RedisClientPool(
              host,
              port,
              maxIdle,
              database,
              Option.apply(secret),
              timeout,
              maxConnections,
              poolWaitTimeout,
              Option.apply(sslContext),
              batch ? com.redis.RedisClient.BATCH$.MODULE$ : com.redis.RedisClient.SINGLE$.MODULE$);
    }
  }

  public RedisClientPool(String host, int port) {
    this(
        host,
        port,
        8,
        0,
        null,
        0,
        com.redis.RedisClientPool.UNLIMITED_CONNECTIONS(),
        3000,
        null,
        false);
  }

  private RedisClientPool(
      String host,
      int port,
      int maxIdle,
      int database,
      Object secret,
      int timeout,
      int maxConnections,
      long poolWaitTimeout,
      SSLContext sslContext,
      boolean batch) {
    this.host = host;
    this.port = port;
    this.maxIdle = maxIdle;
    this.database = database;
    this.secret = secret;
    this.timeout = timeout;
    this.maxConnections = maxConnections;
    this.poolWaitTimeout = poolWaitTimeout;
    this.sslContext = sslContext;
    this.batch = batch;
  }

  public RedisClientPool withMaxIdle(int maxIdle) {
    return new RedisClientPool(
        host,
        port,
        maxIdle,
        database,
        secret,
        timeout,
        maxConnections,
        poolWaitTimeout,
        sslContext,
        batch);
  }

  public RedisClientPool withDatabase(int database) {
    return new RedisClientPool(
        host,
        port,
        maxIdle,
        database,
        secret,
        timeout,
        maxConnections,
        poolWaitTimeout,
        sslContext,
        batch);
  }

  public RedisClientPool withSecret(Object secret) {
    return new RedisClientPool(
        host,
        port,
        maxIdle,
        database,
        secret,
        timeout,
        maxConnections,
        poolWaitTimeout,
        sslContext,
        batch);
  }

  public RedisClientPool withTimeout(int timeout) {
    return new RedisClientPool(
        host,
        port,
        maxIdle,
        database,
        secret,
        timeout,
        maxConnections,
        poolWaitTimeout,
        sslContext,
        batch);
  }

  public RedisClientPool withMaxConnections(int maxConnections) {
    return new RedisClientPool(
        host,
        port,
        maxIdle,
        database,
        secret,
        timeout,
        maxConnections,
        poolWaitTimeout,
        sslContext,
        batch);
  }

  public RedisClientPool withPoolWaitTimeout(long poolWaitTimeout) {
    return new RedisClientPool(
        host,
        port,
        maxIdle,
        database,
        secret,
        timeout,
        maxConnections,
        poolWaitTimeout,
        sslContext,
        batch);
  }

  public RedisClientPool withSSLContext(SSLContext sslContext) {
    return new RedisClientPool(
        host,
        port,
        maxIdle,
        database,
        secret,
        timeout,
        maxConnections,
        poolWaitTimeout,
        sslContext,
        batch);
  }

  public RedisClientPool withBatchMode(boolean batch) {
    return new RedisClientPool(
        host,
        port,
        maxIdle,
        database,
        secret,
        timeout,
        maxConnections,
        poolWaitTimeout,
        sslContext,
        batch);
  }

  com.redis.RedisClientPool asScala() {
    loadScalaInstance();
    return scalaInstance;
  }
}
