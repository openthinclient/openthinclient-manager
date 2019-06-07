package org.openthinclient.service.common.license;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

@Component
public interface LicenseErrorRepository extends JpaRepository<LicenseError, Long> {
}
