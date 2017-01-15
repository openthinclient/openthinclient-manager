package org.openthinclient.ldap;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.ClientGroup;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.Properties;
import org.openthinclient.common.model.Property;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.UnrecognizedClient;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;

import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MappingTest {
  private static Mapping mapping;

  @BeforeClass
  public static void loadMapping() throws Exception {
    MappingTest.mapping = Mapping.load(MappingTest.class.getResourceAsStream("/org/openthinclient/common/directory/APACHE_DS.xml"));
    assertNotNull(mapping);
    assertEquals("Generic RFC directory server", mapping.getName());

  }

  @Test
  public void testClientCorrect() throws Exception {
    final TypeMapping clientMapping = MappingTest.mapping.getMapping(Client.class);
    assertNotNull(clientMapping);

    assertEquals("ou=clients", clientMapping.getBaseRDN());
    assertEquals("(objectclass=ipHost)", clientMapping.getSearchFilter());
    assertEquals("ipHost", clientMapping.getKeyClass());

    assertAttributeMapping(String.class, "dn", "setDn", "getDn", clientMapping.getDNAttribute());

    assertAttributeMapping(java.lang.String.class, "cn", "setName", "getName", clientMapping.getRDNAttribute());

    assertArrayEquals(new String[]{"top", "device", "ipHost", "ieee802Device"}, clientMapping.getObjectClasses());

    assertAttributeMapping(String.class, "description", "setDescription", "getDescription", clientMapping.getAttributeMappings().get(0));
    assertAttributeMapping(String.class, "ipHostNumber", "setIpHostNumber", "getIpHostNumber", clientMapping.getAttributeMappings().get(1));
    assertAttributeMapping(String.class, "macAddress", "setMacAddress", "getMacAddress", clientMapping.getAttributeMappings().get(2));

    assertManyToOne(Location.class, "l", Cardinality.ZERO_OR_ONE, "setLocation", "getLocation", clientMapping.getAttributeMappings().get(3));
    assertManyToMany(ApplicationGroup.class, "applicationGroups", "uniqueMember", "(uniqueMember={0})", clientMapping.getAttributeMappings().get(4));
  }

  private void assertManyToOne(Class<?> fieldType, String fieldName, Cardinality cardinality, String setterName, String getterName, AttributeMapping am) throws Exception {
    ManyToOneMapping mtom = (ManyToOneMapping) am;

    assertEquals(fieldType, mtom.getRefereeType());
    assertEquals(fieldType, mtom.getFieldType());
    assertEquals(cardinality, mtom.getCardinality());
    assertEquals(setterName, mtom.getSetter().getName());
    assertEquals(getterName, mtom.getGetter().getName());
  }

  private void assertManyToMany(Class<?> fieldType, String fieldName, String memberField, String filter, AttributeMapping am) throws Exception {
    ManyToManyMapping mtom = (ManyToManyMapping) am;

    assertEquals(fieldType, mtom.getPeerType());
    assertEquals(Set.class, mtom.getFieldType());
    assertEquals(memberField, mtom.getMemberField());
    assertEquals(filter, mtom.getFilter());
  }

  private void assertAttributeMapping(Class<String> fieldType, String fieldName, String setterName, String getterName, AttributeMapping am) throws Exception {
    assertEquals(fieldName, am.getFieldName());
    assertEquals(fieldType, am.getFieldType());
    assertEquals(setterName, am.getSetter().getName());
    assertEquals(getterName, am.getGetter().getName());

  }

  @Test
  public void testClientGroupCorrect() throws Exception {
    assertNotNull(mapping.getMapping(ClientGroup.class));
  }

  @Test
  public void testLocationCorrect() throws Exception {
    assertNotNull(mapping.getMapping(Location.class));
  }

  @Test
  public void testApplicationGroupCorrect() throws Exception {
    assertNotNull(mapping.getMapping(ApplicationGroup.class));
  }

  @Test
  public void testApplicationCorrect() throws Exception {
    assertNotNull(mapping.getMapping(Application.class));
  }

  @Test
  public void testHardwareTypeCorrect() throws Exception {
    assertNotNull(mapping.getMapping(HardwareType.class));
  }

  @Test
  public void testDeviceCorrect() throws Exception {
    assertNotNull(mapping.getMapping(Device.class));
  }

  @Test
  public void testPrinterCorrect() throws Exception {
    assertNotNull(mapping.getMapping(Printer.class));
  }

  @Test
  public void testUserCorrect() throws Exception {
    assertNotNull(mapping.getMapping(User.class));
  }

  @Test
  public void testUserGroupCorrect() throws Exception {
    assertNotNull(mapping.getMapping(UserGroup.class));
  }

  @Test
  public void testPropertiesCorrect() throws Exception {
    assertNotNull(mapping.getMapping(Properties.class));
  }

  @Test
  public void testPropertyCorrect() throws Exception {
    assertNotNull(mapping.getMapping(Property.class));
  }

  @Test
  public void testOrganizationalUnitCorrect() throws Exception {
    assertNotNull(mapping.getMapping(OrganizationalUnit.class));
  }

  @Test
  public void testRealmCorrect() throws Exception {
    assertNotNull(mapping.getMapping(Realm.class));
  }

  @Test
  public void testUnrecognizedCorrect() throws Exception {
    assertNotNull(mapping.getMapping(UnrecognizedClient.class));
  }
}