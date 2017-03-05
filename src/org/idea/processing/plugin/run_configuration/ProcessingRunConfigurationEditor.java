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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.ui.ColorChooser;
import com.intellij.ui.ColorUtil;
import com.intellij.util.Query;
import org.idea.processing.plugin.ProcessingPluginUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

public class ProcessingRunConfigurationEditor<T extends ProcessingRunConfiguration> extends SettingsEditor<T> {

    private JPanel panel1;
    private com.intellij.application.options.ModulesComboBox moduleSelector;
    private JComboBox sketchSelector;
    private JTextField sketchArguments;
    private JCheckBox fullscreenCheckBox;
    private JCheckBox hideStopButtonCheckBox;
    private TextFieldWithBrowseButton windowBackgroundColorChooser;
    private TextFieldWithBrowseButton stopButtonColorChooser;
    private JComboBox sketchLocationSelector;
    private JComboBox displaySelector;
    private JTextField jvmArguments;
    private TextFieldWithBrowseButton sketchOutputPath;

    private final Project project;

    private UpdateSketchComboBox updateSketchComboBox = new UpdateSketchComboBox();

    private final FileChooserDescriptor DIRECTORY_CHOOSER_DESCRIPTOR =
            new FileChooserDescriptor(false, true,
                    false, false, false, false)
            .withHideIgnored(true)
            .withShowHiddenFiles(false);

    public ProcessingRunConfigurationEditor(Project project) {
        this.project = project;
    }

    @Override
    protected void resetEditorFrom(@NotNull T t) {
        moduleSelector.fillModules(project);

        Module[] allModules = ModuleManager.getInstance(project).getModules();

        if (allModules.length > 0) {
            moduleSelector.setSelectedModule(allModules[0]);
        }

        Module sketchModule = null;

        for (Module projectModule : allModules) {
            if (projectModule.getName().equals(t.getRunSettings().getModule())) {
                sketchModule = projectModule;
                moduleSelector.setSelectedModule(projectModule);
                break;
            }
        }

        refreshSketchSelectorFromSelectedModule();

        if (sketchModule != null) {
            String savedSketchClassFqn = t.getRunSettings().getSketchClass();

            Module[] moduleSearchScope = {sketchModule};

            PsiClass sketchClass = JavaPsiFacade.getInstance(project)
                    .findClass(savedSketchClassFqn,
                            ProcessingPluginUtil.INSTANCE.sketchesInModuleScope(moduleSearchScope)
                    );

            if (sketchClass != null) {
                for (int i = 0; i < sketchSelector.getItemCount(); i++) {
                    SketchSelectorComboItem comboItem = (SketchSelectorComboItem) sketchSelector.getItemAt(i);

                    if (comboItem.getSketchClass().getQualifiedName().equals(savedSketchClassFqn)) {
                        sketchSelector.setSelectedItem(comboItem);
                        break;
                    }
                }
            }
        }

        fullscreenCheckBox.setSelected(t.getRunSettings().isFullscreen());
        hideStopButtonCheckBox.setSelected(t.getRunSettings().isHideStopButton());
        sketchOutputPath.setText(t.getRunSettings().getSketchOutputPath());
        sketchArguments.setText(t.getRunSettings().getSketchArguments());
        jvmArguments.setText(t.getRunSettings().getJvmArguments());
        windowBackgroundColorChooser.setText(t.getRunSettings().getWindowBackgroundColor());
        stopButtonColorChooser.setText(t.getRunSettings().getStopButtonColor());

        DisplayLocation sketchLocation = DisplayLocation.toEnum(t.getRunSettings().getLocation());

        sketchLocationSelector.setSelectedItem(sketchLocation);

        moduleSelector.addActionListener(updateSketchComboBox);
    }

