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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.JavaCommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

class ProcessingCommandLineState extends JavaCommandLineState {

    protected final ProcessingRunConfiguration configuration;

    private final Logger logger = com.intellij.openapi.diagnostic.Logger.getInstance(ProcessingCommandLineState.class);

    private final String ARG_VAL_SEP = "=";
    private final Module module;

    public ProcessingCommandLineState(Module module,
                                      ExecutionEnvironment environment,
                                      @NotNull final ProcessingRunConfiguration configuration) {
        super(environment);
        this.configuration = configuration;
        this.module = module;
    }

    @Override
    protected final JavaParameters createJavaParameters() throws ExecutionException {
        JavaParameters javaParameters = new JavaParameters();

        for(RunConfigurationExtension ext: Extensions.getExtensions(RunConfigurationExtension.EP_NAME)) {
            ext.updateJavaParameters(configuration, javaParameters, getRunnerSettings());
        }

        // @TODO What is the classpath entry constant class?
        javaParameters.configureByModule(module, 3);

        javaParameters.setMainClass(configuration.getRunSettings().getSketchClass());

        ParametersList programParameters = javaParameters.getProgramParametersList();

        int display = configuration.getRunSettings().getDisplay();
        programParameters.addParametersString(CliArg.DISPLAY.text() + ARG_VAL_SEP + display);

        String windowColorHex = configuration.getRunSettings().getWindowBackgroundColor();

        if (windowColorHex != null && ! windowColorHex.isEmpty()) {
            programParameters.addParametersString(CliArg.WINDOW_COLOR.text() + ARG_VAL_SEP + windowColorHex);
        }

        String stopButtonColorHex = configuration.getRunSettings().getStopButtonColor();

        if (stopButtonColorHex != null && ! stopButtonColorHex.isEmpty()) {
            programParameters.addParametersString(CliArg.STOP_BUTTON_COLOR.text() + ARG_VAL_SEP + stopButtonColorHex);
        }

        DisplayLocation sketchLocation = DisplayLocation.toEnum(configuration.getRunSettings().getLocation());

        // Processing Core automatically aligns sketch at the center of the screen, incorporating sketch size.
        if (sketchLocation != DisplayLocation.CENTER) {
            GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

            GraphicsDevice screen;
            if (display >= screens.length) {
                logger.warn("The display with index " + display + " is no longer connected.");
                screen = screens[0];
            } else {
                screen = screens[display];
            }

            Point sketchLocationPoint = sketchLocation.getLocation(screen);

            programParameters.addParametersString(CliArg.SKETCH_SCREEN_LOCATION.text() + ARG_VAL_SEP +
                    ((int) sketchLocationPoint.getX()) + "," + ((int) sketchLocationPoint.getY()));
        }

        String sketchOutputPath = configuration.getRunSettings().getSketchOutputPath();
        if (sketchOutputPath != null && !sketchOutputPath.isEmpty()) {
            programParameters.addParametersString(CliArg.SKETCH_OUTPUT_PATH.text() + ARG_VAL_SEP + sketchOutputPath);
        }

        if (configuration.getRunSettings().isHideStopButton()) {
            programParameters.add(CliArg.HIDE_STOP_BUTTON.text());
        }

        if (configuration.getRunSettings().isFullscreen()) {
            programParameters.addParametersString(CliArg.PRESENTATION_MODE.text());
        }

        programParameters.add(configuration.getRunSettings().getSketchClass());
        programParameters.addParametersString(configuration.getRunSettings().getSketchArguments());

        javaParameters.getVMParametersList().addParametersString(configuration.getRunSettings().getJvmArguments());

        return javaParameters;
    }
}
