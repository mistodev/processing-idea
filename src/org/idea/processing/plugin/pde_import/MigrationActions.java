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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class MigrationActions {

    /*
     * For the interface check determine first whether in the tree the PsiErrorElement has a parent of type PsiClass.
     * If it does not, then the class is a main sketch file.
     */
    public static Collection<PsiFile> firstStageMainSketchIdentification(@NotNull Collection<PsiFile> parsedPdeFiles) {
        Set<PsiFile> shortlist = new HashSet<>();

        for (PsiFile parsedPdeFile : parsedPdeFiles) {
            PsiErrorElement firstErrorInFile = PsiTreeUtil.findChildOfType(parsedPdeFile, PsiErrorElement.class);

            if (firstErrorInFile == null) {
                continue;
            }

            PsiClass errorContainingClass = PsiTreeUtil.getParentOfType(firstErrorInFile, PsiClass.class);

            if (errorContainingClass == null) {
                shortlist.add(parsedPdeFile);
            }
        }

        return shortlist;
    }

    public static Map<Pair<PsiFile, PsiClass>, PsiImportList> secondStageMainSketchIdentification(@NotNull Collection<PsiFile> mainSketchClassShortlist,
                                                                                                  @NotNull PsiElementFactory elementFactory) {
        Map<Pair<PsiFile, PsiClass>, PsiImportList> mainSketchClasses = new HashMap<>(3);

        /* Introduce a topmost, parent class into each of the candidates, and re-evaluate to select the main sketch class. */
        for (PsiFile mainSketchCandidate : mainSketchClassShortlist) {
            PsiImportList sketchImportList = PsiTreeUtil.findChildOfType(mainSketchCandidate, PsiImportList.class);

            Set<String> allImportStatements = Arrays.stream(sketchImportList.getAllImportStatements())
                    .map(PsiImportStatementBase::getText)
                    .collect(Collectors.toSet());

            String mainSketchCandidateStr = mainSketchCandidate.getViewProvider().getContents().toString();

            if (! allImportStatements.isEmpty()) {
                StringJoiner sketchClassWithoutImportsJoiner = new StringJoiner("\n");

                Arrays.stream(mainSketchCandidateStr.split("\n"))
                        .filter(line -> ! allImportStatements.contains(line))
                        .forEachOrdered(sketchClassWithoutImportsJoiner::add);

                mainSketchCandidateStr = sketchClassWithoutImportsJoiner.toString();
            }

            PsiClass unanonymisedSketchClass = elementFactory.createClassFromText(mainSketchCandidateStr, null);
            unanonymisedSketchClass.setName(ClassNameSuggester.suggest(mainSketchCandidate.getName()));

            Collection<PsiMethod> methodsInClass = PsiTreeUtil.findChildrenOfType(unanonymisedSketchClass, PsiMethod.class);

            if (!methodsInClass.isEmpty()) {
                methodsInClass.stream()
                        .filter(method -> method.getParameterList() != null &&
                                (method.getParameterList().getParameters().length == 0) &&
                                (method.getName().equals("draw") || method.getName().equals("setup")))
                        .forEach(processingMethod -> {
                            if (!processingMethod.getModifierList().hasModifierProperty(PsiModifier.PUBLIC)) {
                                processingMethod.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
                            }

                            /*
                                If the class has a draw method, with no parameters, this is probably the PApplet draw method,
                                Save the class reference to the map of potential main sketch classes.
                            */
                            if (processingMethod.getName().equals("draw")) {
                                PsiImportList sketchImportListCopy = (PsiImportList) sketchImportList.copy();

                                // This is a side effect and it would be best to get rid of it.
                                mainSketchClasses.put(Pair.create(mainSketchCandidate, unanonymisedSketchClass), sketchImportListCopy);
                            }
                        });
            }
        }

        return mainSketchClasses;
    }


    /*
        Once the main sketch class has been selected, transform it into a valid Java class.
     */
    public static PsiFile postProcessSelectedMainSketchClass(@NotNull Project project,
                                                             @NotNull PsiClass selectedMainClass,
                                                             @NotNull PsiImportList correspondingImportList) {
        // Construct a valid Java class from the selected main sketch class.
        selectedMainClass.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);

        PsiJavaCodeReferenceElement pAppletReference = JavaPsiFacade.getElementFactory(project)
                .createFQClassNameReferenceElement("processing.core.PApplet", GlobalSearchScope.projectScope(project));
        selectedMainClass.getExtendsList().add(pAppletReference);

        PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(project);

        StringJoiner mainFileJoiner = new StringJoiner("\n\n");
        StringJoiner importStatementJoiner = new StringJoiner("\n");

        for (PsiImportStatementBase importStatement : correspondingImportList.getAllImportStatements()) {
            importStatementJoiner.add(importStatement.getText());
        }

        mainFileJoiner.add(importStatementJoiner.toString());
        mainFileJoiner.add(selectedMainClass.getText());

        return psiFileFactory.createFileFromText(selectedMainClass.getName() + ".java", JavaFileType.INSTANCE, mainFileJoiner.toString());
    }
}
