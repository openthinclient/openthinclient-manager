package org.openthinclient.service.common.license;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
public interface LicenseRepository extends JpaRepository<EncryptedLicense, Long> {
}
