package org.jboss.pnc.bacon.pig.endpoints;

import javax.json.JsonObject;
import javax.json.JsonValue;

public interface Endpoint {

   JsonValue addRequest(JsonObject jsonObject);

   String getPath();
}
