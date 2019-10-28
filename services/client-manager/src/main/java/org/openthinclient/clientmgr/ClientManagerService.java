package org.openthinclient.clientmgr;

import org.openthinclient.clientmgr.db.ItemConfigurationRepository;
import org.openthinclient.clientmgr.db.ItemRepository;

public class ClientManagerService {

  private ItemRepository itemRepository;
  private ItemConfigurationRepository itemConfigurationRepository;

  public ClientManagerService(ItemRepository itemRepository, ItemConfigurationRepository itemConfigurationRepository) {
    this.itemRepository = itemRepository;
    this.itemConfigurationRepository = itemConfigurationRepository;
  }


}
