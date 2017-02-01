package org.openthinclient.api.importer.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProfileReferenceTest {

  @Test
  public void testParse() throws Exception {

    final ProfileReference ref = ProfileReference.parse("application:MyApplication");

    assertEquals(ProfileType.APPLICATION, ref.getType());
    assertEquals("MyApplication", ref.getName());


  }

  @Test
  public void testSerialize() throws Exception {

    assertEquals("hardware_type:My Simple Hardware", new ProfileReference(ProfileType.HARDWARE_TYPE, "My Simple Hardware").getCompactRepresentation());


  }
}