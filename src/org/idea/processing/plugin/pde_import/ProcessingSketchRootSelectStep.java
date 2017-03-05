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

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.projectImport.ProjectImportWizardStep;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.util.PathUtil;
import org.idea.processing.plugin.ProcessingPluginUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ItemEvent;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class ProcessingSketchRootSelectStep extends ProjectImportWizardStep {

    private TextFieldWithBrowseButton projectRootDirectoryBrowser;
    private JPanel importPanel;
    private JLabel projectRootBrowserLabel;
    private JLabel filesDetectedLabel;
    private JTextArea importableClassesListTextArea;
    private JRadioButton importIntoDefaultProjectOption;
    private JRadioButton createProjectInSelectedRootOption;
    private JRadioButton importProjectIntoCustomRootOption;
    private TextFieldWithBrowseButton customImportRootDirectoryBrowser;
    private JTextField projectCreationPathPreviewTextField;

    private ProcessingImportBuilder.Parameters importParameters;

    private final List<JRadioButton> projectCreationRootOptionButtonGroup = new LinkedList<>();

    private final FileChooserDescriptor DIRECTORY_CHOOSER_DESCRIPTOR = new FileChooserDescriptor(false, true, false, false, false, false)
            .withHideIgnored(true)
            .withShowHiddenFiles(false);

    public ProcessingSketchRootSelectStep(WizardContext context) {
        super(context);
    }

    @Override
    public JComponent getComponent() {
        setupImportRootButtons();

        projectRootDirectoryBrowser.setText(getWizardContext().getProjectFileDirectory());

        projectRootDirectoryBrowser.addBrowseFolderListener("Select Sketch Directory",
                "Select a Processing sketch directory containing PDE files to import as a project.",
                getWizardContext().getProject(), DIRECTORY_CHOOSER_DESCRIPTOR);
        customImportRootDirectoryBrowser.addBrowseFolderListener("Select Project Directory",
                "Select a directory in which the project will be created, and the sketch imported.",
                getWizardContext().getProject(), DIRECTORY_CHOOSER_DESCRIPTOR);

        projectRootDirectoryBrowser.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            public void textChanged(DocumentEvent event) {
                updateDataModel();
                refreshPdeFileImportPreview();
                refreshProjectCreationPreview();
            }
        });

        customImportRootDirectoryBrowser.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent) {
                refreshProjectCreationPreview();
            }
        });

        return importPanel;
    }

    private void setupImportRootButtons() {
        if (projectCreationRootOptionButtonGroup.isEmpty()) {
            projectCreationRootOptionButtonGroup.add(importIntoDefaultProjectOption);
            projectCreationRootOptionButtonGroup.add(createProjectInSelectedRootOption);
            projectCreationRootOptionButtonGroup.add(importProjectIntoCustomRootOption);

            for (JRadioButton radioButton : projectCreationRootOptionButtonGroup) {
                if (radioButton == importProjectIntoCustomRootOption) {
                    importProjectIntoCustomRootOption.addItemListener(e -> {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            customImportRootDirectoryBrowser.setVisible(true);
                            customImportRootDirectoryBrowser.setEnabled(true);
                        } else {
                            customImportRootDirectoryBrowser.setVisible(false);
                            customImportRootDirectoryBrowser.setEnabled(false);
                        }

                        refreshProjectCreationPreview();
                    });
                } else {
                    radioButton.addItemListener(e -> refreshProjectCreationPreview());
                }
            }
        }
    }

    @Override
    public void updateDataModel() {
        getWizardContext().setProjectName(PathUtil.getFileName(projectRootDirectoryBrowser.getText()));
        getWizardContext().setProjectFileDirectory(projectRootDirectoryBrowser.getText());

        if (importIntoDefaultProjectOption.isSelected()) {
            getParameters().projectCreationRoot = ProjectUtil.getBaseDir();
        } else if (createProjectInSelectedRootOption.isSelected()) {
            getParameters().projectCreationRoot = projectRootDirectoryBrowser.getText();
        } else if (importProjectIntoCustomRootOption.isSelected()) {
            getParameters().projectCreationRoot = customImportRootDirectoryBrowser.getText();
        }

        getParameters().root = projectRootDirectoryBrowser.getText();
        getParameters().importablePdeFiles = ProcessingPluginUtil.INSTANCE.filterFilesAtRoot(projectRootDirectoryBrowser.getText(), isPdeFile());

        Collection<VirtualFile> dataDirectoryResults = ProcessingPluginUtil.INSTANCE.filterFilesAtRoot(projectRootDirectoryBrowser.getText(), vfsPath -> vfsPath.isDirectory() && vfsPath.getName().equals("data"));

        if (! dataDirectoryResults.isEmpty()) {
            VirtualFile dataDirectory = dataDirectoryResults.iterator().next();

            final AtomicBoolean dataDirectoryHasFiles = new AtomicBoolean(false);
            VfsUtil.visitChildrenRecursively(dataDirectory, new VirtualFileVisitor() {
                @Override
                public boolean visitFile(@NotNull VirtualFile file) {
                    if (dataDirectoryHasFiles.get()) {
                        return false;
                    }

                    if (!file.isDirectory()) {
                        dataDirectoryHasFiles.set(true);
                        return false;
                    }

                    return true;
                }
            });

            if (dataDirectoryHasFiles.get()) {
                getParameters().resourceDirectoryPath = dataDirectory;
            }
        } else {
            getParameters().resourceDirectoryPath = null;
        }
    }

    @NotNull
    public ProcessingImportBuilder.Parameters getParameters() {
        if (importParameters == null) {
            importParameters = ((ProcessingImportBuilder) getBuilder()).getParameters();
        }

        return importParameters;
    }

    private void refreshPdeFileImportPreview() {
        if (! getParameters().importablePdeFiles.isEmpty()) {
            filesDetectedLabel.setForeground(JBColor.BLACK);
            filesDetectedLabel.setText(getParameters().importablePdeFiles.size() + " PDE files");

            StringJoiner importablePdeFilePaths = new StringJoiner("\n");

            for (VirtualFile importablePdeFile : getParameters().importablePdeFiles) {
                importablePdeFilePaths.add(importablePdeFile.getPath());
            }

            importableClassesListTextArea.setText(importablePdeFilePaths.toString());
        } else {
            importableClassesListTextArea.setText("No sketch classes were found at this root directory.");
            filesDetectedLabel.setText("0 PDE files");
            filesDetectedLabel.setForeground(JBColor.RED);
        }
    }

    private void refreshProjectCreationPreview() {
        projectCreationPathPreviewTextField.setText(getProjectCreationRootPreviewPath());
    }

    private String getProjectCreationRootPreviewPath() {
        String sketchDirectoryName = PathUtil.getFileName(projectRootDirectoryBrowser.getText());

        if (importIntoDefaultProjectOption.isSelected()) {

            return Paths.get(ProjectUtil.getBaseDir(), sketchDirectoryName).toString();
        } else if (createProjectInSelectedRootOption.isSelected()) {
            return projectRootDirectoryBrowser.getText();
        } else if (importProjectIntoCustomRootOption.isSelected()) {
            return Paths.get(customImportRootDirectoryBrowser.getText(), sketchDirectoryName).toString();
        }

        return "";
    }

    private Predicate<VirtualFile> isPdeFile() {
        return vfsPath -> FileUtil.getExtension(vfsPath.getPath()).equals("pde");
    }
}
