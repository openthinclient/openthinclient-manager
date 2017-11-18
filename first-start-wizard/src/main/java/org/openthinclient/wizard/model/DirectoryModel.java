package org.openthinclient.wizard.model;

import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.common.model.User;

public class DirectoryModel {

  private final OrganizationalUnit primaryOU;
  private final User administratorUser;

  public DirectoryModel() {
    // initializing with some defaults

    primaryOU = new OrganizationalUnit();
    primaryOU.setName("openthinclient");
    primaryOU.setDescription("The openthinclient root organizational unit");

    administratorUser = new User();
    administratorUser.setName("administrator");
    administratorUser.setGivenName("Unnamed");
    administratorUser.setSn("Administrator");
    administratorUser.setDescription("The main system administration user");
  }

  public OrganizationalUnit getPrimaryOU() {
    return primaryOU;
  }

  public User getAdministratorUser() {
    return administratorUser;
  }

}
