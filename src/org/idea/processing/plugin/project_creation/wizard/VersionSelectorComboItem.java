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

import org.idea.processing.plugin.project_creation.dependency.Version;

class VersionSelectorComboItem {
    private Version version;
    private String location;

    VersionSelectorComboItem(Version version, String location) {
        this.version = version;
        this.location = location;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return version.getMajor() + "." + version.getMinor() + "." + version.getBuild();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VersionSelectorComboItem comboItem = (VersionSelectorComboItem) o;

        return version != null ? version.equals(comboItem.version) : comboItem.version == null;
    }

    @Override
    public int hashCode() {
        return version != null ? version.hashCode() : 0;
    }
}
