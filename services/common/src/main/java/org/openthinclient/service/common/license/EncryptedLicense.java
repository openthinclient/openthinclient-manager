package org.openthinclient.service.common.license;

import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "otc_license")
@Access(AccessType.FIELD)
public class EncryptedLicense implements Serializable {
  @Id
  @GeneratedValue(strategy= GenerationType.AUTO, generator="native")
  @GenericGenerator(name = "native", strategy = "native")
  private Long id;

  @Column
  @Lob
  public String license;

  @Column
  @Lob
  public String encryption_key;
}
