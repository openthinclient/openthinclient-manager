package org.openthinclient.service.common.license;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "otc_license_errors")
@Access(AccessType.FIELD)
public class LicenseError implements Serializable {
  @Id
  @GeneratedValue
  private Long id;

  @Column
  public LocalDateTime datetime;

  @Column
  @Enumerated(EnumType.STRING)
  public ErrorType type;

  @Column
  public String details;

  public static enum ErrorType {
    NO_LICENSE,
    INTERNAL_ERROR,
    DECRYPTION_ERROR,
    NETWORK_ERROR,
    SERVER_ERROR;
  }

  LicenseError(ErrorType errorType, String errorDetails) {
    this.datetime = LocalDateTime.now();
    this.type = errorType;
    this.details = errorDetails;
  }
}
