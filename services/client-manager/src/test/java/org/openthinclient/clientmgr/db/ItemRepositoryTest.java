package org.openthinclient.clientmgr.db;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.clientmgr.ClientManagerDatabaseConfiguration;
import org.openthinclient.clientmgr.ClientManagerInMemoryDatabaseConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ClientManagerInMemoryDatabaseConfiguration.class})
public class ItemRepositoryTest {

  @Autowired
  ItemRepository itemRepository;
  @Autowired
  ItemConfigurationRepository itemConfigurationRepository;

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testCreateItem() {
    assertTrue(itemRepository.findAll().isEmpty());

    Item item = new Item();
    item.setName("TC ABC");
    item.setComment("bdcjbsdjkcbsdkx");
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
    item.setComment("bdcjbsdjkcbsdkx");
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

}