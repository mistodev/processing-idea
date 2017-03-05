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

package org.idea.processing.plugin.project_creation.dependency;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {
    public static final Version LATEST = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
    public static final Version INVALID = new Version(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, true);

    public static final Pattern COMPLETE_VERSION_PATTERN = Pattern.compile("([0-9]+).([0-9]+).([0-9]+)");
    public static final Pattern REDUCED_VERSION_PATTERN = Pattern.compile("([0-9]+).([0-9]+)");

    private int major;
    private int minor;
    private int build;

    private final boolean isCompleteVersion;

    public Version(int major, int minor, int build, boolean isCompleteVersion) {
        this.major = major;
        this.minor = minor;
        this.build = build;
        this.isCompleteVersion = isCompleteVersion;
    }

    public static Version parseString(String versionStr) {
        Matcher buildVerMatcher = COMPLETE_VERSION_PATTERN.matcher(versionStr);

        int majorNum;
        int minorNum;
        int buildNum;

        if (buildVerMatcher.find()) {
            String major = buildVerMatcher.group(1);
            String minor = buildVerMatcher.group(2);
            String build = buildVerMatcher.group(3);

            majorNum = Short.parseShort(major);
            minorNum = Short.parseShort(minor);
            buildNum = Short.parseShort(build);
        } else {
            Matcher reducedBuildVerMatcher = REDUCED_VERSION_PATTERN.matcher(versionStr);

            if (reducedBuildVerMatcher.find()) {
                String major = reducedBuildVerMatcher.group(1);
                String minor = reducedBuildVerMatcher.group(2);

                majorNum = Short.parseShort(major);
                minorNum = Short.parseShort(minor);
                buildNum = 0;

                return new Version(majorNum, minorNum, buildNum, false);
            } else {
                return Version.INVALID;
            }
        }

        return new Version(majorNum, minorNum, buildNum, true);
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public int getBuild() {
        return build;
    }

    public void setBuild(int build) {
        this.build = build;
    }

    @Override
    public String toString() {
        if (isCompleteVersion) {
            return major +
                    "." + minor +
                    "." + build;
        } else {
            return major +
                    "." + minor;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version version = (Version) o;

        if (major != version.major) return false;
        if (minor != version.minor) return false;
        return build == version.build && isCompleteVersion == version.isCompleteVersion;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + build;
        result = 31 * result + (isCompleteVersion ? 1 : 0);
        return result;
    }

    /*
        Sorts versions in ascending order.
     */
    @Override
    public int compareTo(@NotNull Version otherVersion) {
        if (this == otherVersion) {
            return 0;
        }

        if (this.major < otherVersion.major) {
            return -1;
        } else if (this.major > otherVersion.major) {
            return 1;
        }

        if (this.minor < otherVersion.minor) {
            return -1;
        } else if (this.minor > otherVersion.minor) {
            return 1;
        }

        if (this.build < otherVersion.build) {
            return -1;
        } else if (this.build > otherVersion.build) {
            return 1;
        }

        // Major, minor and build numbers are equal, so the versions are equal.
        return 0;
    }

    public static Comparator<Version> descendingOrderComparator() {
        return (o1, o2) -> {
            return (o1.compareTo(o2) * -1); // Descending version sort.
        };
    }
}
