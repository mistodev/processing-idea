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

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;

import java.util.Map;

class MainSketchSelectListItem {
    private final Map.Entry<Pair<PsiFile, PsiClass>, PsiImportList> entry;

    public MainSketchSelectListItem(Map.Entry<Pair<PsiFile, PsiClass>, PsiImportList> entry) {
        this.entry = entry;
    }

    public Map.Entry<Pair<PsiFile, PsiClass>, PsiImportList> getEntry() {
        return entry;
    }

    @Override
    public String toString() {
        return entry.getKey().first.getName();
    }
}