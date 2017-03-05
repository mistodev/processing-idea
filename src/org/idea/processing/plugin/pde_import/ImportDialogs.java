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

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogBuilder;

import javax.swing.*;
import java.awt.*;

public enum ImportDialogs {

    NO_MAIN_SKETCH_CLASS {
        @Override
        public DialogBuilder getDialog(Object... args) {
            JPanel dialogContent = new JPanel();
            dialogContent.setLayout(new GridBagLayout());

            dialogContent.add(new JLabel("No main sketch class code be identified in the selected project. A template class will be generated on your behalf."));

            DialogBuilder builder = new DialogBuilder();
            builder.setTitle("No Main Sketch Class");
            builder.setCenterPanel(dialogContent);
            builder.removeAllActions();
            builder.addOkAction();

            return builder;
        }
    },

    SELECT_MAIN_SKETCH_CLASS {

        @Override
        public DialogBuilder getDialog(Object... args) {
            JPanel dialogContent = new JPanel();
            dialogContent.setLayout(new GridBagLayout());

            dialogContent.add(new JLabel("More than one class can be the main sketch class. Select one to proceed with the import:"));

            if (args.length < 1) {
                throw new IllegalStateException("An instance of " + ComboBox.class + " must be passed in as an argument to " + this.getClass());
            }

            com.intellij.openapi.ui.ComboBox mainSketchSelector = (com.intellij.openapi.ui.ComboBox) args[0];

            dialogContent.add(mainSketchSelector);

            DialogBuilder builder = new DialogBuilder();
            builder.setTitle("Select Main Sketch Class");
            builder.setCenterPanel(dialogContent);
            builder.removeAllActions();
            builder.addOkAction();

            return builder;
        }
    };

    public abstract DialogBuilder getDialog(Object... args);
}
