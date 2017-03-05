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

import java.awt.*;

enum DisplayLocation {

    CENTER ("Center") {
        @Override
        public Point getLocation(final GraphicsDevice graphics) {
            return new Point(graphics.getDisplayMode().getWidth() / 2, graphics.getDisplayMode().getHeight() / 2);
        }
    },
    TOP_LEFT ("Top Left") {
        @Override
        public Point getLocation(final GraphicsDevice graphics) {
            return new Point(0, 0);
        }
    },
    TOP_RIGHT ("Top Right") {
        @Override
        public Point getLocation(final GraphicsDevice graphics) {
            return new Point(graphics.getDisplayMode().getWidth(), 0);
        }
    },
    BOTTOM_LEFT ("Bottom Left") {
        @Override
        public Point getLocation(final GraphicsDevice graphics) {
            return new Point(0, graphics.getDisplayMode().getHeight());
        }
    },
    BOTTOM_RIGHT ("Bottom Right") {
        @Override
        public Point getLocation(final GraphicsDevice graphics) {
            return new Point(graphics.getDisplayMode().getWidth(), graphics.getDisplayMode().getHeight());
        }
    };

    private String description;

    DisplayLocation(String description) {
        this.description = description;
    }

    public abstract Point getLocation(final GraphicsDevice graphics);

    @Override
    public String toString() {
        return description;
    }

    public static DisplayLocation toEnum(String value) {
        for (DisplayLocation location : DisplayLocation.values()) {
            if (location.toString().equals(value)) {
                return location;
            }
        }

        return null;
    }
}
