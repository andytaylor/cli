package org.jboss.pnc.bacon.pig.endpoints;

import io.undertow.server.HttpServerExchange;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

public class EndpointHandler extends HttpServlet {

   public static Endpoints endpoints;

   @Override
   protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      String path = req.getRequestURI();
      BufferedReader reader = req.getReader();
      String json = reader.readLine();
      try {
         resp.setContentType("text/json");
         PrintWriter out = resp.getWriter();
         out.println(endpoints.addRequest(path, json));
      } catch (Exception e) {
         throw new ServletException(e);
      }
   }

}
