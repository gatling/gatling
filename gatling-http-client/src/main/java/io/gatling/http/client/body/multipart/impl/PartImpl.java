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

package io.gatling.http.client.body.multipart.impl;

import static io.gatling.http.client.util.MiscUtils.*;

import io.gatling.http.client.Param;
import io.gatling.http.client.body.multipart.Part;
import io.gatling.http.client.body.multipart.impl.PartVisitor.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static java.nio.charset.StandardCharsets.US_ASCII;

public abstract class PartImpl implements Closeable {

  /**
   * Content disposition as a byte
   */
  static final byte QUOTE_BYTE = '\"';
  /**
   * Carriage return/linefeed as a byte array
   */
  static final byte[] CRLF_BYTES = "\r\n".getBytes(US_ASCII);
  /**
   * Extra characters as a byte array
   */
  static final byte[] EXTRA_BYTES = "--".getBytes(US_ASCII);

  /**
   * Content disposition as a byte array
   */
  private static final byte[] CONTENT_DISPOSITION_BYTES = "Content-Disposition: ".getBytes(US_ASCII);

  /**
   * form-data as a byte array
   */
  private static final byte[] FORM_DATA_DISPOSITION_TYPE_BYTES = "form-data".getBytes(US_ASCII);

  /**
   * name as a byte array
   */
  private static final byte[] NAME_BYTES = "; name=".getBytes(US_ASCII);

  /**
   * Content type header as a byte array
   */
  private static final byte[] CONTENT_TYPE_BYTES = "Content-Type: ".getBytes(US_ASCII);

  /**
   * Content charset as a byte array
   */
  private static final byte[] CHARSET_BYTES = "; charset=".getBytes(US_ASCII);

  /**
   * Content type header as a byte array
   */
  private static final byte[] CONTENT_TRANSFER_ENCODING_BYTES = "Content-Transfer-Encoding: ".getBytes(US_ASCII);

  /**
   * Content type header as a byte array
   */
  private static final byte[] HEADER_NAME_VALUE_SEPARATOR_BYTES = ": ".getBytes(US_ASCII);

  /**
   * Content type header as a byte array
   */
  private static final byte[] CONTENT_ID_BYTES = "Content-ID: ".getBytes(US_ASCII);

  protected final Part<?> part;
  protected final byte[] boundary;

  private final int preContentLength;
  private final int postContentLength;
  protected PartImplState state;

  // lazy
  private ByteBuf preContentBuffer;
  private ByteBuf postContentBuffer;

  PartImpl(Part<?> part, byte[] boundary) {
    this.part = part;
    this.boundary = boundary;
    preContentLength = computePreContentLength();
    postContentLength = computePostContentLength();
    state = PartImplState.PRE_CONTENT;
  }

  public long length() {
    return preContentLength + postContentLength + getContentLength();
  }

  public PartImplState getState() {
    return state;
  }

  public void copyInto(ByteBuf target) throws IOException {

    switch (state) {
      case DONE:
        return;

      case PRE_CONTENT:
        copyInto(lazyLoadPreContentBuffer(), target, PartImplState.CONTENT);
        return;

      case CONTENT:
        copyContentInto(target);
        return;

      case POST_CONTENT:
        copyInto(lazyLoadPostContentBuffer(), target, PartImplState.DONE);
        return;

      default:
        throw new IllegalStateException("Unknown state " + state);
    }
  }

  private ByteBuf lazyLoadPreContentBuffer() {
    if (preContentBuffer == null)
      preContentBuffer = computePreContentBytes(preContentLength);
    return preContentBuffer;
  }

  private ByteBuf lazyLoadPostContentBuffer() {
    if (postContentBuffer == null)
      postContentBuffer = computePostContentBytes(postContentLength);
    return postContentBuffer;
  }

  @Override
  public void close() {
    if (preContentBuffer != null) {
      preContentBuffer.release();
    }
    if (postContentBuffer != null) {
      postContentBuffer.release();
    }
  }

  protected abstract long getContentLength();

  protected abstract void copyContentInto(ByteBuf target) throws IOException;

  void copyInto(ByteBuf source, ByteBuf target, PartImplState sourceFullyWrittenState) {

    int sourceRemaining = source.readableBytes();
    int targetRemaining = target.writableBytes();

    if (sourceRemaining <= targetRemaining) {
      target.writeBytes(source);
      state = sourceFullyWrittenState;
    } else {
      target.writeBytes(source, targetRemaining);
    }
  }

