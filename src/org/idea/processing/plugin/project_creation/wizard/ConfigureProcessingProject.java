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

package org.idea.processing.plugin.project_creation.wizard;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import org.idea.processing.plugin.project_creation.dependency.AddDependenciesToProject;
import org.idea.processing.plugin.project_creation.dependency.Version;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.font.TextAttribute;
import java.util.Collections;

public class ConfigureProcessingProject extends ModuleWizardStep {

    private final Logger logger = com.intellij.openapi.diagnostic.Logger.getInstance(ConfigureProcessingProject.class);

    private JComboBox versionSelectComboBox;
    private JRadioButton otherVersionOptionRadioButton;
    private JRadioButton fromPDEInstallRadioButton;
    private TextFieldWithBrowseButton localPdeLibraryBrowser;
    private JPanel wizardContainerPanel;
    private JLabel versionAnalysisProgressIcon;
    private JLabel pdeCoreLibraryDirSelectErrorLabel;
    private JLabel versionResolutionErrorLabel;

    private ConfigureProcessingProject.Parameters parameters;

    private final WizardContext context;

    private static final FileChooserDescriptor PDE_CORE_LIBRARY_SELECT =
            new FileChooserDescriptor(false, true,
                    false, false, false, false)
            .withHideIgnored(true)
            .withShowHiddenFiles(false);

    public static class Parameters {
        public static Pair<Version, String> processingVersionDescriptor;

        public static void clear() {
            Parameters.processingVersionDescriptor = null;
        }
    }

    public ConfigureProcessingProject(WizardContext context) {
        this.context = context;
    }


    @NotNull
    private Parameters getParameters() {
        if (parameters == null) {
            parameters = new Parameters();
        }

        return parameters;
    }

    @Override
    public JComponent getComponent() {
        CustomVersionSelectorListener versionSelectorListener
                = new CustomVersionSelectorListener(versionSelectComboBox,
                localPdeLibraryBrowser,
                pdeCoreLibraryDirSelectErrorLabel,
                versionResolutionErrorLabel,
                versionAnalysisProgressIcon);

        otherVersionOptionRadioButton.addItemListener(versionSelectorListener);
        versionSelectorListener.populateComboBox();

        localPdeLibraryBrowser.addBrowseFolderListener("Select PDE Core Library Directory",
                "The core library directory can be found in the PDE installation directory, under 'Java/core/library'.",
                context.getProject(), PDE_CORE_LIBRARY_SELECT);

        localPdeLibraryBrowser.getTextField().getDocument().addDocumentListener(new LocalPdeDependencyBrowserAdapter());

        return wizardContainerPanel;
    }

    @Override
    public void updateDataModel() {
        if (otherVersionOptionRadioButton.isSelected()) {
            VersionSelectorComboItem versionSelectorComboItem = (VersionSelectorComboItem) versionSelectComboBox.getSelectedItem();

            getParameters().processingVersionDescriptor = Pair.create(versionSelectorComboItem.getVersion(), versionSelectorComboItem.getLocation());
        } else if (fromPDEInstallRadioButton.isSelected()) {
            getParameters().processingVersionDescriptor = Pair.create(Version.INVALID, localPdeLibraryBrowser.getText());
        }
    }

    private void createUIComponents() {
        versionSelectComboBox = new ComboBox(new VersionComboBoxModel());
        versionSelectComboBox.setRenderer(new VersionComboBoxRenderer());

        otherVersionOptionRadioButton = new JRadioButton();

        fromPDEInstallRadioButton = new JRadioButton();
        fromPDEInstallRadioButton.addItemListener(new LocalPdeDependenciesItemListener());
    }

    private class VersionComboBoxRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            if (value instanceof JComponent) {
                return (JComponent) value;
            }

            boolean isSelectableVersion = (value != null) && (Version.parseString(value.toString()) != Version.INVALID);

            super.getListCellRendererComponent(list, value, index,
                    isSelected && isSelectableVersion, cellHasFocus);

            Font defaultListFont = list.getFont();

            Font titleBoldFont = defaultListFont.deriveFont(
                    Collections.singletonMap(
                            TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD));

            // Non-versions are expected to be titles. Render them in bold.
            setEnabled(isSelectableVersion);

            if (isSelectableVersion) {
                setFont(defaultListFont);
            } else if (!isSelectableVersion && (value instanceof String) && value.equals(VersionComboHeaders.NONE.toString())) {
                setFont(defaultListFont);
            } else {
                setFont(titleBoldFont);
            }

            return this;
        }
    }

    private class VersionComboBoxModel extends DefaultComboBoxModel<String> {
        @Override
        public void setSelectedItem(Object item) {
            if (item instanceof JSeparator) {
                return;
            }

            if (item instanceof VersionSelectorComboItem) {
                VersionSelectorComboItem comboItem = (VersionSelectorComboItem) item;

                // Not a valid version, so don't allow user to select it.
                if (comboItem.getVersion() == Version.INVALID) {
                    return;
                }
            }

            super.setSelectedItem(item);
        }
    }

    private class LocalPdeDependenciesItemListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                localPdeLibraryBrowser.setEnabled(false);
            }

            if (e.getStateChange() == ItemEvent.SELECTED) {
                localPdeLibraryBrowser.setEnabled(true);
            }
        }
    }

    private class LocalPdeDependencyBrowserAdapter extends DocumentAdapter {
            @Override
            public void textChanged(DocumentEvent event) {
                logger.debug("PDE core library directory browser text-field value changed to: " + localPdeLibraryBrowser.getText());

                String pathToPdeLibraryDir = localPdeLibraryBrowser.getText();

                if (! pathToPdeLibraryDir.isEmpty()) {
                    VirtualFile pdeLibraryDirVirtualFile = LocalFileSystem.getInstance().findFileByPath(pathToPdeLibraryDir);

                    if (pdeLibraryDirVirtualFile == null) {
                        pdeCoreLibraryDirSelectErrorLabel.setText("Not a valid path.");
                        pdeCoreLibraryDirSelectErrorLabel.setVisible(true);
                        return;
                    }

                    if (! pdeLibraryDirVirtualFile.isDirectory()) {
                        pdeCoreLibraryDirSelectErrorLabel.setText("Not a directory.");
                        pdeCoreLibraryDirSelectErrorLabel.setVisible(true);
                        return;
                    }

                    VirtualFile[] entries = pdeLibraryDirVirtualFile.getChildren();

                    if (entries.length == 0) {
                        pdeCoreLibraryDirSelectErrorLabel.setText("Directory empty.");
                        pdeCoreLibraryDirSelectErrorLabel.setVisible(true);
                        return;
                    }

                    boolean coreJarFound = false;
                    for (VirtualFile virtualFile : entries) {
                        if (virtualFile.getName().equals("core.jar")) {
                            coreJarFound = true;
                        }
                    }

                    if (coreJarFound) {
                        pdeCoreLibraryDirSelectErrorLabel.setVisible(false);
                    } else {
                        pdeCoreLibraryDirSelectErrorLabel.setText("No 'core.jar' found.");
                        pdeCoreLibraryDirSelectErrorLabel.setVisible(true);
                    }
                } else {
                    pdeCoreLibraryDirSelectErrorLabel.setText("No directory selected.");
                }
        }
    }
}
