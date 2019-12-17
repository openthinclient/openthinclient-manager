package org.openthinclient.service.common.license;

import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.*;

@Entity
@Table(name = "otc_license_errors")
@Access(AccessType.FIELD)
public class LicenseError implements Serializable {
  @Id
  @GeneratedValue(strategy= GenerationType.AUTO, generator="native")
  @GenericGenerator(name = "native", strategy = "native")
  private Long id;

  @Column
  public LocalDateTime datetime;

  @Column
  @Enumerated(EnumType.STRING)
  public ErrorType type;

  public static enum ErrorType {
    UPDATED,
    NO_LICENSE,
    DECRYPTION_ERROR,
    SERVER_ID_ERROR,
    NETWORK_ERROR,
    SERVER_ERROR;
  }

  protected LicenseError() {}

  LicenseError(ErrorType errorType, String errorDetails) {
    this.datetime = LocalDateTime.now();
    this.type = errorType;
  }
}
