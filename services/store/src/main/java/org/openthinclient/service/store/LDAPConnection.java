package org.openthinclient.service.store;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulation of a "direct" LDAP connection to the embedded LDAP server
 */
public class LDAPConnection implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(LDAPConnection.class);

  // Basic configuration setup
  private static final DirectoryServiceConfiguration dsc;
  public static final String BASE_DN;
  public static final String REALM_DN;
  static {
    dsc = (new ManagerHomeFactory()).create().getConfiguration(
        DirectoryServiceConfiguration.class);
    BASE_DN = String.format("ou=%s,%s",
                            dsc.getPrimaryOU(),
                            dsc.getEmbeddedCustomRootPartitionName());
    REALM_DN = "ou=RealmConfiguration," + BASE_DN;
  }


  // Return type of Connection.loadClientData
  public static class ClientData {
    public String dn;
    public String name;
    public String description;
    public String locationDN;
    public String ip;
  }


  // Helper functions
  private static SearchControls SC(long countLimit, String... attrs) {
    return new SearchControls(SearchControls.ONELEVEL_SCOPE,
                              countLimit,
                              500 /* time limit in ms */,
                              attrs,
                              false /* don'return bound object */,
                              true  /* dereference links */);
  }
  private static SearchControls singleSC(String... a) {return SC(1, a);}
  private static SearchControls multiSC(String... a) {return SC(0, a);}

  private static Map<Integer, String>
  uniquememberFilters = new ConcurrentHashMap<>();
  /**
   * Generate a parameterized OR filter for uniquemembers and cache it to
   * avoid repeatedly building the same filters.
   */
  private static String getUniquememberFilter(int length) {
    return uniquememberFilters.computeIfAbsent(length, l -> {
      StringBuilder sb = new StringBuilder("(|");
      for (int i = 0; i < l; i++) {
        sb.append("(uniquemember={").append(i).append("})");
      }
      sb.append(")");
      return sb.toString();
    });
  }


  // The actual instance

  private LdapContext ctx;

  public LDAPConnection() throws NamingException {
    ctx = (ServerLdapContext) DirectoryService.getInstance().getJndiContext(
        new LdapDN(dsc.getContextSecurityPrincipal()),
        dsc.getContextSecurityPrincipal(),
        dsc.getContextSecurityCredentials().getBytes(),
        "simple",
        "");
  }


  private static final String REALM_PROPS_DN = "nismapname=profile," + REALM_DN;
  private static final SearchControls NISMAPENTRY_SC = singleSC("nismapEntry");
  /**
   * @return whether the default client profile (with MAC 00:00:00:00:00:00)
   * should be served if no profile exists for the requesting MAC
   */
  private boolean isDefaultClientEnabled() throws NamingException {
    NamingEnumeration<SearchResult> r;
    r = ctx.search( REALM_PROPS_DN,
                    "(cn=BootOptions.PXEServicePolicy)",
                    NISMAPENTRY_SC);

    if (r.hasMore()) {
      return "AnyClient".equals(
          r.next().getAttributes().get("nismapEntry").get());
    }
    return false;
  }


  private static final String CLIENT_DN = "ou=clients," + BASE_DN;

  private static final SearchControls CLIENT_COUNT_SC = multiSC();
  /**
   * @return number of clients
   */
  public int countClients() throws NamingException {
    NamingEnumeration<SearchResult> results;
    results = ctx.search( CLIENT_DN,
                          "(objectClass=device)",
                          CLIENT_COUNT_SC);
    int count = 0;
    while (results.hasMore()) {
      count++;
      results.next();
    }
    return count;
  }

  private static final SearchControls CLIENT_SC = singleSC(
      "l", "iphostnumber", "description", "cn");
  /**
   * Find client entry for given MAC address and return (selected)
   * attributes as LDAPConnection.ClientData object.
   *
   * @return ClientData for mac or null if nothing was found
   */
  public ClientData loadClientData(String mac) throws NamingException {
    NamingEnumeration<SearchResult> results;
    results = ctx.search( CLIENT_DN,
                          "(macaddress={0})", new String[] { mac },
                          CLIENT_SC);

    boolean found = results.hasMore();
    if (!found && isDefaultClientEnabled()) {
      results = ctx.search( CLIENT_DN,
                            "(macaddress={0})",
                            new String[] { ClientService.DEFAULT_CLIENT_MAC },
                            CLIENT_SC);
      found = results.hasMore();
    }
    if (!found)
      return null;

    SearchResult r = results.next();
    Attributes attrs = r.getAttributes();

    ClientData clientData = new ClientData();
    clientData.dn = r.getName();
    clientData.locationDN = (String) attrs.get("l").get();
    clientData.ip = (String) attrs.get("iphostnumber").get();
    Attribute description = attrs.get("description");
    if (description != null) {
      clientData.description = (String) description.get();
    }
    clientData.name = (String) attrs.get("cn").get();

    return clientData;
  }


  private static final SearchControls CLIENT_DN_SC = singleSC();
  /**
   * @return DN of client for given MAC address or null if nothing was found
   */
  public String searchClientDN(String mac) throws NamingException {
    NamingEnumeration<SearchResult> r;
    r = ctx.search( CLIENT_DN,
                    "(macaddress={0})", new String[] { mac },
                    CLIENT_DN_SC);
    return r.hasMore() ? r.next().getName() : null;
  }

  /**
   * @return either DN of client, DN of default client (MAC 00:00:00:00:00:00)
   * if no client with given mac was found and default client is enabled, or
   * null
   */
  public String getClientDNorDefaultDN(String mac) throws NamingException {
    String clientDN = searchClientDN(mac);
    if (clientDN == null && isDefaultClientEnabled()) {
      clientDN = searchClientDN(ClientService.DEFAULT_CLIENT_MAC);
    }
    return clientDN;
  }

  private static final String HWTYPE_DN = "ou=hwtypes," + BASE_DN;
  private static final SearchControls HWTYPE_SC = singleSC();
  /**
   * @return DN of hardware type for the client or null if nothing was found
   */
  public String searchHwTypeDN(String clientDN) throws NamingException {
    NamingEnumeration<SearchResult> r;
    r = ctx.search( HWTYPE_DN,
                    "(uniquemember={0})", new String[] { clientDN },
                    HWTYPE_SC);
    return r.hasMore() ? r.next().getName() : null;
  }

  private static final String CLIENTGROUPS_DN = "ou=clientgroups," + BASE_DN;
  private static final SearchControls CLIENTGROUPS_SC = multiSC();
  /**
   * @return list of DNs of client groups for the client
   */
  public List<String> searchClientgroupDNs(String clientDN) throws NamingException {
    NamingEnumeration<SearchResult> r;
    r = ctx.search( CLIENTGROUPS_DN,
                    "(uniquemember={0})", new String[] { clientDN },
                    CLIENTGROUPS_SC);
    List<String> clientgroupDNs = new ArrayList<>();
    while (r.hasMore()) {
      clientgroupDNs.add(r.next().getName());
    }
    return clientgroupDNs;
  }

  private static final String APPGROUPS_DN = "ou=appgroups," + BASE_DN;
  private static final SearchControls APPGROUPS_SC = multiSC();
  /**
   * @return list of DNs of associated application groups for the given dns
   */
  public List<String> searchAppgroupDNs(String... memberDNs)
      throws NamingException {
    List<String> appgroupDNs = new ArrayList<>();
    if (memberDNs.length == 0) return appgroupDNs;
    NamingEnumeration<SearchResult> r;
    r = ctx.search( APPGROUPS_DN,
                    getUniquememberFilter(memberDNs.length),
                    memberDNs,
                    APPGROUPS_SC);
    while (r.hasMore()) {
      appgroupDNs.add(r.next().getName());
    }
    return appgroupDNs;
  }

  private static final String USERGROUPS_DN = "ou=usergroups," + BASE_DN;
  private static final SearchControls USERGROUPS_SC = multiSC();
  /**
   * @return list of DNs of user groups for userDN
   */
  public List<String> searchUsergroupDNs(String userDN) throws NamingException {
    NamingEnumeration<SearchResult> r;
    r = ctx.search( USERGROUPS_DN,
                    "(uniquemember={0})", new String[] { userDN },
                    USERGROUPS_SC);
    List<String> usergroupDNs = new ArrayList<>();
    while (r.hasMore()) {
      usergroupDNs.add(r.next().getName());
    }
    return usergroupDNs;
  }


  private static final SearchControls PROFILE_SC = multiSC("cn", "description");
  /**
   * Load all profiles of given type that have any given memeberDNs as
   * uniquemember and add name, description and type attributes
   */
  private Collection<Map<String, String>> loadRelatedProfiles(
        String type, String searchDN, String... memberDNs)
        throws NamingException {
    Collection<Map<String, String>> profiles = new ArrayList<>();
    if (memberDNs.length == 0) return profiles;
    NamingEnumeration<SearchResult> r;
    r = ctx.search( searchDN,
                    getUniquememberFilter(memberDNs.length),
                    memberDNs,
                    PROFILE_SC);
    while (r.hasMore()) {
      SearchResult sr = r.next();
      Map<String, String> profile = loadProfileWithType(type, sr.getName());
      if (profile == null) continue;
      Attributes attrs = sr.getAttributes();
      profile.put("name", (String) attrs.get("cn").get());
      Attribute descriptionAttr = attrs.get("description");
      if (descriptionAttr != null) {
        profile.put("description", (String) descriptionAttr.get());
      }
      profiles.add(profile);
    }
    return profiles;
  }


  private static final String DEVICE_DN = "ou=devices," + BASE_DN;
  public Collection<Map<String, String>> loadDevices(String... dns)
        throws NamingException {
    return loadRelatedProfiles("device", DEVICE_DN, dns);
  }

  private static final String APPLICATIONS_DN = "ou=apps," + BASE_DN;
  public Collection<Map<String, String>> loadApplications(String... dns)
      throws NamingException {
    return loadRelatedProfiles("application", APPLICATIONS_DN, dns);
  }


  /**
   * Close LDAP connection. This instance should be thrown away afterwards.
   */
  @Override
  public void close() {
    try {
      ctx.close();
    } catch (NamingException ex) {
    }
  }


  private static final SearchControls SUBTYPE_SC = singleSC("description");
  /**
   * Load all entries from "profile" nismap via loadProfile and add a "type" key
   * in the form type'/'subtype with given type and subtype from profile's
   * description.
   *
   * @return profile map or null if nothing was found or an error occurred
   */
  private Map<String, String> loadProfileWithType(String type, String dn)
      throws NamingException {
    Map<String, String> profile = loadProfile(dn);
    if (profile == null) return null;
    // get subtype
    NamingEnumeration<SearchResult> r;
    r = ctx.search(dn, "(nismapname=profile)", SUBTYPE_SC);
    if (!r.hasMore()) {
      LOG.error("Profile {} has no subtype", dn);
      return null;
    }
    String subtype = (String) r.next().getAttributes().get("description").get();
    profile.put("type", type + "/" + subtype);
    return profile;
  }


  private static final SearchControls NISMAP_SC = multiSC("cn", "nismapentry");
  /**
   * Load all entries from nismap and return them as a handy Map.
   *
   * @return Map constructed from all nismapentries or null if an error
   * occured
   */
  public Map<String, String> loadProfile(String dn) {
    Map<String, String> map = new HashMap<>();

    try {
      NamingEnumeration<SearchResult> r;
      r = ctx.search( "nismapname=profile," + dn,
                      "(objectClass=nisObject)",
                      NISMAP_SC);

      while (r.hasMore()) {
        Attributes attrs = r.next().getAttributes();
        map.put((String) attrs.get("cn").get(),
                (String) attrs.get("nismapentry").get());
      }
    } catch (NamingException ex) {
      LOG.error("Failed to load profile for {}", dn, ex);
      return null;
    }

    return map;
  }


  /**
   * Update the iphostnumber attribute for given dn
   */
  public void saveIP(String dn, String ip) throws NamingException {
    ctx.modifyAttributes( dn,
                          DirContext.REPLACE_ATTRIBUTE,
                          new BasicAttributes("iphostnumber", ip) );
  }


  private static final String UNRECOG_DN = "ou=unrecognized-clients,"+BASE_DN;
  private static final SearchControls UNRECOG_SC = singleSC("iphostnumber");
  private static Attribute UNRECOG_OBJECTCLASS;
  static {
    UNRECOG_OBJECTCLASS = new BasicAttribute("objectClass");
    UNRECOG_OBJECTCLASS.add("top");
    UNRECOG_OBJECTCLASS.add("device");
    UNRECOG_OBJECTCLASS.add("iphost");
    UNRECOG_OBJECTCLASS.add("ieee802Device");
  }


  /**
   * Update iphostnumber and description attributes of unrecognized-clients
   * entry or create a new entry (if none exists).
   */
  public void updateUnrecognizedClient(String mac, String ip, String desc)
      throws NamingException {
    Attributes attrs = new BasicAttributes(true);
    attrs.put("iphostnumber", ip);
    attrs.put("description", desc);

    NamingEnumeration<SearchResult> r;
    r = ctx.search( UNRECOG_DN,
                    "(macaddress={0})", new String[] { mac },
                    UNRECOG_SC);
    if (r.hasMore()) {
      ctx.modifyAttributes( r.next().getName(),
                            DirContext.REPLACE_ATTRIBUTE,
                            attrs);
    } else {
      attrs.put(UNRECOG_OBJECTCLASS);
      attrs.put("cn", mac);
      attrs.put("macaddress", mac);
      ctx.bind(String.format("cn=%s,%s", mac, UNRECOG_DN), null, attrs);
    }
  }
}
