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

import com.google.common.base.CaseFormat;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

class ClassNameSuggester {

    private static final Pattern ILLEGAL_JAVA_CLASS_CHARACTER_PATTERN = Pattern.compile("[^a-zA-Z0-9_]");

    // @TODO Check whether the generated class name matches any of the existing class names in the imported sketch.
    public static String suggest(@NotNull String input) {
        String preprocessedInput = StringUtils.deleteWhitespace(input).toLowerCase()
                .replaceAll(ILLEGAL_JAVA_CLASS_CHARACTER_PATTERN.pattern(), "")
                .replaceAll("_+", "_");

        return CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, preprocessedInput);
    }
}
