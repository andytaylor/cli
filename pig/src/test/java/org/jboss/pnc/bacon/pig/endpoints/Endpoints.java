package org.jboss.pnc.bacon.pig.endpoints;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class Endpoints {

   private  Map<String, Endpoint> endpoints = new HashMap<>();

   public  String addRequest(String path, String json){
      JsonObject jsonObject = Json.createReader(new StringReader(json)).readObject();
      Endpoint endpoint = endpoints.get(path);
      JsonValue reply = endpoint.addRequest(jsonObject);
      return reply.toString();
   }

   public  void addEndpoint(Endpoint endpoint) {
      endpoints.put(endpoint.getPath(), endpoint);
   }

   public void clear() {
      endpoints.clear();
   }
}
