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

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.execution.configurations.ConfigurationUtil;
import com.intellij.ide.util.ClassFilter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

class SketchClassFilter implements ClassFilter.ClassFilterWithScope {

    private final Project project;
    private final GlobalSearchScope searchScope;

    SketchClassFilter(final GlobalSearchScope scope) {
        project = scope.getProject();
        searchScope = scope;
    }

    @Override
    public GlobalSearchScope getScope() {
        return searchScope;
    }

    @Override
    public boolean isAccepted(PsiClass klass) {
        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
            if (isSketchClass(klass)) {
                final CompilerConfiguration compilerConfiguration = CompilerConfiguration.getInstance(project);
                final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(klass);

                if (virtualFile == null) {
                    return false;
                }

                return ! compilerConfiguration.isExcludedFromCompilation(virtualFile) &&
                        ! ProjectRootManager.getInstance(project)
                                .getFileIndex()
                                .isUnderSourceRootOfType(virtualFile, JavaModuleSourceRootTypes.RESOURCES);
            }

            return false;
        });
    }

    public static boolean isSketchClass(PsiClass klass) {
        if (klass.getQualifiedName() == null) { return false; }

        // klass.isInheritor(myBase, true) && ConfigurationUtil.PUBLIC_INSTANTIATABLE_CLASS.value(klass)

        // @TODO This would only find Processing 3 PApplet classes. Investigate handling Processing 2.0.
        return InheritanceUtil.isInheritor(klass, "processing.core.PApplet")
                && ConfigurationUtil.PUBLIC_INSTANTIATABLE_CLASS.value(klass);
    }
}
