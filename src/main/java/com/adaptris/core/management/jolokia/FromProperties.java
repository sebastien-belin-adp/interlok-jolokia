/*
 * Copyright 2017 Adaptris Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adaptris.core.management.jolokia;

import java.util.Properties;
import javax.servlet.ServletContext;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.Authenticator.AuthConfiguration;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

/**
 * Build a jetty server from properties.
 */
final class FromProperties extends ServerBuilder {

  private final static String DEFAULT_JOLOKIA_JETTY_PORT = "8081";

  private final static String JOLOKIA_SERVLET_PATH = "/*";
  private final static String DEFAULT_JOLOKIA_CONTEXT_PATH = "/jolokia";
  public static final String JOLOKIA_PORT_CFG_KEY = "jolokiaPort";
  public final static String JOLOKIA_CONTEXT_PATH_CFG_KEY = "jolokiaContextPath";
  public final static String JOLOKIA_USERNAME_CFG_KEY = "jolokiaUsername";
  public final static String JOLOKIA_PASSWORD_CFG_KEY = "jolokiaPassword";



  public FromProperties(Properties initialConfig) {
    super(initialConfig);
  }

  @Override
  protected Server build() throws Exception {
    log.trace("Create Server from Jolokia Properties");
    final Server server = createSimpleServer();

    configureThreadPool(server.getThreadPool());
    server.addConnector(createConnector(server));

    // Setting up handler collection
    final HandlerCollection handlerCollection = new HandlerCollection();

    final ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
    contextHandlerCollection.addHandler(servletContextHandler());

    handlerCollection.addHandler(securityHandler(contextHandlerCollection));
    // handlerCollection.addHandler(new DefaultHandler());

    server.setHandler(handlerCollection);
    return server;
  }

  private SecurityHandler securityHandler(Handler handler) {
    ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();

    if (containsConfigItem(JOLOKIA_USERNAME_CFG_KEY)) {
      Authenticator authenticator = new BasicAuthenticator();
      securityHandler.setAuthenticator(authenticator);

      // LoginService loginService = new HashLoginServiceFactory("jolokia", "./config/jetty.xml").retrieveLoginService();
      LoginService loginService = new JolokiaLoginService(getConfigItem(JOLOKIA_USERNAME_CFG_KEY), getConfigItem(JOLOKIA_PASSWORD_CFG_KEY));
      securityHandler.setLoginService(loginService);

      ConstraintMapping constraintMapping = constraint();
      securityHandler.addConstraintMapping(constraintMapping);
    } else {
      // https://github.com/adaptris/interlok/pull/788
      securityHandler.setAuthenticatorFactory(new Authenticator.Factory() {

        @Override
        public Authenticator getAuthenticator(Server server, ServletContext context,
            AuthConfiguration configuration, IdentityService identityService,
            LoginService loginService) {
          return null;
        }
        
      });
    }

    securityHandler.setHandler(handler);

    return securityHandler;
  }

  private ConstraintMapping constraint() {
    Constraint constraint = new Constraint();
    constraint.setName("jolokia");
    constraint.setRoles(new String[] { "jolokia" });
    constraint.setAuthenticate(true);

    ConstraintMapping constraintMapping = new ConstraintMapping();
    constraintMapping.setConstraint(constraint);
    constraintMapping.setPathSpec(JOLOKIA_SERVLET_PATH);
    return constraintMapping;
  }

  private Handler servletContextHandler() {
    ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletContextHandler.setContextPath(getConfigItem(JOLOKIA_CONTEXT_PATH_CFG_KEY, DEFAULT_JOLOKIA_CONTEXT_PATH));
    servletContextHandler.addServlet(jolokiaServlet(), JOLOKIA_SERVLET_PATH);
    return servletContextHandler;
  }

