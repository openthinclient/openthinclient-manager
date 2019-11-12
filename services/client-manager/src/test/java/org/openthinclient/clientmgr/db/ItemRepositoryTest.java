package org.openthinclient.clientmgr.db;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.clientmgr.ClientManagerInMemoryDatabaseConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ClientManagerInMemoryDatabaseConfiguration.class})
@Sql(executionPhase= Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts="classpath:sql/empty-tables.sql")
public class ItemRepositoryTest {

  @Autowired
  ItemRepository itemRepository;
  @Autowired
  ItemConfigurationRepository itemConfigurationRepository;

  @Test
  public void testCreateItem() {
    assertTrue(itemRepository.findAll().isEmpty());

    Item item = new Item();
    item.setName("TC ABC");
    item.setDescription("bdcjbsdjkcbsdkx");
    item.setType(Item.Type.CLIENT);

    Item saved = itemRepository.save(item);
    assertNotNull(saved);
    assertNotNull(saved.getId());
    assertFalse(itemRepository.findAll().isEmpty());
  }

  @Test
  public void testCreateItemConfiguration() {

    Item item = new Item();
    item.setName("TC ABC");
    item.setDescription("bdcjbsdjkcbsdkx");
    item.setType(Item.Type.CLIENT);
    itemRepository.save(item);

    ItemConfiguration ic1 = new ItemConfiguration();
    ic1.setItem(item);
    ic1.setName("secondscreen.rotation");
    ic1.setValue("normal");
    ic1.setType(ItemConfiguration.Type.STRING);
    ItemConfiguration ic2 = new ItemConfiguration();
    ic2.setItem(item);
    ic2.setName("firstscreen.resolution");
    ic2.setValue("1280x1024");
    ic2.setType(ItemConfiguration.Type.STRING);

    List<ItemConfiguration> configurations = itemConfigurationRepository.saveAll(Arrays.asList(ic1, ic2));
    assertEquals(2, configurations.size());
  }

  @Test
  public void testCreateItemMembers() {
    assertTrue(itemRepository.findAll().isEmpty());

    Item client   = itemRepository.save(new Item("TC 1", "Comment", Item.Type.CLIENT));
    Item client2  = itemRepository.save(new Item("TC 2", "Comment", Item.Type.CLIENT));
    Item keyboard = itemRepository.save(new Item("Keyboard", "Comment", Item.Type.DEVICE));

    // add members save
    keyboard.setMembers(Stream.of(client, client2).collect(Collectors.toSet()));
    itemRepository.save(keyboard);

    Optional<Item> keyboardById = itemRepository.findById(keyboard.getId());
    assertTrue(keyboardById.isPresent());
    assertEquals(2, keyboardById.get().getMembers().size());

    // remove first, save, test
    keyboard.getMembers().remove(client);
    itemRepository.save(keyboard);
    assertEquals(1, itemRepository.findById(keyboard.getId()).get().getMembers().size());
  }

  @Test
  public void testFindItems() {
    assertTrue(itemRepository.findAll().isEmpty());

    Item client = itemRepository.save(new Item("TC 1", "Comment", Item.Type.CLIENT));
    Item client2 = itemRepository.save(new Item("TC 2", "Comment", Item.Type.CLIENT));

    Item keyboard = itemRepository.save(new Item("Keyboard", "Comment", Item.Type.DEVICE));
    ItemConfiguration ic1 = new ItemConfiguration(keyboard, "secondscreen.rotation", "normal", ItemConfiguration.Type.STRING);
    ItemConfiguration ic2 = new ItemConfiguration(keyboard, "firstscreen.resolution", "1280x1024", ItemConfiguration.Type.STRING);
    itemConfigurationRepository.saveAll(Arrays.asList(ic1, ic2));

    // find by type
    assertEquals(2, itemRepository.findAllByType(Item.Type.CLIENT).size());
    assertEquals(1, itemRepository.findAllByType(Item.Type.DEVICE).size());
    assertEquals(0, itemRepository.findAllByType(Item.Type.HARDWARETYPE).size());

    // find configurations
    assertEquals(0, itemConfigurationRepository.findByItemId(client.getId()).size());
    assertEquals(0, itemConfigurationRepository.findByItemId(client2.getId()).size());
    assertEquals(2, itemConfigurationRepository.findByItemId(keyboard.getId()).size());

    // count
    assertEquals(2, itemRepository.countByType(Item.Type.CLIENT));
  }

}