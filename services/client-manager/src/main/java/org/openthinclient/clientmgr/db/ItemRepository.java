package org.openthinclient.clientmgr.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

  Set<Item> findAllByType(Item.Type type);

  List<Item> findByName(String name);

  @Query("SELECT COUNT(i) FROM Item i WHERE i.type=?1")
  int countByType(Item.Type type);

  @Query(value = "SELECT * FROM otc_item WHERE id in (SELECT item_id FROM otc_item_configuration WHERE name='macAddress' and value = ?1)", nativeQuery = true)
  Set<Item> findByHwAddress(String hwAddressString);
}
