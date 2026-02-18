package org.openthinclient.service.store;

import java.util.*;

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


  // The actual instance

  private LdapContext ctx;
  private Set<String> seenDNs;

  /**
   * Create a new LDAP connection. If dedupe is true, all DNs found during the
   * lifetime of this connection by any multi-result search will be remembered
   * and not returned again by any subsequent multi-result search. (A multi-
   * result search is a search that can return more than one result. E.g.
   * the searching for related DNs or profiles, but not searchClientDN(mac).)
   */
  public LDAPConnection(boolean dedupe) throws NamingException {
    ctx = (ServerLdapContext) DirectoryService.getInstance().getJndiContext(
        new LdapDN(dsc.getContextSecurityPrincipal()),
        dsc.getContextSecurityPrincipal(),
        dsc.getContextSecurityCredentials().getBytes(),
        "simple",
        ""
    );
    seenDNs = dedupe ? new HashSet<>() : null;
  }

  public LDAPConnection() throws NamingException {
    this(false);
  }

  /** uniqueMemberMatch is utterly broken in the builtin ancient ApacheDS.
   *  It neither supports escaped characters in filters nor can it handle
   *  special characters in filter args and compares DNs case-sensitively
   *  (which is very wrong).
   *  This is a straightforward, ugly workaround that loads all entries and
   *  then filters them less wrongly.
   *
   * Note: The cons parameter MUST include the "uniquemember" attribute for this
   * to work correctly.
   */
  private NamingEnumeration<SearchResult> safeUniqueMembersSearch(String name,
    String[] memberDNs, SearchControls cons)
    throws NamingException {
      NamingEnumeration<SearchResult> r;
      r = ctx.search(name, "(objectClass=*)", cons);

      return new NamingEnumeration<SearchResult>() {
        private SearchResult next = null;

        /** Get the next result that matches one of the memberDNs or null if
         * no more results are available. */
        private SearchResult getNext() throws NamingException {
          NamingEnumeration<?> uniquemembers;
          while (r.hasMore()) {
            SearchResult sr = r.next();
            uniquemembers = sr.getAttributes().get("uniquemember").getAll();
            while (uniquemembers.hasMore()) {
              String uniquemember = (String) uniquemembers.next();
              for (String memberDN : memberDNs) {
                if (uniquemember.equalsIgnoreCase(memberDN)) {
                  return sr;
                }
              }
            }
          }
          return null;
        }

        // NamingEnumeration interface

        @Override
        public boolean hasMore() throws NamingException {
          if (next == null) next = getNext();
          return next != null;
        }
        @Override
        public SearchResult next() throws NamingException {
          if (next == null) next = getNext();
          if (next == null) throw new NoSuchElementException();
          SearchResult r = next;
          next = null;
          return r;
        }
        @Override
        public void close() throws NamingException {
          r.close();
        }
        @Override
        public boolean hasMoreElements() {
          try { return r.hasMore(); }
          catch (NamingException ex) { return false; }
        }
        @Override
        public SearchResult nextElement() {
          try { return r.next(); }
          catch (NamingException ex) {
            throw new NoSuchElementException(ex.toString());
          }
        }
      };
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

  private static final SearchControls CLIENT_LOCATION_SC = singleSC("l");
  /**
   * @return client DN and location DN for given MAC address or default client
   * (if no client with given mac was found and default client is enabled)
   */
  public String[] getClientAndLocationDNs(String mac)
      throws NamingException {
    NamingEnumeration<SearchResult> r;
    r = ctx.search(CLIENT_DN,
        "(macaddress={0})", new String[] { mac },
        CLIENT_LOCATION_SC);
    if (!r.hasMore() && isDefaultClientEnabled()) {
      r = ctx.search( CLIENT_DN,
                      "(macaddress={0})",
                      new String[] { ClientService.DEFAULT_CLIENT_MAC },
                      CLIENT_LOCATION_SC);
    }
    if (!r.hasMore()) return null;
    SearchResult sr = r.next();
    return new String[] {
        sr.getName(),
        (String) sr.getAttributes().get("l").get()
    };
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
   * @implNote This method will consider the dedupe flag of the connection.
   * @return list of DNs of client groups for the client
   */
  public List<String> searchClientgroupDNs(String clientDN) throws NamingException {
    NamingEnumeration<SearchResult> r;
    r = ctx.search( CLIENTGROUPS_DN,
                    "(uniquemember={0})", new String[] { clientDN },
                    CLIENTGROUPS_SC);
    List<String> clientgroupDNs = new ArrayList<>();
    while (r.hasMore()) {
      String dn = r.next().getName();
      if (seenDNs != null && !seenDNs.add(dn)) continue;
      clientgroupDNs.add(dn);
    }
    return clientgroupDNs;
  }

  private static final String APPGROUPS_DN = "ou=appgroups," + BASE_DN;
  private static final SearchControls APPGROUPS_SC = multiSC("uniquemember");
  /**
   * @implNote This method will consider the dedupe flag of the connection.
   * @return list of DNs of associated application groups for the given dns
   */
  public List<String> searchAppgroupDNs(String... memberDNs)
      throws NamingException {
    if (memberDNs.length == 0) return Collections.emptyList();
    List<String> appgroupDNs = new ArrayList<>();
    NamingEnumeration<SearchResult> r;
    r = safeUniqueMembersSearch(APPGROUPS_DN, memberDNs, APPGROUPS_SC);
    while (r.hasMore()) {
      String dn = r.next().getName();
      if (seenDNs != null && !seenDNs.add(dn)) continue;
      appgroupDNs.add(dn);
    }
    return appgroupDNs;
  }

  private static final String USERGROUPS_DN = "ou=usergroups," + BASE_DN;
  private static final SearchControls USERGROUPS_SC = multiSC();
  /**
   * @implNote This method will consider the dedupe flag of the connection.
   * @return list of DNs of user groups for userDN
   */
  public List<String> searchUsergroupDNs(String userDN) throws NamingException {
    NamingEnumeration<SearchResult> r;
    r = ctx.search( USERGROUPS_DN,
                    "(uniquemember={0})", new String[] { userDN },
                    USERGROUPS_SC);
    List<String> usergroupDNs = new ArrayList<>();
    while (r.hasMore()) {
      String dn = r.next().getName();
      if (seenDNs != null && !seenDNs.add(dn)) continue;
      usergroupDNs.add(dn);
    }
    return usergroupDNs;
  }


  private static final SearchControls PROFILE_SC = multiSC(
      "cn", "description", "uniquemember");
  /**
   * Load all profiles of given type that have any given memberDNs as
   * uniquemember and add cn (as "name"), the description and type attributes
   *
   * @implNote This method will consider the dedupe flag of the connection.
   */
  private List<Map<String, String>> loadRelatedProfiles(
        String type, String searchDN, String relation, String... memberDNs)
        throws NamingException {
    if (memberDNs.length == 0) return Collections.emptyList();
    List<Map<String, String>> profiles = new ArrayList<>();
    NamingEnumeration<SearchResult> r;
    r = safeUniqueMembersSearch(searchDN, memberDNs, PROFILE_SC);

    while (r.hasMore()) {
      SearchResult sr = r.next();

      String dn = sr.getName();
      if (seenDNs != null && !seenDNs.add(dn)) continue;

      Map<String, String> profile = loadProfileWithType(type, dn);
      if (profile == null) continue;
      Attributes attrs = sr.getAttributes();
      profile.put("name", (String) attrs.get("cn").get());
      Attribute descriptionAttr = attrs.get("description");
      if (descriptionAttr != null) {
        profile.put("description", (String) descriptionAttr.get());
      }
      profile.put("relation", relation);
      profiles.add(profile);
    }
    return profiles;
  }


  private static final String DEVICE_DN = "ou=devices," + BASE_DN;
  /**
   * @return list of devices for the given memberDNs.
   * @see loadRelatedProfiles
   */
  public List<Map<String, String>> loadDevices(
        String relation, String... memberDNs)
        throws NamingException {
    return loadRelatedProfiles(
          "device", DEVICE_DN, relation, memberDNs);
  }

  private static final String APPLICATIONS_DN = "ou=apps," + BASE_DN;
  /**
   * @return list of applications for the given memberDNs.
   * @see loadRelatedProfiles
   */
  public List<Map<String, String>> loadApplications(
      String relation, String... memberDNs)
      throws NamingException {
    return loadRelatedProfiles(
          "application", APPLICATIONS_DN, relation, memberDNs);
  }

  private static final String PRINTERS_DN = "ou=printers," + BASE_DN;
  /**
   * @return list of applications for the given memberDNs.
   * @see loadRelatedProfiles
   */
  public List<Map<String, String>> loadPrinters(
      String relation, String... memberDNs)
      throws NamingException {
    return loadRelatedProfiles(
          "printer", PRINTERS_DN, relation, memberDNs);
  }

  private static final String ADMINIS_DN = "cn=administrators," + REALM_DN;
  private static final String[] ADMINIS_ATTRS = new String[] { "uniquemember" };
  public Collection<String> loadAdminDNs() throws NamingException{
    Collection<String> adminDNs = new ArrayList<>();
    Attributes attrs = ctx.getAttributes(ADMINIS_DN, ADMINIS_ATTRS);
    if (attrs.get("uniquemember") == null) return adminDNs;
    NamingEnumeration<?> adminDNsEnum = attrs.get("uniquemember").getAll();
    while (adminDNsEnum.hasMore()) {
      adminDNs.add((String) adminDNsEnum.next());
    }
    return adminDNs;
  }

  public void updateAdminDNs(String dn, boolean isAdmin)
  throws NamingException {
    LOG.info("Updating admin DNs: " + dn + " isAdmin: " + isAdmin);
    Collection<String> adminDNs = loadAdminDNs();
    boolean wasAdmin = adminDNs.contains(dn);
    if (wasAdmin && !isAdmin) {
      ctx.modifyAttributes(ADMINIS_DN, DirContext.REMOVE_ATTRIBUTE,
          new BasicAttributes("uniquemember", dn));
    } else if (!wasAdmin && isAdmin) {
      ctx.modifyAttributes(ADMINIS_DN, DirContext.ADD_ATTRIBUTE,
          new BasicAttributes("uniquemember", dn));
    }
  }

  private static final String USERS_DN = "ou=users," + BASE_DN;
  private static final SearchControls USERS_SC = multiSC(
      "cn", "description");
  public List<Map<String, String>> loadAllUsers() throws NamingException {
    Collection<String> adminDNs = loadAdminDNs();
    List<Map<String, String>> users = new ArrayList<>();
    NamingEnumeration<SearchResult> r;
    r = ctx.search( USERS_DN,
                    "(objectClass=person)",
                    USERS_SC);

    while (r.hasMore()) {
      SearchResult sr = r.next();
      Attributes attrs = sr.getAttributes();
      Map<String, String> user = new HashMap<>();
      String dn = sr.getName();
      user.put("dn", dn);
      user.put("role", adminDNs.contains(dn) ? "admin": "");
      user.put("name", (String) attrs.get("cn").get());
      Attribute descriptionAttr = attrs.get("description");
      if (descriptionAttr != null) {
        user.put("description", (String) descriptionAttr.get());
      }
      users.add(user);
    }
    return users;
  }

  public Map<String, String> loadUser(String name) throws NamingException {
    NamingEnumeration<SearchResult> r;
    r = ctx.search( USERS_DN,
                    "(cn={0})", new String[] { name },
                    USERS_SC);
    if (!r.hasMore()) return null;
    SearchResult sr = r.next();
    Attributes attrs = sr.getAttributes();

    Map<String, String> user = new HashMap<>();
    String dn = sr.getName();
    user.put("dn", dn);
    user.put("role", loadAdminDNs().contains(dn) ? "admin": "");
    user.put("name", (String) attrs.get("cn").get());
    Attribute descriptionAttr = attrs.get("description");
    if (descriptionAttr != null) {
      user.put("description", (String) descriptionAttr.get());
    }
    return user;
  }

  public
  String saveUser(String dn, String name, String description, String password,
                  boolean isAdmin)
  throws NamingException {
    Attributes attrs = new BasicAttributes(true);
    attrs.put("cn", name);
    attrs.put("description", description == null? "": description);
    if (password != null && !password.isEmpty()) {
      attrs.put("userPassword", password);
    }
    if (dn == null) {
      Attribute objectClassAttribute = new BasicAttribute("objectClass");
      objectClassAttribute.add("top");
      objectClassAttribute.add("person");
      objectClassAttribute.add("organizationalPerson");
      objectClassAttribute.add("inetOrgPerson");
      attrs.put(objectClassAttribute);
      dn = "cn=" + name + "," + USERS_DN;
      attrs.put("sn", name); // sn is required by person objectClass
      ctx.bind(dn, null, attrs);
    } else {
      ctx.modifyAttributes(dn, DirContext.REPLACE_ATTRIBUTE, attrs);
      String newDN = "cn=" + name + "," + USERS_DN;
      if (!dn.equals(newDN)) {
        ctx.rename(dn, newDN);
        updateReferences(dn, newDN);
        dn = newDN;
      }
    }
    updateAdminDNs(dn, isAdmin);
    return dn;
  }

  public void deleteUser(String dn) throws NamingException {
    ctx.unbind(dn);
    updateReferences(dn, null);
  }

  private
  void updateReferences(String oldDN, String newDN)
  throws NamingException {
    SearchControls sc;
    sc = new SearchControls(SearchControls.SUBTREE_SCOPE,
                              0 /* count limit */,
                              500 /* time limit in ms */,
                              new String[] { "uniquemember" },
                              false /* don't return bound object */,
                              true  /* dereference links */);
    NamingEnumeration<SearchResult> r;
    r = ctx.search( BASE_DN, "(uniquemember={0})", new String[] { oldDN }, sc);
    while (r.hasMore()) {
      SearchResult sr = r.next();
      String dn = sr.getName();
      NamingEnumeration<?> members;
      members = sr.getAttributes().get("uniquemember").getAll();
      Attribute uniquemember = new BasicAttribute("uniquemember");
      while (members.hasMore()) {
        String member = (String) members.next();
        if (member.equals(oldDN)) {
          if (newDN != null) member = newDN;
        }
        uniquemember.add(member);
      }
      Attributes attrs = new BasicAttributes(true);
      attrs.put(uniquemember);
      ctx.modifyAttributes(dn, DirContext.REPLACE_ATTRIBUTE, attrs);
    }
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


  public Map<String, String> loadUnrecognizedClients() throws NamingException {
    Map<String, String> map = new HashMap<>();
    NamingEnumeration<SearchResult> r = ctx.search(UNRECOG_DN, null);

    while (r.hasMore()) {
      SearchResult i = r.next();

      Attributes attrs = i.getAttributes();
      Attribute description_attr = attrs.get("description");

      String description;
      if (description_attr == null) {
        description = new String();
      } else {
        description = (String) description_attr.get();
      }

      map.put((String) attrs.get("cn").get(), description);
    }

    return map;
  }


  public void removeUnrecognizedClient(String mac) throws NamingException {
    ctx.unbind(String.format("cn=%s,%s", mac, UNRECOG_DN));
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
