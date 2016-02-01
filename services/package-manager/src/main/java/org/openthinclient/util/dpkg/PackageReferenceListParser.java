package org.openthinclient.util.dpkg;

import com.google.common.base.Strings;
import org.openthinclient.pkgmgr.I18N;
import org.openthinclient.pkgmgr.db.Version;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackageReferenceListParser {

   private static final Pattern SPECIFIER_PATTERN = Pattern
         // .compile("(\\S+)(?:\\s+\\((<<|<|<=|=|>=|>|>>)\\s+(\\S+)\\))?");
         .compile("(\\S+)(?:\\s+\\((<<|<|<=|=|>=|>|>>)\\s+(\\S+)\\s*\\))?");

   protected PackageReference parseOrReference(String specifier) {
      String segments[] = specifier.split("\\s*\\|\\s*");

      final List<PackageReference.SingleReference> references = new ArrayList<>(segments.length);

      for (String segment : segments) {
         references.add(parseSingle(segment));
      }

      return new PackageReference.OrReference(references);

   }

   public PackageReferenceList parse(String specifier) {

      if (Strings.isNullOrEmpty(specifier))
         return new PackageReferenceList();

      final PackageReferenceList result = new PackageReferenceList();
      String segments[] = specifier.split("\\s*,\\s*");
      for (String segment : segments) {
         if (segment.contains("|"))
            result.add(parseOrReference(segment));
         else
            result.add(parseSingle(segment));
      }

      return result;
   }

   protected PackageReference.SingleReference parseSingle(String specifier) {

      final String name;
      final Version version;
      final PackageReference.Relation relation;

      try {
         specifier = specifier.trim();
         Matcher m = SPECIFIER_PATTERN.matcher(specifier);
         if (!m.matches()) {
            throw new IllegalArgumentException(I18N.getMessage("PackageReference.IllegalArgument") + ": " + specifier);
         }
         name = m.group(1);

         if (m.group(2) != null) {
            // map key to Relation
            relation = PackageReference.Relation.getByTextualRepresentation(m.group(2));
            version = Version.parse(m.group(3));
         } else {
            version = null;
            relation = null;
         }
      } catch (IllegalStateException e) {
         throw new IllegalArgumentException(I18N.getMessage("PackageReference.IllegalArgument") + ": ", e);
      }

      return new PackageReference.SingleReference(name, relation, version);
   }

}
