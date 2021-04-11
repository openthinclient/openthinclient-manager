/*******************************************************************************
 * openthinclient.org ThinClient suite
 * <p/>
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * <p/>
 * <p/>
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *******************************************************************************/
package org.openthinclient.pkgmgr.db;

import org.openthinclient.pkgmgr.I18N;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Embeddable
@Access(AccessType.FIELD)
public class Version implements Comparable<Version>, Serializable {

    private static final long serialVersionUID = 3258135760426317876L;
    private static final Pattern SPECIFIER_PATTERN = Pattern.compile(
              "^"
            + "(?:(\\d+)\\:)?"              // epoch
            + "(\\d[A-Za-z0-9\\.\\+-~]*?)"  // upstream version
            + "(?:-([A-Za-z0-9\\+\\.~]+))?" // debian revision
            + "$"
    );
    private static final Pattern TOKENS = Pattern.compile("(\\d+|\\D+)");

    @Column(name = "version_epoch")
    private int epoch = 0;
    @Column(name = "version_upstream")
    private String upstreamVersion;
    @Column(name = "version_revision")
    private String debianRevision;
    private transient int hashCode = -1;

    private static int compareNullableRevision(String version1, String version2) {
        // a non-specified version is always smaller than a specified one
        if (version1 != null && version2 != null) {
            return compareRevision(version1, version2);
        } else if (version1 != null) {
            return 1;
        } else if(version2 != null) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * Compare to revision specifiers according to the rules laid down in the debian specification:
     * <br> <a href="https://www.debian.org/doc/debian-policy/ch-controlfields.html#version">
     */
    private static int compareRevision(String version1, String version2) {
        boolean version1StartsWithDigit = (
            version1.length() > 0 && Character.isDigit(version1.charAt(0))
        );
        boolean version2StartsWithDigit = (
            version2.length() > 0 && Character.isDigit(version2.charAt(0))
        );

        // Parts currently compared
        // Note: Empty string equals zero in numerical comparision.
        String version1Part = "";
        String version2Part = "";

        // Whether to compare numerically or lexically
        boolean doCompareNumeric = false;

        // Initialize iterators
        Iterator<String> version1Parts = versionPartsIterator(version1);
        Iterator<String> version2Parts = versionPartsIterator(version2);

        // Determine whether to start numerical or lexical comparision
        // and initialize parts accordingly.
        if (version1StartsWithDigit == version2StartsWithDigit) {
            doCompareNumeric = version1StartsWithDigit;
            version1Part = version1Parts.next();
            version2Part = version2Parts.next();
        } else if (version1StartsWithDigit) {
            version2Part = version2Parts.next();
        } else if (version2StartsWithDigit) {
            version1Part = version1Parts.next();
        }

        // Compare parts, alternating between numerical and lexical
        // comparision.
        while (!version1Part.isEmpty() || !version2Part.isEmpty()) {
            int result;
            if (doCompareNumeric) {
                result = compareNumeric(version1Part, version2Part);
            } else {
                result = compareLexical(version1Part, version2Part);
            }
            if (result != 0) {
                return result;
            }
            version1Part = version1Parts.next();
            version2Part = version2Parts.next();
            doCompareNumeric = !doCompareNumeric;
        }
        return 0;
    }

    /**
     * Infinite iterator. Returns digit and non-digit parts alternately.
     * If source string is exhausted, returns empty strings.
     */
    private static Iterator<String> versionPartsIterator(String version) {
        Matcher matcher = TOKENS.matcher(version);
        return Stream.generate(
            () -> matcher.find()? matcher.group(0) : ""
        ).iterator();
    }

    /**
     * Numerical comparision. Empty strings count as zero.
     */
    private static int compareNumeric(String part1, String part2) {
        int number1 = part1.isEmpty()? 0 : Integer.parseInt(part1);
        int number2 = part2.isEmpty()? 0 : Integer.parseInt(part2);
        return number1 - number2;
    }

    /**
     * Modified lexical comparision as per Debian spec
     */
    private static int compareLexical(String part1, String part2) {
        Iterator<Integer> iter1 = codePointIterator(part1);
        Iterator<Integer> iter2 = codePointIterator(part2);
        int length = Math.max(part1.length(), part2.length());
        for (int i = 0; i < length; i++) {
            int result = iter1.next() - iter2.next();
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    /**
     * Infinite iterator of code points for modified lexical comparision.
     * Translations:
     *      characters        -> their code point
     *      after exhaustion  -> -1
     *      tilde             -> -2
    */
    private static Iterator<Integer> codePointIterator(String source) {
        return Stream.concat(
                    source.codePoints().boxed().map(i -> i == 126 ? -2 : i),
                    Stream.generate(() -> -1)
                ).iterator();
    }

    /**
     * Parses the give specifier string into a {@link Version}.
     *
     * @param specifier
     * @return
     */
    public static Version parse(String specifier) {
        int epoch = 0;
        String upstreamVersion;
        String debianRevision = null;

        try {
            Matcher m = SPECIFIER_PATTERN.matcher(specifier);
            if (!m.matches())
                throw new IllegalArgumentException(I18N.getMessage("Version.cantParseVersion") + ": " + specifier);
            if (m.group(1) != null)
                epoch = Integer.parseInt(m.group(1));
            upstreamVersion = m.group(2);
            debianRevision = m.group(3);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(I18N.getMessage("Version.cantParseVersion") + ": ", e);
        }

        final Version version = new Version();
        version.setEpoch(epoch);
        version.setUpstreamVersion(upstreamVersion);
        version.setDebianRevision(debianRevision);
        return version;
    }

    public boolean isPreview() {
        if (debianRevision == null || debianRevision.isEmpty()) {
            return upstreamVersion.contains("~");
        } else {
            return debianRevision.contains("~");
        }
    }

    public int getEpoch() {
        return epoch;
    }

    public void setEpoch(final int epoch) {
        this.epoch = epoch;
    }

    public String getUpstreamVersion() {
        return upstreamVersion;
    }

    public void setUpstreamVersion(final String upstreamVersion) {
        this.upstreamVersion = upstreamVersion;
    }

    public String getDebianRevision() {
        return debianRevision;
    }

    public void setDebianRevision(final String debianRevision) {
        this.debianRevision = debianRevision;
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (epoch >= 0)
            sb.append(epoch).append(":");
        sb.append(upstreamVersion);
        if (null != debianRevision)
            sb.append("-").append(debianRevision);
        return sb.toString();
    }

    public String toStringWithoutEpoch() {
        StringBuffer sb = new StringBuffer();
        sb.append(upstreamVersion);
        if (null != debianRevision)
            sb.append("-").append(debianRevision);
        return sb.toString();
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof Version && compareTo((Version)o) == 0;
    }

    @Override
    public int hashCode() {
        if (-1 == hashCode)
            hashCode = 638324 ^ epoch ^ (upstreamVersion != null ? upstreamVersion.hashCode() : 0) ^ (debianRevision != null ? debianRevision.hashCode() : 0);

        return hashCode;
    }

    /**
     * Compare this version to another version according to the rules laid down in
     * the debian specification: <br>
     * <a
     * href="http://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Version">
     */
    public int compareTo(Version v) {
        int result;

        result = epoch - v.epoch;
        if (result != 0)
            return result;

        result = compareNullableRevision(upstreamVersion, v.upstreamVersion);
        if (result != 0)
            return result;

        return compareNullableRevision(debianRevision, v.debianRevision);
    }
}
