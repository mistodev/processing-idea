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

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.RunResult;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

class ImportedSketchClassWriter implements Runnable {
    private final Logger logger = com.intellij.openapi.diagnostic.Logger.getInstance(ImportedSketchClassWriter.class);

    private final Project project;
    private final PsiDirectory packageFqn;
    private final Collection<PsiFile> sketchFiles;

    public ImportedSketchClassWriter(@NotNull Project project, @NotNull PsiDirectory packageFqn, @NotNull Collection<PsiFile> sketchFiles) {
        this.project = project;
        this.packageFqn = packageFqn;
        this.sketchFiles = sketchFiles;
    }

    @Override
    public void run() {
        logger.info("Preparing to write a total of " + sketchFiles.size() + " to the project package " + packageFqn + ".");

        for (PsiFile sketchFile : sketchFiles) {
            logger.info("Writing the sketch PSI file '" + sketchFile.getName() + "' to the project package '" + packageFqn + "'.");

            String sketchFileExtension = PathUtil.getFileExtension(sketchFile.getName());

            if (sketchFileExtension == null || ! sketchFileExtension.equals(JavaFileType.DEFAULT_EXTENSION)) {
                String generatedSketchFileName = PathUtil.makeFileName(PathUtil.getFileName(sketchFile.getName()), JavaFileType.DEFAULT_EXTENSION);

                sketchFile.setName(generatedSketchFileName);
            }

            WriteCommandAction.Simple<String> command = new WriteCommandAction.Simple<String>(project, sketchFile) {

                @Override
                protected void run() throws Throwable {
                    CodeStyleManager.getInstance(project).reformat(sketchFile, false);
                    packageFqn.add(sketchFile);
                }
            };

            RunResult<String> result = command.execute();
            logger.debug("Result of executing the file write action is: '" + result.getResultObject() + "'.");
        }
    }
}
