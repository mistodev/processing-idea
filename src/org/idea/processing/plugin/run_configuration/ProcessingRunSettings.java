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

public class ProcessingRunSettings {

    private String module;
    private String sketchClass;
    private String sketchOutputPath;
    private String sketchArguments;
    private String jvmArguments;

    private boolean fullscreen;
    private boolean hideStopButton;
    private int display;
    private String location;
    private String stopButtonColor;
    private String windowBackgroundColor;

    public String getSketchClass() {
        return sketchClass;
    }

    public void setSketchClass(String sketchClass) {
        this.sketchClass = sketchClass;
    }

    public String getJvmArguments() {
        return jvmArguments;
    }

    public void setJvmArguments(String jvmArguments) {
        this.jvmArguments = jvmArguments;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String moduleName) {
        this.module = moduleName;
    }

    public String getSketchOutputPath() {
        return sketchOutputPath;
    }

    public void setSketchOutputPath(String sketchOutputPath) {
        this.sketchOutputPath = sketchOutputPath;
    }

    public String getSketchArguments() {
        return sketchArguments;
    }

    public void setSketchArguments(String sketchArguments) {
        this.sketchArguments = sketchArguments;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public boolean isHideStopButton() {
        return hideStopButton;
    }

    public void setHideStopButton(boolean hideStopButton) {
        this.hideStopButton = hideStopButton;
    }

    public int getDisplay() {
        return display;
    }

    public void setDisplay(int display) {
        this.display = display;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStopButtonColor() {
        return stopButtonColor;
    }

    public void setStopButtonColor(String stopButtonColor) {
        this.stopButtonColor = stopButtonColor;
    }

    public String getWindowBackgroundColor() {
        return windowBackgroundColor;
    }

    public void setWindowBackgroundColor(String windowBackgroundColor) {
        this.windowBackgroundColor = windowBackgroundColor;
    }

    @Override
    public ProcessingRunSettings clone() {
        ProcessingRunSettings runSettings = new ProcessingRunSettings();
        runSettings.setSketchClass(getSketchClass());
        runSettings.setModule(getModule());
        runSettings.setSketchOutputPath(sketchOutputPath);
        runSettings.setSketchArguments(sketchArguments);
        runSettings.setFullscreen(fullscreen);
        runSettings.setHideStopButton(hideStopButton);
        runSettings.setDisplay(display);
        runSettings.setLocation(location);
        runSettings.setStopButtonColor(stopButtonColor);
        runSettings.setWindowBackgroundColor(windowBackgroundColor);

        return runSettings;
    }

}
