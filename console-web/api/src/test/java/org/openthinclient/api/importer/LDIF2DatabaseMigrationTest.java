package org.openthinclient.api.importer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.api.importer.model.ProfileType;
import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.api.rest.model.Application;
import org.openthinclient.api.rest.model.ApplicationGroup;
import org.openthinclient.api.rest.model.ClientGroup;
import org.openthinclient.api.rest.model.Device;
import org.openthinclient.api.util.Ldif2JsonModelParser;
import org.openthinclient.clientmgr.ClientManagerService;
import org.openthinclient.clientmgr.db.Item;
import org.openthinclient.clientmgr.db.ItemConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for Ldif2DatabaseMigration
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ClientManagerInMemoryDatabaseConfiguration.class})
//@Sql(executionPhase= Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts="classpath:sql/empty-tables.sql")
public class LDIF2DatabaseMigrationTest {

    @Autowired
    ClientManagerService service;


    /**
     * Real-world use-cases tests
     */
    @Test
    public void testManageItemMemberModel() {


    }

    /**
     * Delete propagation, cascade and integrity testing
     */
    @Test
    public void testDeleteItemMemberModel() {

    }

    /**
     * Store and retrieving Members
     */
    @Test
    public void testMergeItemMemberModel() {

    }

    @Test
    public void testItemMemberModel() {

        Item group1 = new Item("Group1", "", Item.Type.APPLICATION_GROUP);
        Item app1  = new Item("App1", "", Item.Type.APPLICATION);
        Item app2  = new Item("App2", "", Item.Type.APPLICATION);
        service.saveItems(group1, app1, app2);
        group1.setMembers(Stream.of(app1, app2).collect(Collectors.toSet()));
        service.saveItem(group1);

        Item group2 = new Item("Group2", "", Item.Type.APPLICATION_GROUP);
        Item app3  = new Item("App3", "", Item.Type.APPLICATION);
        group2.setMembers(Stream.of(app1, app3).collect(Collectors.toSet()));
        service.saveItem(app3);
        service.saveItem(group2);

        // test
        List<Item> testGroup2 = service.findByName("Group2");
        assertNotNull(testGroup2);
        assertEquals(1, testGroup2.size());
        Set<Item> members = service.getItemMembers(testGroup2.get(0));
        assertEquals(2, members.size());



    }

    @Test
    public void testLdif2Database() throws Exception {

//        File file = new File(LDIF2DatabaseMigrationTest.class.getResource("/pales-old.ldif").toURI());
        File file = new File(LDIF2DatabaseMigrationTest.class.getResource("/VA-2.2.7-last-export.ldif").toURI());
        String envDN = SchemaProfileTest.envDN;
        Pair<String, String> replacement = Pair.of("#%BASEDN%#", envDN);
        Ldif2JsonModelParser f2jmp = new Ldif2JsonModelParser(file, envDN, replacement);
        List<AbstractProfileObject> result = f2jmp.parse();
        assertNotNull(result);

        // map ldap-objects to jpa-models
        for (AbstractProfileObject apo : result) {
            Item item = new Item(apo.getName(), apo.getDescription(), getType(apo.getType()));

            Map<String, Object> configurations = apo.getConfiguration().getAdditionalProperties();
            Set<ItemConfiguration> itemConfigurations = configurations.entrySet()
                            .stream()
                            .filter(entry -> !entry.getKey().equals("schema_version"))
                            .map(entry -> new ItemConfiguration(item, entry.getKey(), entry.getValue().toString(), ItemConfiguration.Type.STRING))
                            .collect(Collectors.toSet());

            // TODO members
            Set<String> members = new HashSet<>();
            ProfileType profileType = apo.getType();
            if (profileType == ProfileType.DEVICE) {
                members.addAll(((Device) apo).getMembers());
            } else if (profileType == ProfileType.APPLICATION) {
                members.addAll(((Application) apo).getMembers());
            } else if (profileType == ProfileType.APPLICATION_GROUP) {
                members.addAll(((ApplicationGroup) apo).getMembers());
            } else if (profileType == ProfileType.CLIENT_GROUP) {
                members.addAll(((ClientGroup) apo).getMembers());
            }
            members.forEach(member -> {
                List<Item> itemByName = service.findByName(member);
                Item item1 = null;
                if (itemByName.size() > 0) {
                    item1 = itemByName.get(0);
                } else {
                    // find the Item in migrated objects
                    Optional<AbstractProfileObject> first = result.stream().filter(abstractProfileObject -> abstractProfileObject.getName().equals(member)).findFirst();
                    if (first.isPresent()) {
                        AbstractProfileObject abstractProfileObject = first.get();
                        item1 = new Item(abstractProfileObject.getName(), abstractProfileObject.getDescription(), getType(abstractProfileObject.getType()));
                    } else {
                        System.out.println("Item not found for name: " + member);
                    }
                }
                if (item1 != null) {
                    item.getMembers().add(item1);
                }
            });
//            System.out.println("SAVE = " + item);
            service.saveItem(item);
            service.saveItemConfigurations(itemConfigurations);


        }

        // TODO Tests 4all
        StringBuilder sb = new StringBuilder("---\n");
        Stream.of(Item.Type.values()).forEach(type -> {
            Set<Item> itemSet = service.findAll(type);
            itemSet.forEach(a -> {
                sb.append(a.getName()).append(" (").append(a.getType()).append(")\n Konfig:\n");
                service.getItemConfiguration(a.getId()).forEach(c -> sb.append("   ").append(c).append("\n"));
                sb.append(" Members:\n");
                service.getItemMembers(a).forEach(m -> sb.append("   ").append(m).append("\n"));
                sb.append("----\n");
            });
        });
        System.out.println(sb.toString());
    }

    private Item.Type getType(ProfileType type) {
        switch (type) {
            case APPLICATION:  return Item.Type.APPLICATION;
            case DEVICE:       return Item.Type.DEVICE;
            case HARDWARETYPE: return Item.Type.HARDWARETYPE;
            case CLIENT:       return Item.Type.CLIENT;
            case LOCATION:     return Item.Type.LOCATION;
            case PRINTER:      return Item.Type.PRINTER;
            case APPLICATION_GROUP: return Item.Type.APPLICATION_GROUP;
            case CLIENT_GROUP:      return Item.Type.CLIENT_GROUP;
        }
        return null;
    }

}
