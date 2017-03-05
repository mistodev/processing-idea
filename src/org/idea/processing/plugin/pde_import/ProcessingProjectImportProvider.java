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

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectWizardStepFactory;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectImportProvider;
import org.jetbrains.annotations.Nullable;
import org.idea.processing.plugin.project_creation.wizard.ConfigureProcessingProject;

public class ProcessingProjectImportProvider extends ProjectImportProvider {

    private final ProcessingProjectOpenProcessor myProcessor;
    
    protected ProcessingProjectImportProvider(ProcessingImportBuilder builder) {
        super(builder);
        myProcessor = new ProcessingProjectOpenProcessor(builder);
    }

    public ModuleWizardStep[] createSteps(WizardContext context) {
        final ProjectWizardStepFactory stepFactory = ProjectWizardStepFactory.getInstance();
        return new ModuleWizardStep[]{
                stepFactory.createProjectJdkStep(context),
                new ProcessingSketchRootSelectStep(context),
                new ConfigureProcessingProject(context)
        };
    }

    @Override
    protected boolean canImportFromFile(VirtualFile file) {
        return myProcessor.canOpenProject(file);
    }

    @Nullable
    @Override
    public String getFileSample() {
        return "<b>Processing</b> sketch directory containing (.pde) files";
    }
}
