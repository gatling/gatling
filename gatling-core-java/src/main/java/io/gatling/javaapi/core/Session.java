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

package io.gatling.javaapi.core;

import static io.gatling.javaapi.core.internal.Converters.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.gatling.javaapi.core.internal.Sessions;
import java.util.*;
import scala.collection.Seq;

/**
 * The state of a given virtual user.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class Session {

  private final io.gatling.core.session.Session wrapped;

  public Session(io.gatling.core.session.Session wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Get a stored value by its key
   *
   * @param key the storage key
   * @param <T> the type of the desired value
   * @return the value if it exists, null otherwise
   */
  public <T> @Nullable T get(@NonNull String key) {
    return wrapped.attributes().getOrElse(key, () -> null);
  }

  /**
   * Get a stored String by its key
   *
   * @param key the storage key
   * @return the value if it exists, null otherwise
   */
  public @Nullable String getString(@NonNull String key) {
    Object value = get(key);
    return value != null ? value.toString() : null;
  }

  /**
   * Get a stored {@link Integer} by its key. String representation of Integers will be parsed.
   *
   * @param key the storage key
   * @return the value if it exists, null otherwise
   * @throws NumberFormatException if the value is a String that can't be parsed into a int
   * @throws ClassCastException if the value is neither a number nor a String
   */
  public @Nullable Integer getIntegerWrapper(@NonNull String key) {
    Object value = get(key);
    if (value instanceof Integer) {
      return (Integer) value;
    } else if (value instanceof String) {
      return Integer.valueOf((String) value);
    } else if (value == null) {
      return null;
    } else {
      throw new ClassCastException(value + " is not an Integer: " + value.getClass());
    }
  }

  /**
   * Get a stored int by its key
   *
   * @param key the storage key
   * @return the value if it exists
   * @throws NullPointerException if the value is undefined
   * @throws NumberFormatException if the value is a String that can't be parsed into a int
   * @throws ClassCastException if the value is neither a number nor a String
   */
  public int getInt(@NonNull String key) {
    Object value = wrapped.attributes().apply(key);
    if (value instanceof Integer) {
      return (Integer) value;
    } else if (value instanceof String) {
      return Integer.parseInt((String) value);
    } else if (value == null) {
      throw new NullPointerException(key + " is null");
    } else {
      throw new ClassCastException(value + " is not an Integer: " + value.getClass());
    }
  }

  /**
   * Get a stored {@link Long} by its key. String representation of Longs will be parsed.
   *
   * @param key the storage key
   * @return the value if it exists, null otherwise
   * @throws NumberFormatException if the value is a String that can't be parsed into a long
   * @throws ClassCastException if the value is neither a number nor a String
   */
  public @Nullable Long getLongWrapper(@NonNull String key) {
    Object value = get(key);
    if (value instanceof Integer) {
      return ((Integer) value).longValue();
    } else if (value instanceof Long) {
      return (Long) value;
    } else if (value instanceof String) {
      return Long.valueOf((String) value);
    } else if (value == null) {
      return null;
    } else {
      throw new ClassCastException(value + " is not an Long: " + value.getClass());
    }
  }

  /**
   * Get a stored long by its key String representation of Longs will be parsed.
   *
   * @param key the storage key
   * @return the value if it exists
   * @throws NullPointerException if the value is undefined
   * @throws NumberFormatException if the value is a String that can't be parsed into a long
   * @throws ClassCastException if the value is neither a number nor a String
   */
  public long getLong(@NonNull String key) {
    Object value = wrapped.attributes().apply(key);
    if (value instanceof Integer) {
      return ((Integer) value).longValue();
    } else if (value instanceof Long) {
      return (Long) value;
    } else if (value instanceof String) {
      return Long.parseLong((String) value);
    } else if (value == null) {
      throw new NullPointerException(key + " is null");
    } else {
      throw new ClassCastException(value + " is not an Long: " + value.getClass());
    }
  }

  /**
   * Get a stored {@link Double} by its key. String representation of Doubles will be parsed.
   *
   * @param key the storage key
   * @return the value if it exists, null otherwise
   * @throws NumberFormatException if the value is a String that can't be parsed into a double
   * @throws ClassCastException if the value is neither a number nor a String
   */
  public @Nullable Double getDoubleWrapper(@NonNull String key) {
    Object value = get(key);
    if (value instanceof Integer) {
      return ((Integer) value).doubleValue();
    } else if (value instanceof Long) {
      return ((Long) value).doubleValue();
    } else if (value instanceof Double) {
      return (Double) value;
    } else if (value instanceof String) {
      return Double.valueOf((String) value);
    } else if (value == null) {
      return null;
    } else {
      throw new ClassCastException(value + " is not an Double: " + value.getClass());
    }
  }

  /**
   * Get a stored double by its key. String representation of Doubles will be parsed.
   *
   * @param key the storage key
   * @return the value if it exists
   * @throws NullPointerException if the value is undefined
   * @throws NumberFormatException if the value is a String that can't be parsed into a double
   * @throws ClassCastException if the value is neither a number nor a String
   */
  public double getDouble(@NonNull String key) {
    Object value = wrapped.attributes().apply(key);
    if (value instanceof Integer) {
      return ((Integer) value).doubleValue();
    } else if (value instanceof Long) {
      return ((Long) value).doubleValue();
    } else if (value instanceof Double) {
      return (Double) value;
    } else if (value instanceof String) {
      return Double.parseDouble((String) value);
    } else if (value == null) {
      throw new NullPointerException(key + " is null");
    } else {
      throw new ClassCastException(value + " is not an Double: " + value.getClass());
    }
  }

  /**
   * Get a stored {@link Boolean} by its key. String representation of Booleans will be parsed.
   *
   * @param key the storage key
   * @return the value if it exists, null otherwise
   * @throws NumberFormatException if the value is a String that can't be parsed into a boolean
   * @throws ClassCastException if the value is neither a boolean nor a String
   */
  public @Nullable Boolean getBooleanWrapper(@NonNull String key) {
    Object value = get(key);
    if (value instanceof Boolean) {
      return (Boolean) value;
    } else if (value instanceof String) {
      return Boolean.valueOf((String) value);
    } else if (value == null) {
      return null;
    } else {
      throw new ClassCastException(value + " is not an Boolean: " + value.getClass());
    }
  }

  /**
   * Get a stored boolean by its key. String representation of Booleans will be parsed.
   *
   * @param key the storage key
   * @return the value if it exists
   * @throws NullPointerException if the value is undefined
   * @throws NumberFormatException if the value is a String that can't be parsed into a boolean
   * @throws ClassCastException if the value is neither a boolean nor a String
   */
  public boolean getBoolean(@NonNull String key) {
    Object value = wrapped.attributes().apply(key);
    if (value instanceof Boolean) {
      return (Boolean) value;
    } else if (value instanceof String) {
      return Boolean.parseBoolean((String) value);
    } else if (value == null) {
      throw new NullPointerException(key + " is null");
    } else {
      throw new ClassCastException(value + " is not an Boolean: " + value.getClass());
    }
  }

  /**
   * Get a stored {@link List} by its key.
   *
   * @param key the storage key
   * @return the value if it exists, an empty List otherwise
   * @throws ClassCastException if the value is not a List
   */
  @NonNull
  @SuppressWarnings("unchecked")
  public <T> List<T> getList(@NonNull String key) {
    Object value = get(key);
    if (value instanceof List<?>) {
      return (List<T>) value;
    } else if (value instanceof Seq<?>) {
      return toJavaList((Seq<T>) value);
    } else if (value == null) {
      return Collections.emptyList();
    } else {
      throw new ClassCastException(value + " is not an List: " + value.getClass());
    }
  }

  /**
   * Get a stored {@link Set} by its key.
   *
   * @param key the storage key
   * @return the value if it exists, an empty Set otherwise
   * @throws ClassCastException if the value is not a Set
   */
  @NonNull
  @SuppressWarnings("unchecked")
  public <T> Set<T> getSet(@NonNull String key) {
    Object value = get(key);
    if (value instanceof Set<?>) {
      return (Set<T>) value;
    } else if (value instanceof scala.collection.Set<?>) {
      return toJavaSet((scala.collection.Set<T>) value);
    } else if (value != null) {
      throw new ClassCastException(value + " is not an Set: " + value.getClass());
    } else {
      return Collections.emptySet();
    }
  }

  /**
   * Get a stored {@link Map} by its key.
   *
   * @param key the storage key
   * @return the value if it exists, an empty Map otherwise
   * @throws ClassCastException if the value is not a Map
   */
  @NonNull
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> getMap(@NonNull String key) {
    Object value = get(key);
    if (value instanceof Map<?, ?>) {
      return (Map<String, T>) value;
    } else if (value instanceof scala.collection.Map<?, ?>) {
      return toJavaMap((scala.collection.Map<String, T>) value);
    } else if (value == null) {
      return Collections.emptyMap();
    } else {
      throw new ClassCastException(value + " is not an Map: " + value.getClass());
    }
  }

  /**
   * Create a new instance updated with a given attribute, possibly overriding an existing one
   *
   * @param key the attribute key
   * @param value the attribute value
   * @return a new instance with the new stored attribute
   */
  @NonNull
  public Session set(@NonNull String key, Object value) {
    return new Session(wrapped.set(key, value));
  }

  /**
   * Create a new instance updated with multiple attributes, possibly overriding existing ones
   *
   * @param newAttributes the new attributes
   * @return a new instance with the new stored attributes
   */
  @NonNull
  public Session setAll(@NonNull Map<String, Object> newAttributes) {
    return new Session(wrapped.setAll(toScalaMap(newAttributes)));
  }

  /**
   * Create a new instance updated with an attribute removed
   *
   * @param key the key of the attribute to remove
   * @return a new instance with the attribute removed
   */
  @NonNull
  public Session remove(@NonNull String key) {
    return new Session(wrapped.remove(key));
  }

  /**
   * Create a new instance updated with all attributes removed except Gatling internal ones
   *
   * @return a new instance with a reset user state
   */
  @NonNull
  public Session reset() {
    return new Session(wrapped.reset());
  }

  /**
   * Create a new instance updated with multiple attributes removed
   *
   * @param keys the keys of the attributes to remove
   * @return a new instance with the attributes removed
   */
  @NonNull
  public Session removeAll(@NonNull String... keys) {
    return new Session(wrapped.removeAll(toScalaSeq(keys)));
  }

  /**
   * Check if the Session contains a given attribute key
   *
   * @param key the attribute key
   * @return true is the key is defined
   */
  public boolean contains(@NonNull String key) {
    return wrapped.contains(key);
  }

  /** @return if the Session's status is failure */
  public boolean isFailed() {
    return wrapped.isFailed();
  }

  /**
   * Create a new instance with the status forced to "succeeded"
   *
   * @return a new instance with the new status
   */
  @NonNull
  public Session markAsSucceeded() {
    return new Session(wrapped.markAsSucceeded());
  }

  /**
   * Create a new instance with the status forced to "failed"
   *
   * @return a new instance with the new status
   */
  @NonNull
  public Session markAsFailed() {
    return new Session(wrapped.markAsFailed());
  }

  /**
   * Provide the name of the scenario of the virtual user
   *
   * @return the virtual user's scenario name
   */
  public String scenario() {
    return wrapped.scenario();
  }

  /**
   * Provide the list of groups at the current position for the virtual user
   *
   * @return the list of groups, from shallowest to deepest
   */
  public List<String> groups() {
    return Sessions.groups(this);
  }

  /**
   * Provide the unique (for this injector) id of the virtual user
   *
   * @return the virtual user's id
   */
  public long userId() {
    return wrapped.userId();
  }

  public io.gatling.core.session.Session asScala() {
    return wrapped;
  }

  @Override
  public String toString() {
    return wrapped.toString();
  }
}
