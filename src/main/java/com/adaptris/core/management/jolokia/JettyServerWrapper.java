package com.adaptris.core.management.jolokia;

public interface JettyServerWrapper {

  default void start() throws Exception {
  };

  default void stop() {
  };

  default void destroy() {
  };

  default void register() {
  };

}
