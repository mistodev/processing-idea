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

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ProcessingConfigurationType implements ConfigurationType {

    private final ProcessingConfigurationFactory configFactory = new ProcessingConfigurationFactory(this);

    // Use bundle strings.
    @Override
    public String getDisplayName() {
        return "Processing";
    }

    @Override
    public String getConfigurationTypeDescription() {
        return "Processing project run configuration.";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @NotNull
    @Override
    public String getId() { return "org.idea.processing.configuration.type"; }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{configFactory};
    }

    private static class ProcessingConfigurationFactory extends ConfigurationFactory {

        public ProcessingConfigurationFactory(ConfigurationType processingConfigurationType) {
            super(processingConfigurationType);
        }

        @NotNull
        @Override
        public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            ProcessingRunConfigurationModule rcm = new ProcessingRunConfigurationModule(project, true);
            return new ProcessingRunConfiguration("Processing", rcm, this);
        }
    }


    public static ProcessingConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(ProcessingConfigurationType.class);
    }
}
