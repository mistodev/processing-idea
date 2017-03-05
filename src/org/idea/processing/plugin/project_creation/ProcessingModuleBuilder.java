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

package org.idea.processing.plugin.project_creation;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.idea.processing.plugin.ProcessingPluginUtil;
import org.idea.processing.plugin.project_creation.dependency.AddDependenciesToProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenExternalExecutor;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;
import org.jetbrains.idea.maven.project.MavenConsole;
import org.jetbrains.idea.maven.project.MavenConsoleImpl;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.idea.processing.plugin.project_creation.dependency.DependencyResolutionBundle;
import org.idea.processing.plugin.project_creation.dependency.Version;
import org.idea.processing.plugin.project_creation.wizard.ConfigureProcessingProject;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import static com.intellij.openapi.vfs.LocalFileSystem.getInstance;

public class ProcessingModuleBuilder extends JavaModuleBuilder {

    private final Logger logger = com.intellij.openapi.diagnostic.Logger.getInstance(ProcessingModuleBuilder.class);

    private final String builderId;
    private final String presentableName;
    private final String description;
    private final Icon bigIcon;

    private boolean generateTemplateSketchClass = true;

    @SuppressWarnings("UnusedDeclaration")
    public ProcessingModuleBuilder() {
        this("processing", "Processing", "Processing sketch.", AllIcons.General.Information);
    }

    protected ProcessingModuleBuilder(String builderId, String presentableName, String description, Icon bigIcon) {
        this.builderId = builderId;
        this.presentableName = presentableName;
        this.description = description;
        this.bigIcon = bigIcon;
    }

    @Override
    public ModuleType getModuleType() {
        return ProcessingModuleType.getInstance();
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext,
                                                @NotNull ModulesProvider modulesProvider) {
        ConfigureProcessingProject projectConfig =  new ConfigureProcessingProject(wizardContext);

        ModuleWizardStep[] wizardSteps = new ModuleWizardStep[1];
        wizardSteps[0] = projectConfig;

        return wizardSteps;
    }

    @Nullable
    public List<Module> commit(@NotNull Project project, ModifiableModuleModel model, ModulesProvider modulesProvider) {
        List<Module> modules = super.commit(project, model, modulesProvider);

        Pair<Version, String> selectedProcessingVersion = ConfigureProcessingProject.Parameters.processingVersionDescriptor;

        File versionSpecificDependencyDirectory = new File(selectedProcessingVersion.getSecond());

        Application app = ApplicationManager.getApplication();

        Predicate<VirtualFile> isJar = file -> FileUtil.getExtension(file.getPath()).equals("jar");

        List<VirtualFile> jarFilesInRoot = new LinkedList<>(ProcessingPluginUtil.INSTANCE.filterFilesAtRoot(
                selectedProcessingVersion.getSecond(), isJar));

        final String versionedDependencyDirUrl = VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL,
                versionSpecificDependencyDirectory.getPath());

        if (jarFilesInRoot.isEmpty()) {
            logger.info("Dependencies do not already exist for version " + selectedProcessingVersion.first +
                    " on disk at path '" + selectedProcessingVersion.getSecond() + "'. Initiating download.");

            // Download dependencies from Maven repository. User selected a version that isn't available locally.
            CreateVersionedProcessingPom createPomTask =
                    new CreateVersionedProcessingPom(DependencyResolutionBundle.getCentralRepos(),
                    selectedProcessingVersion.getFirst(),
                    versionSpecificDependencyDirectory);

            logger.info("Creating a Maven POM file to describe the necessary dependencies.");

            try {
                createPomTask.call();
            } catch (IOException io) {
                logger.error("POM file creation failed at root: '" + versionedDependencyDirUrl + "'. Dependency resolution terminated due to: ", io);
                // @TODO Add a UI notification to indicate that dependencies for project could not be resolved.
                return modules;
            }

            logger.info("POM file creation succeeded. Proceeding to run Maven executor with the generated POM.");

            MavenRunnerSettings settings = new MavenRunnerSettings();
            settings.getMavenProperties().put("interactiveMode", "false");
            settings.getMavenProperties().put("outputDirectory", "./");

            MavenRunnerParameters params = new MavenRunnerParameters();
            params.setWorkingDirPath(versionSpecificDependencyDirectory.getPath());

            String mavenDependencyPluginVersion = DependencyResolutionBundle.key("maven_dependency_plugin_version");

            logger.debug("maven-dependency-plugin version to use is '" + mavenDependencyPluginVersion + "'.");

            params.setGoals(Collections.singletonList("org.apache.maven.plugins:maven-dependency-plugin:"
                                                        + mavenDependencyPluginVersion + ":copy-dependencies"));

            MavenConsole console = new MavenConsoleImpl("Resolve Processing Dependencies", project);

            MavenExternalExecutor runner = new MavenExternalExecutor(project, params, null, settings, console);

            boolean resolutionSuccess = runner.execute(null);

            if (resolutionSuccess) {
                logger.info("Dependency resolution to the root '" + versionedDependencyDirUrl + "' succeeded.");
            } else {
                logger.warn("copy-dependency goal completed unsuccessfully.");
                return modules;
            }
        }

