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

package io.gatling.http.client.body.part.impl;

import io.netty.channel.FileRegion;
import io.netty.util.AbstractReferenceCounted;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.List;

import static io.gatling.http.client.ahc.util.MiscUtils.closeSilently;

public class MultipartFileRegion extends AbstractReferenceCounted implements FileRegion {

  private final List<PartImpl> parts;
  private final long contentLength;
  private boolean done = false;
  private int currentPartIndex;
  private long transferred;

  public MultipartFileRegion(List<PartImpl> parts, long contentLength) {
    this.parts = parts;
    this.contentLength = contentLength;
  }

  @Override
  public long position() {
    return 0;
  }

  @Override
  public long count() {
    return contentLength;
  }

  @Override
  public long transfered() {
    return transferred();
  }

  @Override
  public long transferred() {
    return transferred;
  }

  @Override
  public FileRegion retain() {
    super.retain();
    return this;
  }

  @Override
  public FileRegion retain(int arg0) {
    super.retain(arg0);
    return this;
  }

  @Override
  public FileRegion touch() {
    return this;
  }

  @Override
  public FileRegion touch(Object arg0) {
    return this;
  }

  @Override
  public long transferTo(WritableByteChannel target, long position) throws IOException {
    long written = transferTo(target);
    if (written > 0) {
      transferred += written;
    }
    return written;
  }

  private long transferTo(WritableByteChannel target) throws IOException {

    if (done)
      return -1L;

    long transferred = 0L;
    boolean slowTarget = false;

    while (transferred < MultipartChunkedInput.DEFAULT_CHUNK_SIZE && !done && !slowTarget) {
      PartImpl currentPart = parts.get(currentPartIndex);
      transferred += currentPart.transferTo(target);
      slowTarget = currentPart.isTargetSlow();

      if (currentPart.getState() == PartImplState.DONE) {
        currentPartIndex++;
        if (currentPartIndex == parts.size()) {
          done = true;
        }
      }
    }

    return transferred;
  }

  @Override
  protected void deallocate() {
    for (PartImpl part : parts) {
      closeSilently(part);
    }
  }
}
