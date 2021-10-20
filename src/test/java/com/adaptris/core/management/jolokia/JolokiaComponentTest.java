package com.adaptris.core.management.jolokia;

import com.adaptris.core.management.webserver.JettyServerManager;
import com.adaptris.interlok.junit.scaffolding.util.PortManager;
import java.time.Duration;
import java.util.Properties;
import org.awaitility.Awaitility;
import org.junit.Test;

public class JolokiaComponentTest {

  static final Duration MAX_STARTUP_WAIT = Duration.ofSeconds(5);
  static final Duration STARTUP_POLL = Duration.ofMillis(100);

  @Test
  public void testFromProperties() throws Exception {
    JolokiaComponent jolokia = new JolokiaComponent();
    int portForServer = PortManager.nextUnusedPort(18080);
    try {
      Properties jettyConfig = new Properties();
      jettyConfig.setProperty(FromProperties.JOLOKIA_PORT_CFG_KEY, String.valueOf(portForServer));
      jolokia.init(jettyConfig);
      jolokia.start();
      Thread.sleep(250);
      final JettyServerManager mgr = JettyServerManager.getInstance();
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
      jettyConfig.setProperty(FromProperties.JOLOKIA_PORT_CFG_KEY, String.valueOf(portForServer));
      jettyConfig.setProperty(FromProperties.JOLOKIA_USERNAME_CFG_KEY, "admin");
      jettyConfig.setProperty(FromProperties.JOLOKIA_PASSWORD_CFG_KEY, "admin");
      jolokia.init(jettyConfig);
      jolokia.start();
      Thread.sleep(250);
      final JettyServerManager mgr = JettyServerManager.getInstance();
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
