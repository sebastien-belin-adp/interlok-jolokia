package com.adaptris.core.management.jolokia;

import com.adaptris.core.management.webserver.JettyServerManager;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JettyServerWrapperImpl implements JettyServerWrapper {

  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());
  private static final String FRIENDLY_NAME = JolokiaComponent.class.getSimpleName();
  protected static final String SERVER_ID = JolokiaComponent.class.getSimpleName();

  private Server server;

  JettyServerWrapperImpl(final Server server) {
    this.server = server;
  }

  @Override
  public void register() {
    JettyServerManager.getInstance().addServer(SERVER_ID, server);
  }

  @Override
  public void start() {
    try {
      if (server.isStopped()) {
        server.start();
        log.debug("{} Started", FRIENDLY_NAME);
      }
    } catch (final Exception ex) {
      log.error("Exception while starting Jolokia(Jetty)", ex);
    }
  }

  @Override
  public void stop() {
    try {
      server.stop();
      server.join();
      log.debug("{} Stopped", FRIENDLY_NAME);
    } catch (final Exception ex) {
      log.error("Exception while stopping Jolokia(Jetty)", ex);
    }

  }

  @Override
  public void destroy() {
    try {
      JettyServerManager.getInstance().removeServer(SERVER_ID);
      server.destroy();
      log.debug("{} Destroyed", FRIENDLY_NAME);
    } catch (final Exception ex) {
      log.error("Exception while destroying Jolokia(Jetty)", ex);
    }
  }
}