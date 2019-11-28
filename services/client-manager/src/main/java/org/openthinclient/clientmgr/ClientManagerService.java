package org.openthinclient.clientmgr;

import org.openthinclient.clientmgr.db.Item;
import org.openthinclient.clientmgr.db.ItemConfiguration;
import org.openthinclient.clientmgr.db.ItemConfigurationRepository;
import org.openthinclient.clientmgr.db.ItemRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

  /**
   * Save item and members of deep-1
   * @param item Item
   * @return saved Item
   */
  public Item saveItem(Item item) {
    item.getMembers().stream().filter(i -> i.getId() == null).collect(Collectors.toSet()).forEach(item1 -> itemRepository.save(item1));
//    saveMembers(item.getMembers());
    return itemRepository.save(item);
  }

  // TODO: Recursive
  private void saveMembers(Set<Item> items) {
    items.forEach(item -> {
          if (item.getMembers() != null && item.getMembers().size() > 0) {
            saveMembers(item.getMembers());
          }
          if (item.getId() == null) {
            itemRepository.save(item);
          }
        });
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

  public Set<Item> getItemMembers(Item item) {
    return itemRepository.findMembers(item.getId());
  }
}
