package org.openthinclient.service.common.license;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public interface LicenseErrorRepository extends JpaRepository<LicenseError, Long> {
  @Transactional
  public void deleteByDatetimeBefore(LocalDateTime datetime);
  public List<LicenseError> findTop25ByOrderByDatetimeDesc();
}
