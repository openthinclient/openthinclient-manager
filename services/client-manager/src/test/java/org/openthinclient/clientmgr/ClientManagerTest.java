package org.openthinclient.clientmgr;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.clientmgr.db.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ClientManagerInMemoryDatabaseConfiguration.class})
@Sql(executionPhase= Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts="classpath:sql/empty-tables.sql")
public class ClientManagerTest {

  @Autowired
  ClientManagerService service;

  @Test
  /** test some DirectoryObjectService use-cases */
  public void testDirectoryObjectServiceUseCases() {

    // test  save(T object);
    Item client   = service.saveItem(new Item("TC 1", "Comment", Item.Type.CLIENT));
    Item client2  = service.saveItem(new Item("TC 2", "Comment", Item.Type.CLIENT));
    Item keyboard = service.saveItem(new Item("Keyboard", "Comment", Item.Type.DEVICE));

    // test findAll();
    assertEquals(2, service.findAll(Item.Type.CLIENT).size());
    assertEquals(1, service.findAll(Item.Type.DEVICE).size());

    // test  findByName(String name);
    // do we need this?

    // test  count();
    assertEquals(2, service.count(Item.Type.CLIENT));
    assertEquals(1, service.count(Item.Type.DEVICE));

    // test  queryNames();
    // do we need this?

  }

}
