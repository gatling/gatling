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

package io.gatling.core.json;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class JsonJava {
  private JsonJava() {}

  public static Object asJava(JsonNode node) {
    switch (node.getNodeType()) {
      case ARRAY:
        switch (node.size()) {
          case 0:
            return Collections.emptyList();
          case 1:
            return Collections.singletonList(asJava(node.get(0)));
          default:
            return StreamSupport.stream(node.spliterator(), false)
              .map(JsonJava::asJava)
              .collect(Collectors.toList());
        }
      case OBJECT:
        switch (node.size()) {
          case 0:
            return Collections.emptyMap();
          case 1:
            Map.Entry<String, JsonNode> entry0 = node.fields().next();
            return Collections.singletonMap(entry0.getKey(), asJava(entry0.getValue()));
          default:
            Map<String, Object> map = new TreeMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
              Map.Entry<String, JsonNode> entry = fields.next();
              map.put(entry.getKey(), asJava(entry.getValue()));
            }
            return map;
        }
      case STRING:
        return node.textValue();
      case BOOLEAN:
        return node.booleanValue();
      case NULL:
        return null;
      case NUMBER:
        switch (node.numberType()) {
        case INT : return node.intValue();
        case LONG: return node.longValue();
        case FLOAT       : return node.floatValue();
        case DOUBLE      : return node.doubleValue();
        case BIG_INTEGER : return node.bigIntegerValue();
        case BIG_DECIMAL : return node.decimalValue();
      }
      default: throw new IllegalArgumentException("Unsupported node type " + node.getNodeType());
    }
  }
}