    @Override
    protected void applyEditorTo(@NotNull T t) throws ConfigurationException {
        t.setModule(moduleSelector.getSelectedModule());

        SketchSelectorComboItem comboSketchItem = (SketchSelectorComboItem) sketchSelector.getSelectedItem();

        if (comboSketchItem != null) {
            t.getRunSettings().setSketchClass(comboSketchItem.getSketchClass().getQualifiedName());
        }

         // Window properties
         String stopButtonColorHex = stopButtonColorChooser.getText();

         if (stopButtonColorHex != null && ! stopButtonColorHex.isEmpty()) {
             t.getRunSettings().setStopButtonColor(stopButtonColorHex);
         }

         String windowBackgroundColorHex = windowBackgroundColorChooser.getText();

         if (windowBackgroundColorHex != null && ! windowBackgroundColorHex.isEmpty()) {
             t.getRunSettings().setWindowBackgroundColor(windowBackgroundColorHex);
         }

         // Presentation properties
         t.getRunSettings().setFullscreen(fullscreenCheckBox.isSelected());
         t.getRunSettings().setHideStopButton(hideStopButtonCheckBox.isSelected());

         // Display properties
         t.getRunSettings().setDisplay(displaySelector.getSelectedIndex() + 1);

         DisplayLocation sketchLocation = (DisplayLocation) sketchLocationSelector.getSelectedItem();

         if (sketchLocation != null) {
             t.getRunSettings().setLocation(sketchLocation.toString());
         } else {
             t.getRunSettings().setLocation(DisplayLocation.CENTER.toString());
         }

         t.getRunSettings().setSketchOutputPath(sketchOutputPath.getText());
         t.getRunSettings().setSketchArguments(sketchArguments.getText());
         t.getRunSettings().setJvmArguments(jvmArguments.getText());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        windowBackgroundColorChooser.addActionListener(e -> {
            Color color = ColorChooser.chooseColor(WindowManager.getInstance().suggestParentWindow(project),
                    "Window Background Color", null);
            if (color != null) {
                windowBackgroundColorChooser.setText('#' + ColorUtil.toHex(color).toUpperCase());
                windowBackgroundColorChooser.setBackground(color);
            }
        });

        stopButtonColorChooser.addActionListener(e -> {
            Color color = ColorChooser.chooseColor(WindowManager.getInstance().suggestParentWindow(project),
                    "Stop Button Color", null);
            if (color != null) {
                stopButtonColorChooser.setText('#' + ColorUtil.toHex(color).toUpperCase());
                stopButtonColorChooser.setBackground(color);
            }
        });

        sketchOutputPath.addBrowseFolderListener("Sketch Output Path",
                "Select a directory to which the output of sketches will be saved to. By default, it is the project root directory.",
                project, DIRECTORY_CHOOSER_DESCRIPTOR);

        // Derive the display on which the project window is rendered. This is assumed to be the active and default window.
        JFrame projectFrame = WindowManager.getInstance().getFrame(project);
        GraphicsConfiguration projectFrameDisplayConfig = projectFrame.getGraphicsConfiguration();
        GraphicsDevice activeDevice = projectFrameDisplayConfig.getDevice();

        GraphicsEnvironment displayEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] availableDisplays = displayEnvironment.getScreenDevices();

        int id = 1;
        for (GraphicsDevice device : availableDisplays) {
            DisplayComboItem displayItem = new DisplayComboItem(id + "", device);

            displaySelector.addItem(displayItem);

            if (activeDevice.equals(device)) {
                displaySelector.setSelectedItem(displayItem);
            }

            id++;
        }

        for (DisplayLocation location : DisplayLocation.values()) {
            sketchLocationSelector.addItem(location);
        }

        return panel1;
    }

    private class UpdateSketchComboBox implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (moduleSelector.getSelectedModule() == null) {
                return;
            }

            sketchSelector.removeAllItems();

            Module[] searchScopeModules = new Module[1];
            searchScopeModules[0] = moduleSelector.getSelectedModule();

            refreshSketchSelector(searchScopeModules);
        }
    }

    private void refreshSketchSelectorFromSelectedModule() {
        if (moduleSelector.getSelectedModule() == null) {
            return;
        }

        sketchSelector.removeAllItems();

        Module[] searchScopeModules = new Module[1];
        searchScopeModules[0] = moduleSelector.getSelectedModule();

        refreshSketchSelector(searchScopeModules);
    }

    private void refreshSketchSelector(Module [] moduleSearchScope) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            Query<PsiClass> classQuery =
                    AllClassesSearch.search(ProcessingPluginUtil.INSTANCE.sketchesInModuleScope(moduleSearchScope), project);
            Collection<PsiClass> classesInModule = classQuery.findAll();

            for (PsiClass classInModule : classesInModule) {
                if (SketchClassFilter.isSketchClass(classInModule)) {
                    sketchSelector.addItem(new SketchSelectorComboItem(classInModule));
                }
            }
        });
    }

    private class DisplayComboItem {
        private String id;
        private GraphicsDevice device;

        public DisplayComboItem(String id, GraphicsDevice device) {
            this.id = id;
            this.device = device;
        }

        private String formattedResolution() {
            return "(" + device.getDisplayMode().getWidth() + "x" + device.getDisplayMode().getHeight() + ")";
        }

        @Override
        public String toString() {
            return id + " " + formattedResolution();
        }
    }
}
