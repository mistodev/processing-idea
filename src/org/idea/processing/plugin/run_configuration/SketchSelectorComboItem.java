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

import com.intellij.psi.PsiClass;

class SketchSelectorComboItem {
    private PsiClass sketchClass;

    public SketchSelectorComboItem(PsiClass sketchClass) {
        this.sketchClass = sketchClass;
    }

    public PsiClass getSketchClass() {
        return sketchClass;
    }

    public void setSketchClass(PsiClass sketchClass) {
        this.sketchClass = sketchClass;
    }

    @Override
    public String toString() {
        String sketchFqn = sketchClass.getQualifiedName();

        int packageEnd = sketchFqn.lastIndexOf(".");
        String className = sketchFqn.substring(packageEnd + 1);

        return className + " (" + sketchFqn.substring(0, packageEnd) + ")";
    }

}
