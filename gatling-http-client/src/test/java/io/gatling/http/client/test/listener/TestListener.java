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

package io.gatling.http.client.test.listener;

import java.util.concurrent.CompletableFuture;

public abstract class TestListener extends ResponseAsStringListener {

  private final CompletableFuture<Void> result = new CompletableFuture<>();

  public abstract void onComplete0();

  @Override
  public void onComplete() {
    try {
      onComplete0();
      result.complete(null);
    } catch (Throwable e) {
      result.completeExceptionally(e);
    }
  }

  @Override
  public void onThrowable(Throwable e) {
    result.completeExceptionally(e);
  }

  public CompletableFuture<Void> getResult() {
    return result;
  }

  public static class NoopTestListener extends TestListener {
    @Override
    public void onComplete0() {}
  }
}
