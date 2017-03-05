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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
    Converts classes of the Processing 'pde' type into valid Java classes.
 */
class PdeConverter {

    private final Logger logger = com.intellij.openapi.diagnostic.Logger.getInstance(PdeConverter.class);

    /*
        Parse all of the given PDE files into a Psi tree, and return the resulting collection of PsiClasses.
     */
    public Collection<PsiFile> parseAll(Project project, @NotNull Collection<VirtualFile> pdeFiles) throws InterruptedException {
        Collection<PsiFile> parsed = new ArrayList<>(pdeFiles.size());

        Collection<Callable<PsiFile>> parsingTasks = new ArrayList<>(pdeFiles.size());

        for (VirtualFile pdeFile : pdeFiles) {
            parsingTasks.add(() -> parse(project, pdeFile));
        }

        ExecutorService parsingExecutor = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());

        List<Future<PsiFile>> parsingResults = parsingExecutor.invokeAll(parsingTasks);

        for (Future<PsiFile> futureParsedClass : parsingResults) {
            try {
                parsed.add(futureParsedClass.get());
            } catch (ExecutionException ee) {
                logger.error(ee);
            }
        }

        parsingExecutor.shutdown();

        return parsed;
    }

    private PsiFile parse(Project project, @NotNull VirtualFile pdeFile) throws IOException {
        return ApplicationManager.getApplication().runReadAction(new ParserComputable(project, pdeFile));
    }

    private class ParserComputable implements ThrowableComputable<PsiFile, IOException> {

        private final Project project;
        private final VirtualFile pdeFile;

        public ParserComputable(Project project, @NotNull VirtualFile pdeFile) {
            this.project = project;
            this.pdeFile = pdeFile;
        }

        @Override
        public PsiFile compute() throws IOException {
            String pdeContents = new String(FileUtil.loadBytes(pdeFile.getInputStream()), CharsetToolkit.UTF8);

            return PsiFileFactory.getInstance(project).createFileFromText(pdeFile.getNameWithoutExtension(), JavaFileType.INSTANCE, pdeContents);
        }
    }
}
