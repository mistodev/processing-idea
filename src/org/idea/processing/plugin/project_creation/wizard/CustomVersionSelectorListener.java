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

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.idea.processing.plugin.project_creation.dependency.DependencyUtils;
import org.idea.processing.plugin.project_creation.dependency.Version;
import org.jetbrains.annotations.Nullable;
import org.idea.processing.plugin.project_creation.dependency.DependencyResolutionBundle;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class CustomVersionSelectorListener implements ItemListener {
    private final Logger logger = com.intellij.openapi.diagnostic.Logger.getInstance(CustomVersionSelectorListener.class);

    private TextFieldWithBrowseButton localPdeLibraryBrowser;
    private JComboBox versionSelectComboBox;
    private JLabel pdeDependencySelectErrorLabel;
    private JLabel versionResolutionErrorLabel;
    private JLabel versionAnalysisProgressIcon;

    CustomVersionSelectorListener(JComboBox versionSelectComboBox,
                                         TextFieldWithBrowseButton localPdeLibraryBrowser,
                                         JLabel pdeDependencySelectErrorLabel,
                                         JLabel versionResolutionErrorLabel,
                                         JLabel versionAnalysisProgressIcon) {
        this.versionSelectComboBox = versionSelectComboBox;
        this.localPdeLibraryBrowser = localPdeLibraryBrowser;
        this.pdeDependencySelectErrorLabel = pdeDependencySelectErrorLabel;
        this.versionResolutionErrorLabel = versionResolutionErrorLabel;
        this.versionAnalysisProgressIcon = versionAnalysisProgressIcon;
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.DESELECTED) {
            versionSelectComboBox.setEnabled(false);
            localPdeLibraryBrowser.setEnabled(true);
            return;
        }

        if (e.getStateChange() == ItemEvent.SELECTED) {
            populateComboBox();
        }
    }

    public void populateComboBox() {
        modifyFormComponentVisibility();

        // If the selector has already been populated, do not populate it again.
        if (versionComboBoxContains(VersionComboHeaders.INSTALLED.toString()) ||
                versionComboBoxContains(VersionComboHeaders.AVAILABLE.toString())) {
            return;
        }

        String relativeDependencyRootPath = DependencyResolutionBundle.key("dependency_root");
        File dependencyRoot = new File(PluginManager
                .getPlugin(PluginId.getId("org.idea.processing.plugin")).getPath(),
                relativeDependencyRootPath);

        // Populate the versions already installed.
        populateVersionSelectorWithInstalledVersions(dependencyRoot);

        logger.info("Querying Processing versions available for download.");

        // INVOKE
        populateVersionSelectorWithAvailableVersions(dependencyRoot);
    }

    private void modifyFormComponentVisibility() {
        if (localPdeLibraryBrowser != null) {
            localPdeLibraryBrowser.setEnabled(false);
            localPdeLibraryBrowser.setText("");

            pdeDependencySelectErrorLabel.setVisible(false);
            pdeDependencySelectErrorLabel.setText("");

            versionSelectComboBox.setEnabled(true);

            if (!versionComboBoxContains(VersionComboHeaders.AVAILABLE.toString())) {
                versionAnalysisProgressIcon.setVisible(true);
            }
        }
    }

    private void populateVersionSelectorWithInstalledVersions(final File processingDependencyDirectory) {
        if (!processingDependencyDirectory.exists()) {
            processingDependencyDirectory.mkdir();
        }

        List<Version> installedVersions = new LinkedList<>();

        try {
            installedVersions = Files.list(processingDependencyDirectory.toPath())
                    .filter(entry -> Files.isDirectory(entry))
                    .map(entry -> Version.parseString(entry.getFileName().toString()))
                    .filter(version -> version != Version.INVALID)
                    .collect(Collectors.toList());

            installedVersions.sort(Version.descendingOrderComparator());
        } catch (IOException io) {
            versionResolutionErrorLabel.setText("Querying of installed Processing library versions failed");
            logger.error("Querying of installed Processing library versions failed", io);
        }

        logger.info("Identified " + installedVersions.size() + " installed Processing library versions.");

        versionSelectComboBox.addItem(VersionComboHeaders.INSTALLED.toString());

        boolean firstVersion = true;
        for (Version installed : installedVersions) {
            VersionSelectorComboItem installedItem = new VersionSelectorComboItem(installed,
                    new File(processingDependencyDirectory, installed.toString()).getPath());
            versionSelectComboBox.addItem(installedItem);

            if (firstVersion) {
                versionSelectComboBox.setSelectedItem(installedItem);
                firstVersion = false;
            }
        }

        if (installedVersions.isEmpty()) {
            versionSelectComboBox.addItem(VersionComboHeaders.NONE.toString());
        }
    }

    private void populateVersionSelectorWithAvailableVersions(final File processingDependencyDirectory) {
        logger.debug("Initializing available version population thread.");

        new Thread(() -> {
            try {
                Map<Version, String> availableProcessingVersions = DependencyUtils.getAvailableArtifactVersions(
                        "org.processing",
                        "core",
                        DependencyResolutionBundle.getCentralRepos());

                versionSelectComboBox.addItem(new JSeparator(JSeparator.HORIZONTAL));
                versionSelectComboBox.addItem(VersionComboHeaders.AVAILABLE.toString());

                if (availableProcessingVersions.isEmpty()) {
                    versionSelectComboBox.addItem(VersionComboHeaders.NONE.toString());
                    return;
                }

                logger.info("Identified " + availableProcessingVersions.size() + " downloadable Processing library versions.");

                for (Map.Entry<Version, String> availableVersion : availableProcessingVersions.entrySet()) {
                    Version version = availableVersion.getKey();

                    String earliestProcessingSupported = DependencyResolutionBundle.key("earliest_processing_version_supported");

                    if (version.compareTo(Version.parseString(earliestProcessingSupported)) >= 0) {
                        VersionSelectorComboItem comboItem = new VersionSelectorComboItem(version,
                                new File(processingDependencyDirectory, version.toString()).getPath());

                        if (versionComboBoxContains(comboItem)) {
                            continue;
                        }

                        versionSelectComboBox.addItem(comboItem);
                    }
                }

                versionAnalysisProgressIcon.setVisible(false);
            } catch (IOException io) {
                versionResolutionErrorLabel.setText("Querying downloadable Processing library versions failed");
                logger.error("Querying Processing versions from remote repository failed.", io);
            }
        }).start();
    }

    private boolean versionComboBoxContains(@Nullable Object findItem) {
        if (findItem == null) {
            return false;
        }

        for (int i = 0; i < versionSelectComboBox.getItemCount(); i++) {
            Object item = versionSelectComboBox.getItemAt(i);

            if (item.equals(findItem)) {
                return true;
            }
        }

        return false;
    }
}