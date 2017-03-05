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

import com.intellij.AbstractBundle;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class DependencyResolutionBundle extends AbstractBundle {

    public static String key(@NotNull @PropertyKey(resourceBundle = PATH_TO_BUNDLE) String key, @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    private static final Logger logger = com.intellij.openapi.diagnostic.Logger.getInstance(DependencyResolutionBundle.class);

    private static final String PATH_TO_BUNDLE = "DependencyResolutionBundle";
    private static final AbstractBundle INSTANCE = new DependencyResolutionBundle();

    private DependencyResolutionBundle() {
        super(PATH_TO_BUNDLE);
    }

    public static Collection<URL> getCentralRepos() {
        Collection<String> centralRepoStrings = Arrays.asList(DependencyResolutionBundle.key("artifact_repo_urls")
                                                              .split(","));

        Collection<URL> centralRepoUrls = new ArrayList<>(centralRepoStrings.size());

        for (String repoStr : centralRepoStrings) {
            try {
                centralRepoUrls.add(new URL(repoStr));
            } catch (MalformedURLException mue) {
                logger.error(mue);
            }
        }

        return centralRepoUrls;
    }
}
