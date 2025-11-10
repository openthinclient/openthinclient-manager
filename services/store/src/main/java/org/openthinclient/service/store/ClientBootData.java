package org.openthinclient.service.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

/**
 * Provides client data required by proxyDHCP's *PXEServices and the
 * PXEConfigurationTFTPProvider.
 *
 * A ClientBootData object can be acquired from the static load(mac)
 * method.
 * The get(key, default) method return the property value from the
 * client or inherited from its hardware type, its location and the
 * realm or the default from the schemas (along the same inheritance
 * chain).
 *
 * Properties from hardware type, location and realm are cached for
 * 2 seconds. ClientBootData objects are also kept for 2 seconds to
 * cushion against repeated DHCP and TFTP requests / negotiations.
 */
public class ClientBootData {
  private static final Logger LOG = LoggerFactory.getLogger(ClientBootData.class);

  /** Cache for profile properties of hardware types, locations and realm */
  private static final Cache<String, Map<String, String>> propsCache =
      Caffeine.newBuilder()
      .expireAfterWrite(2, TimeUnit.SECONDS)
      .build();

  /**
   * Cached Loader for ClientBootData objects.
   */
  private static final LoadingCache<String, ClientBootData> cachedLoader =
      Caffeine.newBuilder()
      .expireAfterWrite(2, TimeUnit.SECONDS)
      .build(mac -> {
        try (LDAPConnection ldapCon = new LDAPConnection()) {

          LDAPConnection.ClientData client = ldapCon.loadClientData(mac);
          if (client == null) {
            LOG.debug("No client found for MAC {}.", mac);
            return null;
          }

          if (client.locationDN == null) {
            LOG.warn("No location set for {}, Ignoring client.", client.dn);
            return null;
          }

          String hwTypeDN = ldapCon.searchHwTypeDN(client.dn);
          if (hwTypeDN == null) {
            LOG.warn("No hwtype found for {}, Ignoring client.", client.dn);
            return null;
          }

          final List<Map<String, String>> props = new ArrayList<>(4);
          Map<String, String> clientProfile = ldapCon.loadProfile(client.dn);
          clientProfile.put("name", client.name);
          clientProfile.put("mac", mac);
          clientProfile.put("ip", client.ip);
          clientProfile.put("description", client.description);
          props.add(clientProfile);

          for (String dn: new String[]{hwTypeDN, client.locationDN, LDAPConnection.REALM_DN}){
            Map<String, String> map = propsCache.get(dn, ldapCon::loadProfile);
            if (map == null) {
              LOG.error("Failed to load profile of {}", dn);
              return null;
            }
            props.add(map);
          }

          return new ClientBootData(client.dn, client.ip, props);
        }
      });


  /**
   * Get ClientBootData for given MAC address.
   *
   * @return  new or cached ClientBootData for mac or null if loading
   *          from LDAP fails or nothing was found.
   */
  public static ClientBootData load(String mac) {
    return cachedLoader.get(mac);
  }


  private String dn;
  private String ip;
  private List<Map<String, String>> props;

  /**
   * ClientBootData is not supoosed to be instanciated directly (except
   * in tests). Use ClientBootData.load(mac) instead.
   */
  public ClientBootData(String dn, String ip, List<Map<String, String>> props) {
    this.dn = dn;
    this.ip = ip;
    this.props = props;
  }


  /**
   * Get value for key from client or inherited value from hardware type,
   * location or realm (in this order). If no value was found return
   * default value from schema (again following the above hierarchy).
   * If also no default value was found return the given orElse.
   */
  public String get(String key, String orElse) {
    for (Map<String, String> map : props) {
      String value = map.get(key);
      if (value != null) {
        return value;
      }
    }
    return SchemaStore.getClientBootDefaults().getOrDefault(key, orElse);
  }


  public Map<String, String> getAll() {
    Map<String, String> result = new HashMap<>();
    for (String key: new String[]{"name", "mac", "ip", "description"}) {
      result.put(key, get(key, null));
    }
    for (String key: SchemaStore.getClientKeys()) {
      result.put(key, get(key, null));
    }
    return result;
  }


  /**
   * IP from LDAP entry
   */
  public String getIP() {
    return ip;
  }

  /**
   * Udpate / set IP in LDAP entry
   */
  public void saveIP(String ip) throws InterruptedException, NamingException {
    if (this.ip.equals(ip)) {
      return;
    }
    try (LDAPConnection ldapCon = new LDAPConnection()) {
      ldapCon.saveIP(dn, ip);
    }
    this.ip = ip;
  }
}
