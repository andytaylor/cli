/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bacon.pig.impl.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.jboss.pnc.bacon.pig.impl.pnc.GitRepoInspector;
import org.jboss.pnc.bacon.pig.impl.pnc.PncBuildConfig;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 11/28/17
 */
@Data
public class BuildConfig {
    private String name;
    private String project;
    private String buildScript;
    private String scmUrl;
    private String externalScmUrl;
    private String scmRevision;
    private String description;
    private Integer environmentId;
    private List<String> dependencies = new ArrayList<>();
    private Set<String> customPmeParameters = new TreeSet<>();
    private Boolean branchModified;

    public void setDefaults(BuildConfig defaults) {
        try {
            for (Field f : BuildConfig.class.getDeclaredFields()) {
                f.setAccessible(true);
                if (Collection.class.isAssignableFrom(f.getType())) {
                    Collection<?> defaultValues = (Collection<?>) f.get(defaults);
                    //noinspection unchecked
                    ((Collection) f.get(this)).addAll(defaultValues);
                } else {
                    if (f.get(this) == null) {
                        f.set(this, f.get(defaults));
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to set default values for " + this + " from " + defaults);
        }
    }

    public static Map<String, BuildConfig> mapByName(List<BuildConfig> newConfigs) {
        return newConfigs.stream()
                .collect(toMap(BuildConfig::getName, identity()));
    }

    public void sanitizebuildScript() {
        buildScript = buildScript.trim();
    }

    @JsonIgnore
    public boolean isTheSameAs(PncBuildConfig old) {
        return old != null
                && StringUtils.equals(name, old.getName())
                && StringUtils.equals(project, old.getProject())
                && StringUtils.equals(buildScript, old.getBuildScript())
                && StringUtils.equals(scmRevision, old.getScmRevision())
                && environmentId.equals(old.getEnvironmentId())
                && customPmeParameters.equals(old.getCustomPmeParameters())
                && urlsEqual(old)
                && !isBranchModified(old);
    }

    private synchronized boolean isBranchModified(PncBuildConfig oldVersion) {
        if (branchModified == null) {
            branchModified = GitRepoInspector.isModifiedBranch(oldVersion.getId(), oldVersion.getScmUrl(), getScmRevision());
        }
        return branchModified;
    }

    private boolean urlsEqual(PncBuildConfig old) {
        return StringUtils.equals(externalScmUrl, old.getExternalScmUrl()) ||
                StringUtils.equals(scmUrl, old.getScmUrl());
    }

    @JsonIgnore
    public String getGenericParameters(PncBuildConfig oldConfig, boolean forceRebuild) {
        String oldForceValue = oldConfig == null ? "" : oldConfig.getBuildForceParameter();
        String forceValue = forceRebuild ? randomAlphabetic(5) : oldForceValue;
        String forceParameter = ", '" + PncBuildConfig.BUILD_FORCE + "': '" + forceValue + "'";
        String dependencyExclusions = String.join(" ", customPmeParameters);
        if (dependencyExclusions.isEmpty()) {
            return "\"{'CUSTOM_PME_PARAMATERS':''" + forceParameter + "}\"";
        } else {
            return String.format("\"{'CUSTOM_PME_PARAMETERS':'%s'" + forceParameter + "}\"", dependencyExclusions);
        }
    }

    public String toCreateParams(Integer projectId, Integer repoId, Integer versionId) {
        return String.format("\"%s\" %s %s %s %s \"%s\" --product-version-id %d --generic-parameters %s",
                name,
                projectId,
                environmentId,
                repoId,
                scmRevision,
                buildScript,
                versionId,
                getGenericParameters(null, false)
        );
    }

    public String toUpdateParams(Integer projectId, PncBuildConfig oldVersion) {
        // TODO: updating versionId?
        return " --scm-revision " + getScmRevision() +
                " --build-script \"" + getBuildScript() + "\"" + " --generic-parameters " +
                getGenericParameters(oldVersion, isBranchModified(oldVersion)) +
                paramIfChanged(" --name ", name, oldVersion.getName()) +
                paramIfChanged(" --project ", project, oldVersion.getProject(), projectId) +
                paramIfChanged(" --environment ", environmentId, oldVersion.getEnvironmentId());
    }

    private <T, V> String paramIfChanged(String param, V newValue, V oldValue, T mappedValue) {
        if (!newValue.equals(oldValue)) {
            return param + mappedValue;
        }
        return "";
    }

    private <V> String paramIfChanged(String param, V newValue, V oldValue) {
        return paramIfChanged(param, newValue, oldValue, newValue);
    }

    //Work around for NCL-3670
    public String getShortScmURIPath() {
        String scmUrl = getScmUrl() != null ? getScmUrl() : getExternalScmUrl();
        try {
            URI scmUri = new URI(scmUrl);
            return scmUri.getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid scm URI: " + getScmUrl(), e);
        }

    }

    public void validate(List<String> errors) {
        // TODO!
    }

    public boolean matchesRepository(Map<String, ?> repositoryAsMap) {
        String currentInternalUrl = (String) repositoryAsMap.get("internal_url");

        // currentInternalUrl cannot be null, if the url from the config is not null, they have to be the same
        if (scmUrl != null) {
            return areSameRepoUrls(scmUrl, currentInternalUrl);
        }

        String currentExternalUrl = (String) repositoryAsMap.get("external_url");
        // if the internal scm url is not provided in the config, check if external scm urls are the same
        return externalScmUrl != null && areSameRepoUrls(currentExternalUrl, externalScmUrl);
    }

    private boolean areSameRepoUrls(String scmUrl1, String scmUrl2) {
        String normalizedUrl1 = normalize(scmUrl1);
        String normalizedUrl2 = normalize(scmUrl2);
        return normalizedUrl1.equals(normalizedUrl2);
    }

    private String normalize(String url) {
        if (!url.endsWith(".git")) {
            url += ".git";
        }

        if (url.startsWith("https")) {
            url = url.replaceFirst("https", "http");
        }

        return url;
    }

    public String toCreateParamsForExternalScm(Integer projectId, int versionId) {
        return String.format("%s %s \"%s\" %d %d \"%s\" -pvi %d --generic-parameters %s",
                externalScmUrl,
                scmRevision,
                name,
                projectId,
                environmentId,
                buildScript,
                versionId,
                getGenericParameters(null, false)
        );
    }
}
