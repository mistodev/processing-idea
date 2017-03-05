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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.projectImport.ProjectImportBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.idea.processing.plugin.project_creation.ProcessingModuleBuilder;

import javax.swing.*;
import java.util.*;

public class ProcessingImportBuilder extends ProjectImportBuilder<String> {

    public static class Parameters {
        public String root;
        public String projectCreationRoot;
        public Collection<VirtualFile> importablePdeFiles;
        public VirtualFile resourceDirectoryPath;
        public List<String> workspace;
        public List<String> projectsToConvert = new ArrayList<>();
        public boolean openModuleSettings;
        public Set<String> existingModuleNames;
    }

    private final Logger logger = com.intellij.openapi.diagnostic.Logger.getInstance(ProcessingImportBuilder.class);

    private Parameters parameters;

    @NotNull
    @Override
    public String getName() {
        return "Processing Sketch";
    }

    // @TODO Update icon to a proper one.
    @Override
    public Icon getIcon() {
        return AllIcons.Icons.Ide.SpeedSearchPrompt;
    }

    @Override
    public List<String> getList() {
        return getParameters().workspace;
    }

    @Override
    public boolean isMarked(String element) {
        if (getParameters().projectsToConvert != null) {
            return getParameters().projectsToConvert.contains(element);
        }

        // @TODO Implement isMarked method properly.
        throw new UnsupportedOperationException("isMarked method not fully implemented.");
    }

    @Override
    public void setList(List<String> list) throws ConfigurationException {
        getParameters().projectsToConvert = list;
    }

    @Override
    public boolean isOpenProjectSettingsAfter() {
        return getParameters().openModuleSettings;
    }

    @Override
    public void setOpenProjectSettingsAfter(boolean enabled) {
        getParameters().openModuleSettings = enabled;
    }

    @Nullable
    @Override
    public List<Module> commit(Project project,
                               ModifiableModuleModel modifiableModuleModel,
                               ModulesProvider modulesProvider,
                               ModifiableArtifactModel modifiableArtifactModel) {

        logger.debug("Initializing module builder instance.");
        ProcessingModuleBuilder processingModuleBuilder = new ProcessingModuleBuilder();
        processingModuleBuilder.setGenerateTemplateSketchClass(false);

        logger.debug("Creating modules for project '" + project + "' at path '" + getParameters().projectCreationRoot + "'.");

        List<Module> modules = processingModuleBuilder.commit(project, modifiableModuleModel, modulesProvider);

        Collection<VirtualFile> importablePdeFiles = new LinkedList<>(getParameters().importablePdeFiles);

        logger.info("Identified a total of " + importablePdeFiles.size() + " PDE files for import from '" + getParameters().root + "'.");

        ImportSketchClasses importSketchClasses = new ImportSketchClasses(this, project, modules, importablePdeFiles);
        DumbService.getInstance(project).smartInvokeLater(importSketchClasses);

        return modules;
    }

    @Nullable
    public String getRootDirectory() {
        return getParameters().root;
    }

    public void setRootDirectory(String sketchRoot) {
        this.getParameters().root = sketchRoot;
    }

    @NotNull
    public Parameters getParameters() {
        if (parameters == null) {
            parameters = new Parameters();
            parameters.existingModuleNames = new HashSet<>();
            if (isUpdate()) {
                Project project = getCurrentProject();
                if (project != null) {
                    for (Module module : ModuleManager.getInstance(project).getModules()) {
                        parameters.existingModuleNames.add(module.getName());
                    }
                }
            }
        }

        return parameters;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        parameters = null;
    }
}
