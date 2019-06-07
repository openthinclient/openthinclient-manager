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

  @Column
  public String license;

  @Column
  public String encryption_key;
}
