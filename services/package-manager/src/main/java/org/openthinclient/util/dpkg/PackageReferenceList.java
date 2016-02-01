package org.openthinclient.util.dpkg;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PackageReferenceList extends ArrayList<PackageReference> {

   @Override
   public String toString() {
      return stream().map(PackageReference::toString).collect(Collectors.joining(", "));
   }

   /**
    * Checks if this {@link PackageReferenceList} references the given {@link Package}.
    *
    * @param pkg the {@link Package} to be checked
    * @return <code>true</code> if the {@link Package} is referenced in this {@link PackageReferenceList}
    */
   public boolean isReferenced(Package pkg) {

      return stream().flatMap(ref -> {
         if (ref instanceof PackageReference.SingleReference) {
            return Stream.of(((PackageReference.SingleReference) ref));
         } else {
            return ((PackageReference.OrReference) ref).getReferences().stream();
         }
      }).filter(ref -> ref.matches(pkg)).findFirst().isPresent();

   }
}
