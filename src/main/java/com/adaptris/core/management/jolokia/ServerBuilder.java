/*
 * Copyright 2017 Adaptris Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.adaptris.core.management.jolokia;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ServerBuilder {


  private enum Builder {
    /*XML() {
      @Override
      public boolean canBuild(Properties p) {
        return p.containsKey(WEB_SERVER_CONFIG_FILE_NAME_CGF_KEY);
      }

      @Override
      public ServerBuilder builder(Properties cfg) {
        return new FromXmlConfig(cfg);
      }

    },*/
    PROPERTIES() {
      @Override
      public boolean canBuild(Properties p) {
        return true; // p.containsKey(JOLOKIA_PORT_CFG_KEY);
      }

      @Override
      public ServerBuilder builder(Properties cfg) {
        return new FromProperties(cfg);
      }
    }/*,
    FAILSAFE() {
      @Override
      public boolean canBuild(Properties p) {
        return true;
      }
      @Override
      public ServerBuilder builder(Properties cfg) {
        return new FromClasspath(cfg);
      }
    }*/;

    public abstract boolean canBuild(Properties p);

    public abstract ServerBuilder builder(Properties p);

  }

  private static List<Builder> FACTORIES = Collections
      .unmodifiableList(Arrays.asList(Builder.PROPERTIES/* , Builder.XML, Builder.FAILSAFE */));

  protected static final Logger log = LoggerFactory.getLogger(JolokiaComponent.class);
  private Properties initialProperties;

  protected ServerBuilder(Properties p) {
    initialProperties = p;
  }

  protected abstract Server build() throws Exception;

  protected static final Server build(final Properties config) throws Exception {
    return FACTORIES.stream().filter((b) -> b.canBuild(config)).findFirst().get().builder(config).build();
  }

  protected Properties getConfig() {
    return initialProperties;
  }

  protected boolean containsConfigItem(String key) {
    return getConfig().containsKey(key);
  }

  protected String getConfigItem(String key) {
    return getConfigItem(key, null);
  }

  protected String getConfigItem(String key, String defaultValue) {
    return getConfig().getProperty(key, defaultValue);
  }
}
