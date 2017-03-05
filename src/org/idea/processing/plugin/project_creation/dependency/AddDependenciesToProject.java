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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class AddDependenciesToProject implements Runnable {

    private final Logger logger = com.intellij.openapi.diagnostic.Logger.getInstance(AddDependenciesToProject.class);

    private final Module module;
    private final VirtualFile jarDirectory;

    public AddDependenciesToProject(@NotNull Module module, @NotNull VirtualFile jarDirectory) {
        this.module = module;
        this.jarDirectory = jarDirectory;
    }

    /*
        Adds the designated directory as a dependency of the specified module and project associated with the module.
     */
    @Override
    public void run() {
        if (module.isDisposed() || module.getProject().isDisposed()) {
            logger.warn("Either the module '" + module.getName() + "' or project '" + module.getProject().getName() + " has been disposed." +
                    " Cannot add root '" + jarDirectory.getPath() + " as a project and module dependency.");
            return;
        }

        LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(module.getProject());
        LibraryTable.ModifiableModel projectLibraryModel = projectLibraryTable.getModifiableModel();

        logger.debug("Adding '" + jarDirectory + "' as a dependency of the project '" + module.getProject().getName() + "'.");

        // Create library and add it as a project level dependency.
        Library processingLibrary = createProcessingCoreDependencyLibrary(projectLibraryModel);
        projectLibraryModel.commit();

        logger.debug("Change committed to the project library table.");

        logger.debug("Adding '" + jarDirectory + "' as a dependency of the module '" + module.getProject().getName() + "'.");

        // Add library as the module level dependency.
        ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
        rootModel.addLibraryEntry(processingLibrary);
        rootModel.commit();

        logger.debug("Change committed to the module library table.");
    }

    private Library createProcessingCoreDependencyLibrary(@NotNull LibraryTable.ModifiableModel libraryModel) {
         Library processingCoreLibrary = libraryModel.createLibrary("processing-core");
         Library.ModifiableModel modifiableModel = processingCoreLibrary.getModifiableModel();

         modifiableModel.addJarDirectory(jarDirectory, true, OrderRootType.CLASSES);
         modifiableModel.commit();

        return processingCoreLibrary;
    }
}
