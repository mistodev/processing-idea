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

package org.idea.processing.plugin.run_configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedList;

public class ProcessingRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule> {
    private ProcessingRunSettings runSettings;
    private Module module;

    public ProcessingRunConfiguration(String name, @NotNull RunConfigurationModule configurationModule, @NotNull ConfigurationFactory factory)
    {
        super(name, configurationModule, factory);
        this.runSettings = new ProcessingRunSettings();
    }

    public ProcessingRunSettings getRunSettings() {
        return runSettings;
    }

    private boolean isModuleSupported(Module module) {
        return ModuleRootManager.getInstance(module).getSdk() != null;

    }

    @Override
    public void setModule(Module module) {
        if (module != null) {
            this.module = module;
            runSettings.setModule(module.getName());
        }
    }

    @Override
    public Collection<Module> getValidModules() {
        Module[] modules = ModuleManager.getInstance(getProject()).getModules();
        java.util.List<Module> res = new LinkedList<>();

        for (Module module : modules) {
            if (isModuleSupported(module)) {
                res.add(module);
            }
        }

        return res;
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new ProcessingRunConfigurationEditor<>(getProject());
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) throws ExecutionException {
        Module firstValidModule = getValidModules().iterator().next();
        return new ProcessingCommandLineState(firstValidModule, executionEnvironment, this);
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);

        String name = ProcessingRunSettings.class.getSimpleName();
        Element settingsElement = element.getChild(name);

        if (settingsElement == null) return;

        ProcessingRunSettings deserializedConfig = XmlSerializer.deserialize(settingsElement, ProcessingRunSettings.class);

        runSettings.setModule(deserializedConfig.getModule());
        runSettings.setSketchClass(deserializedConfig.getSketchClass());
        runSettings.setFullscreen(deserializedConfig.isFullscreen());
        runSettings.setHideStopButton(deserializedConfig.isHideStopButton());
        runSettings.setJvmArguments(deserializedConfig.getJvmArguments());
        runSettings.setSketchArguments(deserializedConfig.getSketchArguments());
        runSettings.setWindowBackgroundColor(deserializedConfig.getWindowBackgroundColor());
        runSettings.setStopButtonColor(deserializedConfig.getStopButtonColor());
        runSettings.setSketchOutputPath(deserializedConfig.getSketchOutputPath());
        runSettings.setLocation(deserializedConfig.getLocation());

        readModule(element);
    }

    @Override
    public void writeExternal(Element parentNode) throws WriteExternalException {
        super.writeExternal(parentNode);

        parentNode.addContent(XmlSerializer.serialize(runSettings));

        writeModule(parentNode);
    }

    @Override
    public ProcessingRunConfiguration clone() {
        ProcessingRunConfiguration clonedConfiguration = (ProcessingRunConfiguration) super.clone();
        clonedConfiguration.runSettings = runSettings.clone();

        return clonedConfiguration;
    }
}