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

package org.idea.processing.plugin.project_creation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class CreateSketchTemplateFile implements Runnable {
    private final Logger logger = com.intellij.openapi.diagnostic.Logger.getInstance(CreateSketchTemplateFile.class);

    private final VirtualFile sourceDirPointer;

    public CreateSketchTemplateFile(VirtualFile sourceDirPointer) {
        this.sourceDirPointer = sourceDirPointer;
    }

    @Override
    public void run() {
        try {
            logger.info("Attempting to read template main sketch file.");

            InputStream templateFileStream = this.getClass().getResourceAsStream("SketchTemplate.java.template");

            byte[] templateContent = StreamUtil.loadFromStream(templateFileStream);

            logger.info("Read a total of " + templateContent.length + " bytes from template sketch file stream.");

            VirtualFile sketchFile = sourceDirPointer.createChildData(this, "Sketch.java");

            logger.info("Attempting to create template sketch file '" + sketchFile + "' as a child of '" + sourceDirPointer.getPath() + "'.");

            try (OutputStream sketchFileStream = sketchFile.getOutputStream(ApplicationManager.getApplication())) {
                sketchFileStream.write(templateContent);
            }
        } catch (IOException ex) {
            logger.error(ex);
        }
    }
}