  private ServletHolder jolokiaServlet() {
    ServletHolder servletHolder = new ServletHolder("jolokia-agent", org.jolokia.http.AgentServlet.class);

    /*
     * Debugging state after startup. Can be changed via the Config MBean during runtime.
     * Default false
     */
    // servletHolder.setInitParameter("debug", "false");

    /*
     * Entries to keep in the history. Can be changed during runtime via the config MBean.
     * Default 10
     */
    // servletHolder.setInitParameter("historyMaxEntries", "10");

    /*
     * Maximum number of entries to keep in the local debug history if switched on. Can be change via the config MBean during runtime.
     * Default 100
     */
    // servletHolder.setInitParameter("debugMaxEntries", "100");

    /*
     * Maximum depth when traversing bean properties. If set to 0, depth checking is disabled
     * Default 0
     */
    servletHolder.setInitParameter("maxDepth", "15");

    /*
     * Maximum size of collections returned when serializing to JSON. When set to 0 collections are not truncated.
     * Default 0
     */
    // servletHolder.setInitParameter("maxCollectionSize", "0");

    /*
     * Maximum number of objects which is traversed when serializing a single response. Use this as airbag to avoid boosting your memory and
     * network traffic. Nevertheless when set to 0 not limit is used.
     * Default 0
     */
    // servletHolder.setInitParameter("maxObjects", "0");

    /*
     * Options specific for certain application server detectors. Detectors can evaluate these options and perform a specific initialization
     * based on these options. The value is a JSON object with the detector's name as key and the options as value. E.g. '{glassfish:
     * {bootAmx: false}}' would prevent the booting of the AMX subsystem on a glassfish with is done by default.
     * Default null
     */
    // servletHolder.setInitParameter("detectorOptions", "{}");

    /*
     * This option specifies in which order the key-value properties within ObjectNames as returned by "list" or "search" are returned. By
     * default this is the so called 'canonical order' in which the keys are sorted alphabetically. If this option is set to "false", then
     * the natural order is used, i.e. the object name as it was registered. This option can be overridden with a query parameter of the
     * same name.
     * Default true
     */
    // servletHolder.setInitParameter("canonicalNaming", "true");

    /*
     * Whether to include a stacktrace of an exception in case of an error. By default it it set to "true" in which case the stacktrace is
     * always included. If set to "false", no stacktrace is included. If the value is "runtime" a stacktrace is only included for
     * RuntimeExceptions. This global option can be overridden with a query parameter.
     * Default true
     * For 2.0: Default to false
     */
    // servletHolder.setInitParameter("includeStackTrace", "true");

    /*
     * When this parameter is set to "true", then an exception thrown will be serialized as JSON and included in the response under the key
     * "error_value". By default it is "false". This global option can be overridden by a query parameter of the same name.
     * Default false
     */
    // servletHolder.setInitParameter("serializeException", "false");

    /*
     * If discoveryEnabled is set to true, then this servlet will listen for multicast discovery request and responds with its agent URL and
     * other server specific information. Instead of setting this confog variable, discovery can be also enabled via the system property
     * "jolokia.discoveryEnabled" or the environment variable "JOLOKIA_DISCOVERY_ENABLED".
     *
     * In addition the config parameter "discoveryAgentUrl" can be used to set the the agent's URL. By default, auto detection (after the
     * first request was processed by the servlet)) of the URL is used. If the URL is set, then discovery is automatically enabled (i.e.
     * there is no need to set "discoveryEnabled=true"). This configuration option is especially useful if the WAR is used in a proxy setup.
     * Instead of setting the URL here, it can be set also either via the system property "jolokia.discoveryAgentUrl" or the environment
     * variable "JOLOKIA_DISCOVERY_AGENT_URL".
     */
    servletHolder.setInitParameter("discoveryEnabled", "false");

    /*
     * The MIME type to return for the response. By default, this is text/plain, but it can be useful for some tools to change it to
     * application/json. Init parameters can be used to change the default mime type. Only text/plain and application/json are allowed. For
     * any other value Jolokia will fallback to text/plain. A request parameter overrides a global
     * configuration.
     * Default text/plain
     */
    servletHolder.setInitParameter("mimeType", "application/json");

    return servletHolder;
  }

  private Server createSimpleServer() {
    final Server server = new Server();
    // Setting up extra options
    server.setStopAtShutdown(true);
    server.setStopTimeout(5000);
    server.setDumpAfterStart(false);
    server.setDumpBeforeStop(false);
    return server;
  }

  private void configureThreadPool(ThreadPool threadPool) {
    if (threadPool instanceof QueuedThreadPool) {
      QueuedThreadPool qp = (QueuedThreadPool) threadPool;
      qp.setMinThreads(10);
      qp.setMaxThreads(200);
      qp.setDetailedDump(false);
      qp.setIdleTimeout(60000);
    }
  }

  private ServerConnector createConnector(Server server) {
    ServerConnector connector = new ServerConnector(server, -1, -1,
        new HttpConnectionFactory(configure(new HttpConfiguration()), HttpCompliance.RFC2616));
    connector.setPort(Integer.parseInt(getConfigItem(JOLOKIA_PORT_CFG_KEY, DEFAULT_JOLOKIA_JETTY_PORT)));
    return connector;
  }

  private HttpConfiguration configure(final HttpConfiguration httpConfig) {
    httpConfig.setSecureScheme("http");
    // httpConfig.setSecurePort(8443);
    httpConfig.setSecurePort(9443);
    httpConfig.setOutputBufferSize(32768);
    httpConfig.setOutputAggregationSize(8192);
    httpConfig.setRequestHeaderSize(8192);
    httpConfig.setResponseHeaderSize(8192);
    httpConfig.setSendDateHeader(true);
    httpConfig.setSendServerVersion(true);
    httpConfig.setHeaderCacheSize(512);
    httpConfig.setDelayDispatchUntilContent(true);
    httpConfig.setMaxErrorDispatches(10);
    // httpConfig.setBlockingTimeout(-1);
    httpConfig.setMinRequestDataRate(-1);
    httpConfig.setMinResponseDataRate(-1);
    httpConfig.setPersistentConnectionsEnabled(true);
    return httpConfig;
  }

}
