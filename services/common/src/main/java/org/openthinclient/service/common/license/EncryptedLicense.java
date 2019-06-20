package org.openthinclient.service.common.license;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "otc_license")
@Access(AccessType.FIELD)
public class EncryptedLicense implements Serializable {
  @Id
  @GeneratedValue
  private Long id;

  @Column(length = 32672)
  public String license;

  @Column(length = 4096)
  public String encryption_key;
}
