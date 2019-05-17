package org.jboss.pnc.bacon.pig.impl;

import io.undertow.Handlers;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import org.jboss.pnc.bacon.pig.endpoints.DaReportGavEndpoint;
import org.jboss.pnc.bacon.pig.endpoints.EndpointHandler;
import org.jboss.pnc.bacon.pig.endpoints.Endpoints;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import io.undertow.Undertow;

import java.util.List;

public class PigTestBase {


   private static Undertow server;
   protected static Endpoints endpoints;

   public static final String MYAPP = "/myapp";

   @BeforeAll
   private static void startServer() throws Exception {
      endpoints = new Endpoints();
      EndpointHandler.endpoints = endpoints;
      DeploymentInfo servletBuilder = Servlets.deployment()
            .setClassLoader(PigTestBase.class.getClassLoader())
            .setContextPath("/myapp")
            .setDeploymentName("test.war")
            .addServlets(
                  Servlets.servlet("MessageServlet", EndpointHandler.class)
                        .addInitParam("message", "Hello World")
                        .addMapping("/*"),
                  Servlets.servlet("MyServlet", EndpointHandler.class)
                        .addInitParam("message", "MyServlet")
                        .addMapping("/myservlet"));

      DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
      manager.deploy();

      PathHandler path = Handlers.path(Handlers.redirect(DaReportGavEndpoint.DA_REPORTS_GAVS))
            .addPrefixPath(DaReportGavEndpoint.DA_REPORTS_GAVS, manager.start());

      server = Undertow.builder()
            .addHttpListener(8080, "localhost")
            .setHandler(path)
            .build();
      server.start();
   }

   @AfterEach
   private void reset() {
      endpoints.clear();
   }

   @AfterAll
   private static void stopServer() {
      server.stop();
      server = null;
   }

}
