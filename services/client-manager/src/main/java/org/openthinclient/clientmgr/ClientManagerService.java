package org.openthinclient.clientmgr;

import org.openthinclient.clientmgr.db.Item;
import org.openthinclient.clientmgr.db.ItemConfiguration;
import org.openthinclient.clientmgr.db.ItemConfigurationRepository;
import org.openthinclient.clientmgr.db.ItemRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ClientManagerService {

  private ItemRepository itemRepository;
  private ItemConfigurationRepository itemConfigurationRepository;

  public ClientManagerService(ItemRepository itemRepository, ItemConfigurationRepository itemConfigurationRepository) {
    this.itemRepository = itemRepository;
    this.itemConfigurationRepository = itemConfigurationRepository;
  }

  public int count(Item.Type type) {
    return itemRepository.countByType(type);
  }

  public Set<Item> findAll(Item.Type type) {
    return itemRepository.findAllByType(type);
  }

  public Set<ItemConfiguration> getItemConfiguration(Long itemId) {
    return itemConfigurationRepository.findByItemId(itemId);
  }

  public Item createItem(String name, String comment, Item.Type type) {
    return itemRepository.save(new Item(name, comment, type));
  }

  public Item saveItem(Item item) {
    return itemRepository.save(item);
  }

  public List<ItemConfiguration> saveItemConfigurations(Set<ItemConfiguration> itemConfigurations) {
    return itemConfigurationRepository.saveAll(itemConfigurations);
  }

  public Set<Item> findByHwAddress(String hwAddressString) {
    return itemRepository.findByHwAddress(hwAddressString);
  }

  public List<Item> findByName(String name) {
    return itemRepository.findByName(name);
  }

  public List<Item> saveItems(Item... items) {
    return itemRepository.saveAll(Arrays.asList(items));
  }
}
