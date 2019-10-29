package org.openthinclient.clientmgr.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface ItemConfigurationRepository extends JpaRepository<ItemConfiguration, Long> {

  @Query(value = "SELECT * FROM otc_item_configuration WHERE item_id = ?1", nativeQuery = true)
  Set<ItemConfiguration> findByItemId(Long itemId);
}
