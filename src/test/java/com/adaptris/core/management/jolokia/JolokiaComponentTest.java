package com.adaptris.core.management.jolokia;

import java.time.Duration;
import java.util.Properties;

import org.awaitility.Awaitility;
import org.junit.Test;

import com.adaptris.core.management.webserver.ServerManager;
import com.adaptris.core.management.webserver.WebServerManagementUtil;
import com.adaptris.interlok.junit.scaffolding.util.PortManager;

public class JolokiaComponentTest {

  private static final Duration MAX_STARTUP_WAIT = Duration.ofSeconds(5);
  private static final Duration STARTUP_POLL = Duration.ofMillis(100);

  @Test
  public void testFromProperties() throws Exception {
    JolokiaComponent jolokia = new JolokiaComponent();
    int portForServer = PortManager.nextUnusedPort(18080);
    try {
      Properties jettyConfig = new Properties();
      jettyConfig.setProperty(ServerBuilder.JOLOKIA_PORT_CFG_KEY, String.valueOf(portForServer));
      jolokia.init(jettyConfig);
      jolokia.setClassLoader(Thread.currentThread().getContextClassLoader());
      jolokia.start();
      Thread.sleep(250);
      final ServerManager mgr = WebServerManagementUtil.getServerManager();
      Awaitility.await().atMost(MAX_STARTUP_WAIT).with().pollInterval(STARTUP_POLL).until(() -> mgr.isStarted());
    } finally {
      stopAndDestroy(jolokia);
      PortManager.release(portForServer);
    }
  }

  @Test
  public void testFromProperties_withUsernameAndPassword() throws Exception {
    JolokiaComponent jolokia = new JolokiaComponent();
    int portForServer = PortManager.nextUnusedPort(18080);
    try {
      Properties jettyConfig = new Properties();
      jettyConfig.setProperty(ServerBuilder.JOLOKIA_PORT_CFG_KEY, String.valueOf(portForServer));
      jettyConfig.setProperty(FromProperties.JOLOKIA_USERNAME_CFG_KEY, "admin");
      jettyConfig.setProperty(FromProperties.JOLOKIA_PASSWORD_CFG_KEY, "admin");
      jolokia.init(jettyConfig);
      jolokia.setClassLoader(Thread.currentThread().getContextClassLoader());
      jolokia.start();
      Thread.sleep(250);
      final ServerManager mgr = WebServerManagementUtil.getServerManager();
      Awaitility.await().atMost(MAX_STARTUP_WAIT).with().pollInterval(STARTUP_POLL).until(() -> mgr.isStarted());
    } finally {
      stopAndDestroy(jolokia);
      PortManager.release(portForServer);
    }
  }

  private void stopAndDestroy(JolokiaComponent c) {
    try {
      if (c != null) {
        c.stop();
        c.destroy();
      }
    } catch (Exception e) {

    }
  }
}
