package org.openthinclient.util.dpkg;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PackageReferenceListParserTest {

   @Test
   public void testParseSingle() throws Exception {

      final String input = "foo-data";

      PackageReferenceList list = parse(input);

      assertSingle(list, 0, "foo-data");

   }

   @Test
   public void testParseSingleWithVersion() throws Exception {

      final String input = "foo-data (<< 2.15-2)";

      PackageReferenceList list = parse(input);

      assertSingle(list, 0, "foo-data", PackageReference.Relation.EARLIER, "2.15", "2");

   }

   @Test
   public void testParseMultiple() throws Exception {

      final String input = "foo-data, bar, baz";

      PackageReferenceList list = parse(input);

      assertSingle(list, 0, "foo-data");
      assertSingle(list, 1, "bar");
      assertSingle(list, 2, "baz");

   }

   @Test
   public void testParseMultipleWithVersion() throws Exception {

      final String input = "foo-data (<< 2.15-2), bar (= 23-1), baz (>> 15)";

      PackageReferenceList list = parse(input);

      assertSingle(list, 0, "foo-data", PackageReference.Relation.EARLIER, "2.15", "2");
      assertSingle(list, 1, "bar", PackageReference.Relation.EQUAL, "23", "1");
      assertSingle(list, 2, "baz", PackageReference.Relation.LATER, "15", null);

   }

   private void assertSingle(PackageReferenceList list, int index, String packageName, PackageReference.Relation relation, String upstreamVersion,
         String debianRevision) {

      assertSingle(list, index, packageName);

      final PackageReference.SingleReference ref = (PackageReference.SingleReference) list.get(index);

      assertEquals("Expected " + index + " to be a relation of type " + relation.getTextualRepresentation(), relation, ref.getRelation());

      assertEquals("Expected " + index + " to reference the upstream version " + upstreamVersion, upstreamVersion, ref.getVersion().getUpstreamVersion());
      assertEquals("Expected " + index + " to specify the debian revision " + debianRevision, debianRevision, ref.getVersion().getDebianRevision());
   }

   private void assertSingle(PackageReferenceList list, int index, String packageName) {

      final PackageReference ref = list.get(index);

      assertTrue("Expected " + index + " to be a " + PackageReference.SingleReference.class.getSimpleName(), ref instanceof PackageReference.SingleReference);

      assertEquals("Expected " + index + " to reference package " + packageName, packageName, ((PackageReference.SingleReference) ref).getName());

   }

   private PackageReferenceList parse(String input) {

      return new PackageReferenceListParser().parse(input);

   }
}
