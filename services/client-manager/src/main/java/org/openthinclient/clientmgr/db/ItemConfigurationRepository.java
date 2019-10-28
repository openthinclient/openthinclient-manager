package org.openthinclient.clientmgr.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemConfigurationRepository extends JpaRepository<ItemConfiguration, Integer> {

}
