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

import com.intellij.ide.util.PackageUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.idea.processing.plugin.project_creation.RunnableActionUtils;

import java.util.*;

public class ImportSketchClasses implements Runnable {

    private final Logger logger = com.intellij.openapi.diagnostic.Logger.getInstance(ImportSketchClasses.class);

    private final String DEFAULT_SKETCH_PACKAGE_STATEMENT = "com.processing.sketch";

    private ProcessingImportBuilder importBuilder;
    private Collection<VirtualFile> importablePdeFiles;
    private List<Module> modules;
    private Project project;

    public ImportSketchClasses(ProcessingImportBuilder importBuilder,
                               Project project,
                               List<Module> modules,
                               Collection<VirtualFile> importablePdeFiles) {
        this.project = project;
        this.modules = modules;
        this.importBuilder = importBuilder;
        this.importablePdeFiles = importablePdeFiles;
    }

    @Override
    public void run() {
        PsiElementFactory javaElementFactory = JavaPsiFacade.getElementFactory(project);

        PdeConverter converter = new PdeConverter();
        Collection<PsiFile> parsedPdeFiles = new LinkedList<>();

        try {
            parsedPdeFiles = converter.parseAll(project, importablePdeFiles);
        } catch (InterruptedException ie) {
            logger.warn("Thread interrupted whilst parsing importable PDE files", ie);
        }

        Collection<PsiFile> mainSketchClassShortlist = MigrationActions.firstStageMainSketchIdentification(parsedPdeFiles);

        Map<Pair<PsiFile, PsiClass>, PsiImportList> mainSketchClasses = finalizeMainSketchClass(mainSketchClassShortlist, javaElementFactory);

        logger.info("Identified a total of " + mainSketchClasses.size() + " main sketch classes.");

        final Collection<PsiFile> importableSketchFiles = new HashSet<>(parsedPdeFiles);

        /*
        if (generateSketchClass) {
            ImportDialogs.NO_MAIN_SKETCH_CLASS.getDialog().show();
        }*/

        PsiFile selectedMainSketchFile;
        PsiClass selectedMainSketchClass;
        PsiImportList correspondingImportList;

        // At this point, a short-list of potential main classes has been established. Select one for the purposes of import.
        Iterator<Map.Entry<Pair<PsiFile, PsiClass>, PsiImportList>> classesIterator = mainSketchClasses.entrySet().iterator();

        Map.Entry<Pair<PsiFile, PsiClass>, PsiImportList> entry = classesIterator.next();

        selectedMainSketchFile = entry.getKey().getFirst();
        selectedMainSketchClass = entry.getKey().getSecond();
        correspondingImportList = entry.getValue();

        if (selectedMainSketchFile == null || selectedMainSketchClass == null || correspondingImportList == null) {
            throw new IllegalStateException("The selected main sketch class is null, or the corresponding import list to the class is null.");
        }

        PsiFile postProcessedMainSketchFile = MigrationActions.postProcessSelectedMainSketchClass(project, selectedMainSketchClass, correspondingImportList);

        importableSketchFiles.remove(selectedMainSketchFile);
        importableSketchFiles.add(postProcessedMainSketchFile);

        // Write all of the imported sketch files to disk.
        final VirtualFile sketchResourcesRoot = importBuilder.getParameters().resourceDirectoryPath;

        logger.info("Preparing to import sketch resources from '" + sketchResourcesRoot + "'.");

        RunnableActionUtils.runWhenInitialized(project, () -> {

            PsiDirectory defaultSketchPackage = findDefaultSketchPackage(modules, DEFAULT_SKETCH_PACKAGE_STATEMENT);

            if (defaultSketchPackage == null) {
                throw new IllegalStateException("Unable to find default sketch sources package for writing converted PDE files to.");
            }

            // @TODO Convert Processing cast methods to native casts and add an 'f' suffix at the end of float value declarations.
            for (PsiFile sketchFile : importableSketchFiles) {
                migrateColorToIntegerType(sketchFile, javaElementFactory);
            }

            ApplicationManager.getApplication().runWriteAction(new ImportedSketchClassWriter(project, defaultSketchPackage, importableSketchFiles));
            ApplicationManager.getApplication().runWriteAction(new ImportSketchResources(project, sketchResourcesRoot));
        });

        importBuilder.cleanup();
    }

    private void migrateColorToIntegerType(@NotNull PsiFile sketchFile, PsiElementFactory elementFactory) {
        Collection<PsiTypeElement> typeElementsInFile = PsiTreeUtil.findChildrenOfType(sketchFile, PsiTypeElement.class);

        for (PsiTypeElement typeElement : typeElementsInFile) {
            boolean isProcessingColorType = typeElement.getType().equalsToText("color");
            if (isProcessingColorType) {
                PsiTypeElement integerType = elementFactory.createTypeElementFromText("int", null);
                typeElement.replace(integerType);
            }
        }
    }

    private PsiDirectory findDefaultSketchPackage(Collection<Module> modules, String packageFqn) {
        PsiDirectory defaultSketchPackage = null;

        for (Module module : modules) {
            PsiDirectory defaultSketchPackageResult = PackageUtil.findPossiblePackageDirectoryInModule(module, packageFqn);

            if (defaultSketchPackageResult != null) {
                defaultSketchPackage = defaultSketchPackageResult;
                break;
            }
        }

        return defaultSketchPackage;
    }

    private Map<Pair<PsiFile, PsiClass>, PsiImportList> finalizeMainSketchClass(Collection<PsiFile> mainSketchClassShortlist,
                                                                                PsiElementFactory javaElementFactory) {
        Map<Pair<PsiFile, PsiClass>, PsiImportList> mainSketchClasses =
                MigrationActions.secondStageMainSketchIdentification(mainSketchClassShortlist, javaElementFactory);

        if (mainSketchClasses.size() == 1 || mainSketchClasses.isEmpty()) {
            return mainSketchClasses;
        }

        /*
            To cater for an unusual scenario in which there is more than one sketch class that has a draw method.

            This scenario should never arise, however, as Processing IDE doesn't allow multiple sketch classes to declare the draw() method.

            Prompt the user to select a class that will be the main sketch class?
            Perhaps a further heuristic can be applied prior to prompting the user to decide: how many of the other classes
            are referenced from each of the "main" classes.
         */
        MainSketchSelectListItem[] sketchSelectItems = new MainSketchSelectListItem[mainSketchClasses.entrySet().size()];

        int idx = 0;
        for (Map.Entry<Pair<PsiFile, PsiClass>, PsiImportList> pairPsiImportListEntry : mainSketchClasses.entrySet()) {
            sketchSelectItems[idx] = new MainSketchSelectListItem(pairPsiImportListEntry);
            idx++;
        }

        com.intellij.openapi.ui.ComboBox mainSketchSelector = new com.intellij.openapi.ui.ComboBox(sketchSelectItems);

        ImportDialogs.SELECT_MAIN_SKETCH_CLASS.getDialog(mainSketchSelector).show();

        MainSketchSelectListItem selectedClassForMain = (MainSketchSelectListItem) mainSketchSelector.getSelectedItem();
        mainSketchClasses = new HashMap<>();

        mainSketchClasses.put(selectedClassForMain.getEntry().getKey(), selectedClassForMain.getEntry().getValue());

        return mainSketchClasses;
    }

}
