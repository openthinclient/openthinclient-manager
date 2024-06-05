package org.openthinclient.service.store;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Profiles {
  private static final Logger LOG = LoggerFactory.getLogger(Profiles.class);

  public static Iterable<Map<String, String>> getDevices(String mac) {
    Iterable<Map<String, String>> devices = loadDevices(mac);
    for (Map<String, String> device: devices) {
      String type = device.get("type");
      if (type == null) {
        LOG.warn("Device {} has no type", device.get("name"));
        continue;
      }
      Map<String, String> schema = SchemaStore.getSchema(type);
      if (schema == null) {
        LOG.warn( "No schema for type {} of device {}.",
          type, device.get("name"));
        continue;
      }
      schema.forEach((key, value) ->
                  device.computeIfAbsent(key, k -> value));
    }
    return devices;
  }

  /**
   * Load all assigned devices for the given MAC.
   *
   * The devices are loaded in order of precedence. The order is:
   *
   * <pre>
   *   1.  client
   *   2.    |-- hwtype
   *   3.    '-- clientgroups
   * </pre>
   *
   * @param mac MAC address of the client
   * @return all assigned devices as a list of maps
   */
  private static Iterable<Map<String, String>> loadDevices(String mac) {
    // Load all assigned devices in order of precedence.
    List<Map<String, String>> devices = new ArrayList<>();

    try (LDAPConnection ldapCon = new LDAPConnection(true)) {
      String clientDN = ldapCon.getClientDNorDefaultDN(mac);
      if (clientDN == null) {
        LOG.warn("No client found for MAC {}", mac);
        return devices;
      }
      devices.addAll(ldapCon.loadDevices("client", clientDN));

      String hwTypeDN = ldapCon.searchHwTypeDN(clientDN);
      if (hwTypeDN == null) {
        LOG.warn("No hardware type found for {} ({})", clientDN, mac);
      } else {
        devices.addAll(ldapCon.loadDevices("hwtype", hwTypeDN));
      }

      devices.addAll(ldapCon.loadDevices("clientgroup",
          ldapCon.searchClientgroupDNs(clientDN).toArray(new String[0])));

    } catch (NamingException ex) {
      LOG.error("Error loading devices for MAC {}.", mac, ex);
    }
    return devices;
  }


  public static List<Map<String, String>> getApps(String mac, String userDN) {
    List<Map<String, String>> apps = loadApps(mac, userDN);
    for (Map<String, String> app : apps) {
      String type = app.get("type");
      if (type == null) {
        LOG.warn("App {} has no type", app.get("name"));
        continue;
      }
      Map<String, String> schema = SchemaStore.getSchema(type);
      if (schema == null) {
        LOG.warn( "No schema for type {} of app {}.",
                  type, app.get("name"));
        continue;
      }
      schema.forEach((key, value) ->
                  app.computeIfAbsent(key, k -> value));
    }
    return apps;
  }

  /**
   * Load all assigned applications for the given MAC and userDN.
   *
   * The applications are loaded in order of precedence. The order is:
   *
   * <pre>
   *   1.  user
   *   2.    |-- appgroups
   *   3.    '-- usergroups
   *   4.          '-- appgroups
   *   5.  client
   *   6.    |-- appgroups
   *   7.    '-- clientgroups
   *   8.         '-- appgroups
   * </pre>
   *
   * @param mac    MAC address of the client or null
   * @param userDN full DN of the user or null
   * @return all assigned applications as a list of maps
   */
  private static List<Map<String, String>> loadApps(String mac, String userDN) {
    List<Map<String, String>> apps = new ArrayList<>();

    try (LDAPConnection ldapCon = new LDAPConnection(true)) {
      if (userDN != null) {
        apps.addAll(ldapCon.loadApplications("user", userDN));
        apps.addAll(ldapCon.loadApplications("user-appgroup",
            ldapCon.searchAppgroupDNs(userDN).toArray(new String[0])));
        String[] usergroupDNs = ldapCon.searchUsergroupDNs(userDN)
                                .toArray(new String[0]);
        apps.addAll(ldapCon.loadApplications("usergroup", usergroupDNs));
        apps.addAll(ldapCon.loadApplications("usergroup-appgroup",
            ldapCon.searchAppgroupDNs(usergroupDNs).toArray(new String[0])));
      }
      if (mac != null) {
        String clientDN = ldapCon.getClientDNorDefaultDN(mac);
        if (clientDN == null) {
          LOG.warn("No client found for MAC {}", mac);
        } else {
          apps.addAll(ldapCon.loadApplications("client", clientDN));
          apps.addAll(ldapCon.loadApplications("client-appgroup",
              ldapCon.searchAppgroupDNs(clientDN).toArray(new String[0])));
          String[] clientgroupDNs = ldapCon.searchClientgroupDNs(clientDN)
                                    .toArray(new String[0]);
          apps.addAll(ldapCon.loadApplications("clientgroup", clientgroupDNs));
          apps.addAll(ldapCon.loadApplications("clientgroup-appgroup",
              ldapCon.searchAppgroupDNs(clientgroupDNs).toArray(new String[0])));
        }
      }
    } catch (NamingException ex) {
      LOG.error("Error loading apps for MAC {} and user {}.",
                mac, userDN, ex);
    }
    return apps;
  }


  public static List<Map<String, String>> getPrinters(String mac, String userDN) {
    List<Map<String, String>> printers = loadPrinters(mac, userDN);
    for (Map<String, String> printer : printers) {
      String type = printer.get("type");
      if (type == null) {
        LOG.warn("Printer {} has no type", printer.get("name"));
        continue;
      }
      Map<String, String> schema = SchemaStore.getSchema(type);
      if (schema == null) {
        LOG.warn("No schema for type {} of printer {}.",
            type, printer.get("name"));
        continue;
      }
      schema.forEach((key, value) -> printer.computeIfAbsent(key, k -> value));
    }
    return printers;
  }

  /**
   * Load all assigned printers for the given MAC and userDN.
   *
   * The printers are loaded in order of precedence. The order is:
   *
   * <pre>
   *   1.  user
   *   3.    '-- usergroups
   *   5.  client
   *   6.    '-- location
   * </pre>
   *
   * @param mac    MAC address of the client or null
   * @param userDN full DN of the user or null
   * @return all assigned printers as a list of maps
   */
  private static List<Map<String, String>> loadPrinters(String mac, String userDN) {
    List<Map<String, String>> printers = new ArrayList<>();

    try (LDAPConnection ldapCon = new LDAPConnection(true)) {
      if (userDN != null) {
        printers.addAll(ldapCon.loadPrinters("user", userDN));
        printers.addAll(ldapCon.loadPrinters("usergroup",
            ldapCon.searchUsergroupDNs(userDN).toArray(new String[0])));
      }
      if (mac != null) {
        String[] clientAndLocationDNs = ldapCon.getClientAndLocationDNs(mac);
        if (clientAndLocationDNs == null) {
          LOG.warn("No client found for MAC {}", mac);
        } else {
          printers.addAll(ldapCon.loadPrinters( "client",
                                                clientAndLocationDNs[0]));
          if (clientAndLocationDNs[1] == null) {
            LOG.warn("No location found for MAC {}", mac);
          } else {
            printers.addAll(ldapCon.loadPrinters( "location",
                                                  clientAndLocationDNs[1]));
          }
        }
      }
    } catch (NamingException ex) {
      LOG.error("Error loading printers for MAC {} and user {}.",
          mac, userDN, ex);
    }
    return printers;
  }

}
