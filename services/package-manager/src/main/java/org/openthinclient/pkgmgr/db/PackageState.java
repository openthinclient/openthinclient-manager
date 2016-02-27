package org.openthinclient.pkgmgr.db;

import javax.persistence.*;

@Entity
public class PackageState {

  @Id
  @GeneratedValue
  private Integer id;
  @Column
  @Enumerated(EnumType.STRING)
  private State state;
  @ManyToOne(fetch = FetchType.LAZY)
  private Installation installation;
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "package")
  private Package pkg;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public Installation getInstallation() {
    return installation;
  }

  public void setInstallation(Installation installation) {
    this.installation = installation;
  }

  public Package getPkg() {
    return pkg;
  }

  public void setPkg(Package pkg) {
    this.pkg = pkg;
  }

  public enum State {
    INSTALLED,
    UNINSTALLED
  }
}
