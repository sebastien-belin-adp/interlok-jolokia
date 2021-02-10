package com.adaptris.core.management.jolokia;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.core.management.jetty.JettyServerComponent;
import com.adaptris.core.management.webserver.JettyServerManager;
import com.adaptris.core.management.webserver.WebServerManagementUtil;

class JettyServerWrapperImpl implements JettyServerWrapper {

  protected transient Logger log = LoggerFactory.getLogger(this.getClass().getName());

  private Server server;

  JettyServerWrapperImpl(final Server server) {
    this.server = server;
  }

  @Override
  public void register() {
    final JettyServerManager jettyManager = (JettyServerManager) WebServerManagementUtil.getServerManager();
    jettyManager.addServer(server);
  }

  @Override
  public void start() {
    try {
      if (server.isStopped()) {
        server.start();
        log.debug("{} Started", JettyServerComponent.class.getSimpleName());
      }
    } catch (final Exception ex) {
      log.error("Exception while starting Jetty", ex);
    }
  }

  @Override
  public void stop() {
    try {
      server.stop();
      server.join();
      log.debug("{} Stopped", JettyServerComponent.class.getSimpleName());
    } catch (final Exception ex) {
      log.error("Exception while stopping Jetty", ex);
    }

  }

  @Override
  public void destroy() {
    try {
      final JettyServerManager jettyManager = (JettyServerManager) WebServerManagementUtil.getServerManager();
      jettyManager.removeServer(server);
      server.destroy();
      log.debug("{} Destroyed", JettyServerComponent.class.getSimpleName());
    } catch (final Exception ex) {
      log.error("Exception while destroying Jetty", ex);
    }
  }

}