  protected int computePreContentLength() {
    CounterPartVisitor counterVisitor = new CounterPartVisitor();
    visitPreContent(counterVisitor);
    return counterVisitor.getCount();
  }

  protected ByteBuf computePreContentBytes(int preContentLength) {
    ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(preContentLength);
    ByteBufVisitor bytesVisitor = new ByteBufVisitor(buffer);
    visitPreContent(bytesVisitor);
    return buffer;
  }

  protected int computePostContentLength() {
    CounterPartVisitor counterVisitor = new CounterPartVisitor();
    visitPostContent(counterVisitor);
    return counterVisitor.getCount();
  }

  protected ByteBuf computePostContentBytes(int postContentLength) {
    ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(postContentLength);
    ByteBufVisitor bytesVisitor = new ByteBufVisitor(buffer);
    visitPostContent(bytesVisitor);
    return buffer;
  }

  private void visitStart(PartVisitor visitor) {
    visitor.withBytes(EXTRA_BYTES);
    visitor.withBytes(boundary);
  }

  protected void visitContentDispositionHeader(PartVisitor visitor) {
    visitor.withBytes(CRLF_BYTES);
    visitor.withBytes(CONTENT_DISPOSITION_BYTES);
    visitor.withBytes(part.getDispositionType() != null ? part.getDispositionType().getBytes(US_ASCII) : FORM_DATA_DISPOSITION_TYPE_BYTES);
    if (part.getName() != null) {
      visitor.withBytes(NAME_BYTES);
      visitor.withByte(QUOTE_BYTE);
      visitor.withBytes(part.getName().getBytes(US_ASCII));
      visitor.withByte(QUOTE_BYTE);
    }
  }

  private void visitContentTypeHeader(PartVisitor visitor) {
    String contentType = part.getContentType();
    if (contentType != null) {
      visitor.withBytes(CRLF_BYTES);
      visitor.withBytes(CONTENT_TYPE_BYTES);
      visitor.withBytes(contentType.getBytes(US_ASCII));
      Charset charSet = part.getCharset();
      if (charSet != null) {
        visitor.withBytes(CHARSET_BYTES);
        visitor.withBytes(part.getCharset().name().getBytes(US_ASCII));
      }
    }
  }

  private void visitTransferEncodingHeader(PartVisitor visitor) {
    String transferEncoding = part.getTransferEncoding();
    if (transferEncoding != null) {
      visitor.withBytes(CRLF_BYTES);
      visitor.withBytes(CONTENT_TRANSFER_ENCODING_BYTES);
      visitor.withBytes(transferEncoding.getBytes(US_ASCII));
    }
  }

  private void visitContentIdHeader(PartVisitor visitor) {
    String contentId = part.getContentId();
    if (contentId != null) {
      visitor.withBytes(CRLF_BYTES);
      visitor.withBytes(CONTENT_ID_BYTES);
      visitor.withBytes(contentId.getBytes(US_ASCII));
    }
  }

  private void visitCustomHeaders(PartVisitor visitor) {
    if (isNonEmpty(part.getCustomHeaders())) {
      List<Param> customHeaders = part.getCustomHeaders();
      for (Param param : customHeaders) {
        visitor.withBytes(CRLF_BYTES);
        visitor.withBytes(param.getName().getBytes(US_ASCII));
        visitor.withBytes(HEADER_NAME_VALUE_SEPARATOR_BYTES);
        visitor.withBytes(param.getValue().getBytes(US_ASCII));
      }
    }
  }

  private void visitEndOfHeaders(PartVisitor visitor) {
    visitor.withBytes(CRLF_BYTES);
    visitor.withBytes(CRLF_BYTES);
  }

  private void visitPreContent(PartVisitor visitor) {
    visitStart(visitor);
    visitContentDispositionHeader(visitor);
    visitContentTypeHeader(visitor);
    visitTransferEncodingHeader(visitor);
    visitContentIdHeader(visitor);
    visitCustomHeaders(visitor);
    visitEndOfHeaders(visitor);
  }

  private void visitPostContent(PartVisitor visitor) {
    visitor.withBytes(CRLF_BYTES);
  }
}
