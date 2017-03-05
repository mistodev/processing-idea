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

package org.idea.processing.plugin;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.io.LocalFileFinder;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum ProcessingPluginUtil {
    INSTANCE;

    public Collection<VirtualFile> filterFilesAtRoot(@NotNull String path, @NotNull Predicate<VirtualFile> predicate) {
        VirtualFile importableRoot = LocalFileFinder.findFile(path);

        if (importableRoot == null) {
            return new HashSet<>();
        }

        return Arrays.stream(importableRoot.getChildren())
                .filter(predicate)
                .collect(Collectors.toSet());
    }

    public GlobalSearchScope sketchesInModuleScope(Module[] modules) {
        if (modules == null || modules.length == 0) return null;

        GlobalSearchScope scope = GlobalSearchScope.moduleScope(modules[0]);

        for (int i = 1; i < modules.length; i++) {
            scope.uniteWith(GlobalSearchScope.moduleScope(modules[i]));
        }

        return scope;
    }
}
