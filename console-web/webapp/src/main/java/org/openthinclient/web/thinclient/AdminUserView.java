package org.openthinclient.web.thinclient;

import javax.annotation.PostConstruct;

import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.User;
import org.openthinclient.service.store.LDAPConnection;
import org.openthinclient.web.Audit;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.exception.ProfileNotDeletedException;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.SettingsUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import com.vaadin.server.SerializableComparator;
import com.vaadin.spring.annotation.SpringView;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
@SpringView(name = AdminUserView.NAME, ui= SettingsUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT,
             captionCode = "UI_ADMIN_USER_HEADER", order = 15)
public final class AdminUserView extends AbstractUserView {
  private static final Logger LOG = LoggerFactory.getLogger(AdminUserView.class);

  public static final String NAME = "admin_user_view";
  public static final String ICON = "icon/user.svg";
  public static final ConsoleWebMessages TITLE_KEY = UI_ADMIN_USER_HEADER;


  private static User toUser(Map<String, String> userMap) {
    if (userMap == null) {
      return null;
    }
    User user = new User();
    user.setDn(userMap.get("dn"));
    user.setName(userMap.get("name"));
    user.setDescription(userMap.get("description"));
    return user;
  }


  @PostConstruct
  public void setup() {
    isAdminView = true;
    addStyleName(NAME);
  }

  @Override
  public Set<User> getAllItems() {
    Collection<Map<String, String>> userMaps;
    Collection<String> adminDNs;
    try (LDAPConnection connection = new LDAPConnection()) {
      userMaps = connection.loadAllUsers();
      adminDNs = connection.loadAdminDNs();
    } catch (Exception ex) {
      LOG.error("Could not load users", ex);
      return Collections.emptySet();
    }
    Set<User> users = new java.util.HashSet<>();
    for (Map<String, String> userMap : userMaps) {
      User user = toUser(userMap);
      if (adminDNs.contains(user.getDn())) {
        user.setRole("admin");
      } else {
        user.setRole("user");
      }
      users.add(user);
    }
    return users;
  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(User user) {
    return null;
  }

  @Override
  public User getFreshProfile(String name) {
    try (LDAPConnection connection = new LDAPConnection()) {
      User user = toUser(connection.loadUser(name));
      if (user != null) {
        Collection<String> adminDNs = connection.loadAdminDNs();
        if (adminDNs.contains(user.getDn())) {
          user.setRole("admin");
        } else {
          user.setRole("user");
        }
      }
      return user;
    } catch (Exception ex) {
      LOG.error("Could not load user with name " + name, ex);
      return null;
    }
  }

  @Override
  public void save(User profile) {
    LOG.info("Save: " + profile);
    try (LDAPConnection connection = new LDAPConnection()) {
      byte[] password = profile.getUserPassword();
      profile.setDn(connection.saveUser(
        profile.getDn(),
        profile.getName(),
        profile.getDescription(),
        password != null ? new String(password) : null,
        "admin".equals(profile.getRole())
      ));
    } catch (Exception ex) {
      throw new RuntimeException("Failed to save user", ex);
    }
    Audit.logSave(profile);
  }

  @Override
  public void delete(User profile) throws ProfileNotDeletedException {
    if (isLoggedInUser(profile)) {
      LOG.warn("{} attempted to delete their own profile", profile.getName());
      return;
    }
    try (LDAPConnection connection = new LDAPConnection()) {
      connection.deleteUser(profile.getDn());
    } catch (Exception ex) {
      throw new ProfileNotDeletedException(profile.getName(), ex);
    }
    Audit.logDelete(profile);
  }

  @Override
  public String getViewName() {
    return NAME;
  }

  @Override
  public ConsoleWebMessages getViewTitleKey() {
    return TITLE_KEY;
  }

  @Override
  protected SerializableComparator<DirectoryObject> getComparator() {
    return new SerializableComparator<DirectoryObject>() {
      @Override
      public int compare(DirectoryObject self, DirectoryObject other) {
        if (self instanceof User && other instanceof User) {
          String role = ((User) self).getRole();
          if (!role.equals(((User) other).getRole())) {
            return "admin".equals(role) ? -1 : 1;  // sort admins before users
          }
        }
        return self.getName().compareToIgnoreCase(other.getName());
      }
    };
  }

  @Override
  public void showOverview() {
    super.showOverview(true);
  }

  @Override
  protected void onSave(User profile, boolean success, boolean isNew) {
    // return to overview after saving
    if (success) {
      navigateTo(null);
    }
  }
}
