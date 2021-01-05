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

package io.gatling.jms;

import org.mockito.internal.stubbing.defaultanswers.ReturnsEmptyValues;
import org.mockito.invocation.InvocationOnMock;

class BytesMessageAnswer extends ReturnsEmptyValues {
    private byte[] bytes;

    public BytesMessageAnswer(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public Object answer(InvocationOnMock invocation) {
        switch (invocation.getMethod().getName()) {
            case "getBodyLength":
                return (long) bytes.length;
            case "readBytes":
                byte[] targetBuffer = invocation.getArgument(0);
                System.arraycopy(bytes, 0, targetBuffer, 0, targetBuffer.length);
                return targetBuffer.length;
            case "readUTF":
                return new String(bytes);
            default:
                return super.answer(invocation);
        }
    }
}
