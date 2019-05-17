package org.jboss.pnc.bacon.pig.endpoints;

import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.CommunityDependency;
import org.jboss.pnc.bacon.pig.impl.documents.sharedcontent.da.DependencyState;
import org.jboss.pnc.bacon.pig.util.JsonLoader;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class DaReportGavEndpoint implements Endpoint {

   public static final String DA_REPORTS_GAVS = "/da/rest/v-1/reports/lookup/gavs";
   private CommunityDependency reply;

   @Override
   public JsonValue addRequest(JsonObject jsonObject) {
      return getJsonObject(reply);
   }

   public String getPath() {
      return DA_REPORTS_GAVS;
   }

   public void setReply(CommunityDependency reply) {
      this.reply = reply;
   }

   private JsonValue getJsonObject(CommunityDependency expected) {
      JsonObjectBuilder gavBuilder = JsonLoader.createObjectBuilder();
      JsonArrayBuilder availableVersionsBuilder = JsonLoader.createArrayBuilder();
      String[] availableVersions = expected.getAvailableVersions().split(",");
      for (String availableVersion : availableVersions) {
         availableVersionsBuilder.add(availableVersion);
      }
      gavBuilder.add("artifactId", expected.getArtifactId())
            .add("groupId", expected.getGroupId())
            .add("version", expected.getVersion())
            .add("availableVersions", availableVersionsBuilder);
      JsonArrayBuilder reply = JsonLoader.createArrayBuilder().add(gavBuilder);
      return reply.build();
   }
}