        VirtualFile dependencyDirectory = VirtualFileManager.getInstance().refreshAndFindFileByUrl(versionedDependencyDirUrl);

        logger.info("Adding Processing dependencies at root '" + dependencyDirectory.getPath() + "' as a dependency of the project.");

        Module createdModule = modules.get(0);

        AddDependenciesToProject depsToProject = new AddDependenciesToProject(createdModule, dependencyDirectory);

        app.invokeLater(() -> app.runWriteAction(depsToProject));

        logger.info("Processing library version " + selectedProcessingVersion.first + " dependency resolution completed.");

        return modules;
    }

    @Override
    public void setupRootModel(final ModifiableRootModel modifiableRootModel) throws ConfigurationException {
        super.setupRootModel(modifiableRootModel);

        String contentEntryPath = getContentEntryPath();
        if (StringUtil.isEmpty(contentEntryPath)) {
            throw new ConfigurationException("There is no valid content entry path associated with the module. Unable to generate template directory structure.");
        }

        LocalFileSystem fileSystem = getInstance();
        VirtualFile modelContentRootDir = fileSystem.refreshAndFindFileByIoFile(new File(contentEntryPath));

        if (modelContentRootDir == null) {
            throw new ConfigurationException("Model content root directory '" + contentEntryPath + "' could not be found. Unable to generate template directory structure.");
        }

        ContentEntry content = modifiableRootModel.addContentEntry(modelContentRootDir);

        try {
            VirtualFile sourceCodeDir = VfsUtil.createDirectories(modelContentRootDir.getPath() + "/src/main/java");
            VfsUtil.createDirectories(modelContentRootDir.getPath() + "/src/main/java/com/processing/sketch");

            VirtualFile resources = VfsUtil.createDirectories(modelContentRootDir.getPath() + "/src/main/resources");

            content.addSourceFolder(sourceCodeDir, false);
            content.addSourceFolder(resources, JavaResourceRootType.RESOURCE, JavaResourceRootType.RESOURCE.createDefaultProperties());
        } catch (IOException io) {
            logger.error("Unable to generate template directory structure:", io);
            throw new ConfigurationException("Unable to generate template directory structure.");
        }

        VirtualFile sketchPackagePointer = getInstance().refreshAndFindFileByPath(getContentEntryPath() + "/src/main/java/com/processing/sketch");

        if (generateTemplateSketchClass) {
            ApplicationManager.getApplication().runWriteAction(new CreateSketchTemplateFile(sketchPackagePointer));
        }
    }

    public void setGenerateTemplateSketchClass(boolean generateTemplateSketchClass) {
        this.generateTemplateSketchClass = generateTemplateSketchClass;
    }

    @Override
    public String getBuilderId() {
        return builderId;
    }

    @Override
    public Icon getBigIcon() {
        return bigIcon;
    }

    @Override
    public Icon getNodeIcon() {
        return AllIcons.General.BalloonInformation;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getPresentableName() {
        return presentableName;
    }

    @Override
    public String getGroupName() {
        return "Processing";
    }

    @Override
    public String getParentGroup() {
        return "Processing";
    }

    @Override
    public int getWeight() {
        return 60;
    }
}
