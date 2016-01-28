/*******************************************************************************
 * openthinclient.org ThinClient suite
 * <p/>
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * <p/>
 * <p/>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *******************************************************************************/
package org.openthinclient.util.dpkg;

import org.openthinclient.pkgmgr.I18N;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author levigo
 */
@Embeddable
public class Version implements Comparable, Serializable {

   private static final long serialVersionUID = 3258135760426317876L;
   private static final Pattern SPECIFIER_PATTERN = Pattern.compile("(?:(\\d+)\\:)?([\\w\\+\\.\\:\\-~]+)");

   /**
    * Compare to revision specifiers according to the rules laid down in the
    * debian specification: <br>
    * <a
    * href="http://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Version">
    *
    * @param v1
    * @param v2
    * @return
    */
   private static int compareRevision(String v1, String v2) {
      int i1 = 0, i2 = 0;
      int digitStart1 = -1;
      int digitStart2 = -1;
      while (i1 < v1.length() && i2 < v2.length()) {
         // compare non-digit
         while (i1 < v1.length() && i2 < v2.length() && !Character.isDigit(v1.charAt(i1)) && !Character.isDigit(v2.charAt(i2)) && v1.charAt(i1) == v2
               .charAt(i2)) {
            i1++;
            i2++;
         }

         if (i1 >= v1.length() || i2 >= v2.length() || (!Character.isDigit(v1.charAt(i1)) && Character.isDigit(v2.charAt(i2))))
            break;

         digitStart1 = i1;
         digitStart2 = i2;

         // Compare digits. Find out where the digits end
         while (i1 < v1.length() && Character.isDigit(v1.charAt(i1)))
            i1++;
         while (i2 < v2.length() && Character.isDigit(v2.charAt(i2)))
            i2++;

         // extract numeric values. the empty string counts as zero.
         int d1 = i1 > digitStart1 ? Integer.parseInt(v1.substring(digitStart1, i1)) : 0;
         int d2 = i2 > digitStart2 ? Integer.parseInt(v2.substring(digitStart2, i2)) : 0;

         if (d1 < d2)
            return -1;
         if (d1 > d2)
            return 1;

         digitStart1 = digitStart2 = -1;
      }

      // does the length differ?
      if (i1 < v1.length() && i2 >= v2.length())
         return 1; // shorter -> smaller!
      if (i1 >= v1.length() && i2 < v2.length())
         return -1; // longer -> larger!

      if (digitStart1 >= 0) {
         // does the character differ?
         // we use the natural (unicode) collation order. No need to get fancy.
         if (v1.charAt(i1) < v2.charAt(i2))
            return -1;
         if (v1.charAt(i1) > v2.charAt(i2))
            return 1;
      }

      return 0;
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
         String version = m.group(2);
         int index = version.lastIndexOf('-');
         if (index > 0) {
            upstreamVersion = version.substring(0, index);
            debianRevision = version.substring(index + 1);
         } else
            upstreamVersion = version;
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

   @Column(name="version_epoch")
   private int epoch = 0;
   @Column(name="version_upstream")
   private String upstreamVersion;
   @Column(name="version_revision")
   private String debianRevision;
   private int hashCode = -1;

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

   /*
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override public boolean equals(Object o) {
      return o instanceof Version && compareTo(o) == 0;
   }

   @Override public int hashCode() {
      if (-1 == hashCode)
         hashCode = 638324 ^ epoch ^ (upstreamVersion != null ? upstreamVersion.hashCode() : 0) ^ (debianRevision != null ? debianRevision.hashCode() : 0);

      return hashCode;
   }

   /**
    * Compare this version to another version according to the rules laid down in
    * the debian specification: <br>
    * <a
    * href="http://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Version">
    *
    * @param o
    * @return
    */
   public int compareTo(Object o) {
      if (!(o instanceof Version))
         throw new IllegalArgumentException("Version can't be compared to a " + o.getClass());

      Version v = (Version) o;

      // compare epoch
      if (epoch < v.epoch)
         return -1;
      if (epoch > v.epoch)
         return 1;

      // a non-specified version is always smaller than a specified one
      if (upstreamVersion != null && v.upstreamVersion == null)
         return 1;
      if (upstreamVersion == null && v.upstreamVersion != null)
         return -1;

      if (upstreamVersion != null && v.upstreamVersion != null) {
         int r = compareRevision(upstreamVersion, v.upstreamVersion);
         if (r != 0)
            return r;
      }

      // a non-specified version is always smaller than a specified one
      if (debianRevision != null && v.debianRevision == null)
         return 1;
      if (debianRevision == null && v.debianRevision != null)
         return -1;

      if (debianRevision != null && v.debianRevision != null)
         return compareRevision(debianRevision, v.debianRevision);
      else
         return 0;
   }

}
