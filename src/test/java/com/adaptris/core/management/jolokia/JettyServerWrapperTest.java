package com.adaptris.core.management.jolokia;

import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.adaptris.core.management.webserver.JettyServerManager;

public class JettyServerWrapperTest {

  @Test
  public void testDefaultMethods() throws Exception {
    JettyServerWrapper wrapper = new JettyServerWrapper() {
    };
    wrapper.register();
    wrapper.start();
    wrapper.stop();
    wrapper.destroy();
  }

  @Test
  public void testLifecycle() throws Exception {
    Server server = Mockito.mock(Server.class);
    Mockito.when(server.isStarted()).thenReturn(true);
    Mockito.when(server.isStopped()).thenReturn(true);
    try {
      JettyServerWrapperImpl wrapper = new JettyServerWrapperImpl(server);
      wrapper.register();
      wrapper.start();
    } finally {
      stopAndDestroy(server);
      JettyServerManager.getInstance().removeServer(JettyServerWrapperImpl.SERVER_ID);
    }
  }

  @Test
  public void testLifecycle_StartNotStopped() throws Exception {
    Server server = Mockito.mock(Server.class);
    Mockito.when(server.isStopped()).thenReturn(false);
    try {
      JettyServerWrapperImpl wrapper = new JettyServerWrapperImpl(server);
      wrapper.register();
      wrapper.start();
    } finally {
      stopAndDestroy(server);
      JettyServerManager.getInstance().removeServer(JettyServerWrapperImpl.SERVER_ID);
    }
  }

  @Test
  public void testLifecycle_HasException() throws Exception {
    Server server = Mockito.mock(Server.class);
    Mockito.when(server.isStopped()).thenReturn(true);
    Mockito.doThrow(new RuntimeException()).when(server).start();
    Mockito.doThrow(new RuntimeException()).when(server).stop();
    Mockito.doThrow(new RuntimeException()).when(server).destroy();
    try {
      JettyServerWrapperImpl wrapper = new JettyServerWrapperImpl(server);
      wrapper.register();
      wrapper.start(); // throws an exception which is then eaten quietly
      wrapper.stop();
      wrapper.destroy();
    } finally {
      JettyServerManager.getInstance().removeServer(JettyServerWrapperImpl.SERVER_ID);
    }
  }

  static void stopAndDestroy(Server c) {
    if (c != null) {
      executeQuietly(c::stop);
      executeQuietly(c::destroy);
    }
  }

  private static void executeQuietly(Runner r) {
    try {
      r.execute();
    } catch (Exception ignored) {

    }
  }

  @FunctionalInterface
  public interface Runner {
    void execute() throws Exception;
  }

}
