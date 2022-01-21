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

package io.gatling.javaapi.jms;

import javax.annotation.Nonnull;
import javax.jms.ConnectionFactory;

/**
 * DSL for building JNDI ConnectionFactory configurations
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class JmsJndiConnectionFactoryBuilder {

  private final io.gatling.jms.jndi.JmsJndiConnectionFactoryBuilder wrapped;

  private JmsJndiConnectionFactoryBuilder(
      io.gatling.jms.jndi.JmsJndiConnectionFactoryBuilder wrapped) {
    this.wrapped = wrapped;
  }

  public ConnectionFactory build() {
    return wrapped.build();
  }

  public static final class Base {

    public static final Base INSTANCE = new Base();

    private Base() {}

    /**
     * Define the ConnectionFactory JNDI name
     *
     * @param cfn the name
     * @return the next DSL step
     */
    public Url connectionFactoryName(String cfn) {
      return new Url(io.gatling.jms.Predef.jmsJndiConnectionFactory().connectionFactoryName(cfn));
    }
  }

  public static final class Url {
    private final io.gatling.jms.jndi.JmsJndiConnectionFactoryBuilder.Url wrapped;

    private Url(io.gatling.jms.jndi.JmsJndiConnectionFactoryBuilder.Url wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the ConnectionFactory url
     *
     * @param url the url
     * @return the next DSL step
     */
    @Nonnull
    public ContextFactory url(@Nonnull String url) {
      return new ContextFactory(wrapped.url(url));
    }
  }

  public static final class ContextFactory {
    private final io.gatling.jms.jndi.JmsJndiConnectionFactoryBuilder.ContextFactory wrapped;

    private ContextFactory(
        io.gatling.jms.jndi.JmsJndiConnectionFactoryBuilder.ContextFactory wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the credentials to populate the {@link javax.naming.Context#SECURITY_PRINCIPAL} and
     * {@link javax.naming.Context#SECURITY_CREDENTIALS} properties.
     *
     * @param user the user
     * @param password the password
     * @return a new instance
     */
    @Nonnull
    public ContextFactory credentials(@Nonnull String user, @Nonnull String password) {
      return new ContextFactory(wrapped.credentials(user, password));
    }

    /**
     * Define some property
     *
     * @param key the property key
     * @param value the property value
     * @return a new instance
     */
    @Nonnull
    public ContextFactory property(@Nonnull String key, @Nonnull String value) {
      return new ContextFactory(wrapped.property(key, value));
    }

    /**
     * Define the ConnectionFactory class
     *
     * @param clazz the class
     * @return a usable {@link JmsJndiConnectionFactoryBuilder}
     */
    @Nonnull
    public JmsJndiConnectionFactoryBuilder contextFactory(@Nonnull String clazz) {
      return new JmsJndiConnectionFactoryBuilder(wrapped.contextFactory(clazz));
    }
  }
}
