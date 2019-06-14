package org.openthinclient.service.common.license;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

@Component
public interface LicenseErrorRepository extends JpaRepository<LicenseError, Long> {
  public void deleteByDatetimeBefore(LocalDateTime datetime);
  public List<LicenseError> findByOrderByDatetimeDesc();
}
