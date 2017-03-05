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

package org.idea.processing.plugin.pde_import;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

class ImportSketchResources implements Runnable {

    private final Logger logger = com.intellij.openapi.diagnostic.Logger.getInstance(ImportSketchResources.class);

    private final Project project;
    private final VirtualFile sketchDataDirectory;

    public ImportSketchResources(@NotNull Project project, @Nullable VirtualFile sketchDataDirectory) {
        this.project = project;
        this.sketchDataDirectory = sketchDataDirectory;
    }

    @Override
    public void run() {
        if (sketchDataDirectory == null) {
            return;
        }

        VirtualFile projectResourceDirectory = VfsUtil.findRelativeFile(project.getBaseDir(), "src", "main", "resources");

        if (projectResourceDirectory == null) {
            throw new IllegalStateException("Cannot find directory 'src/main/resources' into which sketch resources are to be copied. Sketch resources cannot be imported.");
        }

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {

                for (VirtualFile resourceEntry : sketchDataDirectory.getChildren()) {
                    try {
                        logger.debug("Copying '" + resourceEntry.getPath() + "' into sketch project.");
                        resourceEntry.copy(this, projectResourceDirectory, resourceEntry.getName());
                        logger.debug("Copy of '" + resourceEntry.getPath() + "' succeeded.");
                    } catch (IOException io) {
                        // @TODO This failure is silent. Spawn a notification to inform the user.
                        logger.error("Whilst importing sketch resources into project, encountered an exception.", io);
                    }
                }
            }
        });
    }
}
