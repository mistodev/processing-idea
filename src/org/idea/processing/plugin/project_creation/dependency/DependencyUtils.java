/*
 * Copyright (c) 2017  mistodev
 *
 * This file is part of "Processing IDEA plugin" and is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.idea.processing.plugin.project_creation.dependency;

import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.util.net.HttpConfigurable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DependencyUtils {

    public static final Pattern ARTIFACT_ROOT_VERSION_PATTERN = Pattern.compile("href=\"(.*)/\"");

    public static Map<Version, String> getAvailableArtifactVersions(String groupId, String artifactId, Collection<URL> repositories) throws IOException {
        Map<Version, String> availableVersions = new TreeMap<>(Version.descendingOrderComparator());

        final String relativePathToArtifact = groupId.replace(".", "/") + "/" + artifactId.replace(".", "/");

        String artifactLocation;
        for (final URL repository : repositories) {
            artifactLocation = repository.toExternalForm() + "/" + relativePathToArtifact;

            HttpURLConnection artifactRootConnection = HttpConfigurable.getInstance().openHttpConnection(artifactLocation);
            String artifactRootPage = StreamUtil.readText(artifactRootConnection.getInputStream(), StandardCharsets.UTF_8);

            Matcher artifactVersionMatcher = ARTIFACT_ROOT_VERSION_PATTERN.matcher(artifactRootPage);

            while (artifactVersionMatcher.find()) {
                String matchedVersion = artifactVersionMatcher.group(1);

                Version parsedVersion = Version.parseString(matchedVersion);

                if (parsedVersion != Version.INVALID) {
                    String versionUrl = repository.toExternalForm() + "/" + relativePathToArtifact + "/" + parsedVersion.toString();
                    availableVersions.put(parsedVersion, versionUrl);
                }
            }

            artifactRootConnection.disconnect();

            if (! availableVersions.isEmpty()) {
                break;
            }
        }

        return availableVersions;
    }
}
