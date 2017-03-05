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

package org.idea.processing.plugin.intention;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.CleanupLocalInspectionTool;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.idea.processing.plugin.project_creation.ProcessingModuleType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

// @TODO Refactor code to reduce duplication.
public class SettingsInvokedInSetupInspection extends LocalInspectionTool implements CleanupLocalInspectionTool {

    private final Collection<String> SETTINGS_METHODS =
            new HashSet<>(Arrays.asList("size", "fullScreen", "pixelDensity", "smooth", "noSmooth"));

    @NotNull
    public String getGroupDisplayName() {
        return "Processing";
    }

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Settings methods must be invoked only from 'settings' method";
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        Module psiFileModule = ModuleUtil.findModuleForPsiElement(holder.getFile());

        if (psiFileModule != null && ModuleUtil.getModuleType(psiFileModule) != ProcessingModuleType.getInstance()) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        PsiClass pAppletSubclass = getMainSketchClass(holder.getFile());

        if (pAppletSubclass == null) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof PsiMethodCallExpression) {
                    PsiMethodCallExpression methodCall = (PsiMethodCallExpression) element;

                    PsiMethod referencedMethod = methodCall.resolveMethod();

                    if (referencedMethod != null &&
                            referencedMethod.getContainingClass().getQualifiedName().equals("processing.core.PApplet")) {

                        if (SETTINGS_METHODS.contains(referencedMethod.getName())) {
                            PsiMethod enclosingMethod = PsiTreeUtil.getParentOfType(methodCall, PsiMethod.class);

                            if (enclosingMethod.getName().equals("settings")) {
                                PsiMethod[] superMethods = enclosingMethod.findSuperMethods();

                                if (superMethods.length > 0) {
                                    for (PsiMethod superMethod : superMethods) {
                                        if (superMethod.getContainingClass().getQualifiedName().equals("processing.core.PApplet")) {
                                            return;
                                        }
                                    }
                                }
                            }

                            holder.registerProblem(element, "PApplet#" + referencedMethod.getName() +
                                    " method should be invoked only from 'settings' method", new MoveSettingsFromSetupQuickFix(element));
                        }
                    }
                }
            }
        };
    }

    public static PsiClass getMainSketchClass(PsiFile sketchFile) {
        if (sketchFile instanceof PsiJavaFile) {
            PsiClass[] classes = ((PsiJavaFile) sketchFile).getClasses();

            for (PsiClass innerClass : classes) {
                PsiReferenceList superclasses = innerClass.getExtendsList();

                PsiJavaCodeReferenceElement [] referenceElements = superclasses.getReferenceElements();

                for (PsiJavaCodeReferenceElement reference : referenceElements) {
                    if (reference.getQualifiedName().equals("processing.core.PApplet")) {
                        return innerClass;
                    }
                }
            }
        }

        return null;
    }

    private static final class MoveSettingsFromSetupQuickFix extends LocalQuickFixOnPsiElement implements HighPriorityAction {

        public MoveSettingsFromSetupQuickFix(@NotNull PsiElement element) {
            super(element);
        }

        @Nls
        @NotNull
        @Override
        public String getText() {
            return "Sketch settings methods must be invoked only from 'settings' method";
        }

        @Override
        public void invoke(@NotNull Project project, @NotNull PsiFile file, @NotNull PsiElement element, @NotNull PsiElement endElement) {
            PsiMethodCallExpression methodCall = (PsiMethodCallExpression) element;
            PsiExpressionStatement methodCallStatementCopy = PsiTreeUtil.getParentOfType(methodCall, PsiExpressionStatement.class);

            PsiClass mainSketchClass = SettingsInvokedInSetupInspection.getMainSketchClass(file);

            if (mainSketchClass == null) {
                throw new IllegalStateException("Main sketch class couldn't be identified whilst attempting to determine whether settings() method exists.");
            }

            PsiMethod settingsMethod = null;

            for (PsiMethod method : mainSketchClass.getMethods()) {
                PsiMethod [] overridenMethods = method.findSuperMethods();

                for (PsiMethod overridenMethod : overridenMethods) {
                    if (overridenMethod.getName().equals("settings") &&
                            overridenMethod.getContainingClass().getQualifiedName().equals("processing.core.PApplet")) {
                        settingsMethod = method;
                    }
                }
            }

            if (settingsMethod == null) {
                // The sketch class doesn't have a settings method declared. Create one.
                PsiJavaFile methodCallFile = (PsiJavaFile) methodCall.getContainingFile();

                PsiClass [] innerClasses = methodCallFile.getClasses();

                PsiClass pAppletSubclass = null;
                for (PsiClass innerClass : innerClasses) {
                    PsiReferenceList superclasses = innerClass.getExtendsList();

                    for (PsiJavaCodeReferenceElement superclassRef : superclasses.getReferenceElements()) {
                        if (superclassRef.getQualifiedName().equals("processing.core.PApplet")) {
                            pAppletSubclass = innerClass;
                        }
                    }
                }

                if (pAppletSubclass == null) {
                    throw new IllegalStateException("Unable to find class in file '" + methodCallFile.getName() +
                            "' which extends 'processing.core.PApplet'.");
                }

                PsiMethod newSettingsMethod = JavaPsiFacade.getElementFactory(project).createMethod("settings", PsiType.VOID);

                PsiCodeBlock newSettingsMethodCodeBlock = PsiTreeUtil.findChildOfType(newSettingsMethod, PsiCodeBlock.class);
                newSettingsMethodCodeBlock.add(methodCallStatementCopy);

                pAppletSubclass.add(newSettingsMethod);

                methodCall.delete();
            } else {
                PsiCodeBlock settingsMethodCodeBlock = PsiTreeUtil.findChildOfType(settingsMethod, PsiCodeBlock.class);
                settingsMethodCodeBlock.add(methodCallStatementCopy);

                methodCall.delete();
            }
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return getText();
        }
    }
}
