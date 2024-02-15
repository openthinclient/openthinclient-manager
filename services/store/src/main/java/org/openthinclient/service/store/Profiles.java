package org.openthinclient.service.store;


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  private static Iterable<Map<String, String>> loadDevices(String mac) {
    // Use LinkedHashSet to avoid duplicates.
    // Load all assigned devices in order of precedence.
    Set<Map<String, String>> devices = new LinkedHashSet<>();
    try (LDAPConnection ldapCon = new LDAPConnection()) {
      String clientDN = ldapCon.getClientDNorDefaultDN(mac);
      if (clientDN == null) {
        LOG.warn("No client found for MAC {}", mac);
        return devices;
      }
      devices.addAll(ldapCon.loadDevices(clientDN));
      devices.addAll(ldapCon.loadDevices(
        ldapCon.searchClientgroupDNs(clientDN).toArray(new String[0]))
      );
      String hwTypeDN = ldapCon.searchHwTypeDN(clientDN);
      if (hwTypeDN == null) {
        LOG.warn("No hardware type found for {} ({})", clientDN, mac);
      } else {
        devices.addAll(ldapCon.loadDevices(hwTypeDN));
      }
    } catch (NamingException ex) {
      LOG.error("Error loading devices for MAC {}.", mac, ex);
    }
    return devices;
  }


  public static Iterable<Map<String, String>> getApps(String mac, String userDN) {
    Iterable<Map<String, String>> apps = loadApps(mac, userDN);
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

  private static Iterable<Map<String, String>> loadApps(String mac, String userDN) {
    List<String> relatedDNs = new ArrayList<>();
    try (LDAPConnection ldapCon = new LDAPConnection()) {

      if (mac != null ) {
        String clientDN = ldapCon.getClientDNorDefaultDN(mac);
        if (clientDN == null) {
          LOG.warn("No client found for MAC {}", mac);
        } else {
          relatedDNs.add(clientDN);
          relatedDNs.addAll(ldapCon.searchClientgroupDNs(clientDN));
        }
      }

      if (userDN != null) {
        relatedDNs.add(userDN);
        relatedDNs.addAll(ldapCon.searchUsergroupDNs(userDN));
      }

      relatedDNs.addAll(ldapCon.searchAppgroupDNs(relatedDNs.toArray(new String[0])));
      return ldapCon.loadApplications(relatedDNs.toArray(new String[0]));

    } catch (NamingException ex) {
      LOG.error("Error loading apps for MAC {} and user {}.",
                mac, userDN, ex);
      return Collections.emptyList();
    }
  }

}
