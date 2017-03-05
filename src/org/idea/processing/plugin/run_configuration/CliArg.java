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

enum CliArg {

    DISPLAY("--display"),
    WINDOW_COLOR("--window-color"),
    SKETCH_SCREEN_LOCATION("--location"),
    SKETCH_OUTPUT_PATH("--sketch-path"),
    STOP_BUTTON_COLOR("--stop-color"),
    HIDE_STOP_BUTTON("--hide-stop"),
    PRESENTATION_MODE("--present");

    private final String text;

    CliArg(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
