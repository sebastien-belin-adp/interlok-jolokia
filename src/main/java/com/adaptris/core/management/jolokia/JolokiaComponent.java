/*
 * Copyright 2015 Adaptris Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.adaptris.core.management.jolokia;

import java.util.Properties;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import com.adaptris.core.management.MgmtComponentImpl;
import com.adaptris.core.util.ManagedThreadFactory;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * This class can be used for configuring and starting a Jetty webserver for the adapter.
 * <p>
 * You must provide the jetty config file in the bootstrap.properties file for a Jetty instance to startup.<br/>
 * See the Jetty Guide for more information on configuration options.
 * <p>
 * For adding webapps to the server the jettyWebappBase parameter can be used.
 *
 */
@XStreamAlias("jolokia")
public class JolokiaComponent extends MgmtComponentImpl {

  private static final long STARTUP_WAIT = TimeUnit.SECONDS.toMillis(60L);

  private Properties properties;

  private JettyServerWrapper wrapper = new JettyServerWrapper() {
  };

  public JolokiaComponent() {
  }

  @Override
  public void init(final Properties properties) throws Exception {
    this.properties = properties;
  }

  /**
   * Had to do some tricks for proper classloading.
   */
  @Override
  public void start() throws Exception {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    final CyclicBarrier barrier = new CyclicBarrier(2);
    // This is to make sure we don't break the barrier before the real delay is up.
    //
    final long barrierDelay = STARTUP_WAIT;
    ManagedThreadFactory.createThread("JolokiaEmbeddedJettyStart", new Runnable() {
      @Override
      public void run() {
        try {
          log.debug("Creating Jetty wrapper");
          Thread.currentThread().setContextClassLoader(classLoader);
          wrapper = new JettyServerWrapperImpl(ServerBuilder.build(properties));
          wrapper.register();
          // Wait until at least the server is registered.
          barrier.await(barrierDelay, TimeUnit.MILLISECONDS);
          wrapper.start();
        } catch (final Exception ex) {
          log.error("Could not create wrapper", ex);
        }
      }
    }).start();
    barrier.await(barrierDelay, TimeUnit.MILLISECONDS);
  }

  @Override
  public void stop() {
    wrapper.stop();
  }

  @Override
  public void destroy() throws Exception {
    wrapper.destroy();
  }

}
