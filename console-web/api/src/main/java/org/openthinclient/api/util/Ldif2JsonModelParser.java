package org.openthinclient.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openthinclient.api.importer.model.ProfileType;
import org.openthinclient.api.rest.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ldif.MappingLdifReader;
import org.springframework.batch.item.ldif.RecordMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.util.Pair;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.ldap.LdapName;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ldif2JsonModelParser parses an LDIF-file an creates Profile-objects
 */
public class Ldif2JsonModelParser {

    Logger logger = LoggerFactory.getLogger(Ldif2JsonModelParser.class);

    /** the input file */
    private File file;
    /** A tuple of String values to be used for replace an placeholder in LDIF-file */
    private Pair<String, String> replacement;
    /** the environment DN of this entries */
    private String envDN;

    public Ldif2JsonModelParser(File ldifFile, String envDN, Pair<String, String> replacement) {
        this.file = ldifFile;
        this.envDN = envDN;
        this.replacement = replacement;
    }

    public List<AbstractProfileObject> parse() throws Exception {
        MappingLdifReader<AbstractProfileObject> reader = new MappingLdifReader();
        reader.setRecordMapper(new RecordMapper<AbstractProfileObject>() {
            @Override
            public AbstractProfileObject mapRecord(org.springframework.ldap.core.LdapAttributes attributes) {
                if (attributes == null) return null;
                LdapName name = attributes.getName();
                Attribute objectClass = attributes.get("objectClass");
                Attribute description = attributes.get("description");
                Attribute uniquemember = attributes.get("uniquemember");
                Attribute cn = attributes.get("cn");
                Attribute nismapname = attributes.get("nismapname");
                Attribute nismapentry = attributes.get("nismapentry");

                ProfileType profileType;
                String ou = getString(name, "ou");
                if (ou.equals("hwtypes")) {
                    profileType = ProfileType.HARDWARETYPE;
                } else if (ou.equals("apps")) {
                    profileType = ProfileType.APPLICATION;
                } else if (ou.equals("printers")) {
                    profileType = ProfileType.PRINTER;
                } else if (ou.equals("clients")) {
                    profileType = ProfileType.CLIENT;
                } else if (ou.equals("locations")) {
                    profileType = ProfileType.LOCATION;
                } else if (ou.equals("devices")) {
                    profileType = ProfileType.DEVICE;
                } else {
                    logger.debug("Skipping entry because there is no JsonModel-object for type '" + ou + "'.");
                    return new AbstractProfileObject(null){};
                }

                AbstractProfileObject profileObject;
                switch (profileType) {
                    case CLIENT:
                        profileObject = new Client();
                        profileObject.setName(getString(name, "cn"));
                        break;
                    case HARDWARETYPE:
                        profileObject = new HardwareType();
                        profileObject.setName(getString(name, "cn"));
                        break;
                    case APPLICATION:
                        profileObject = new Application();
                        profileObject.setName(getString(name, "cn"));
                        break;
                    case DEVICE:
                        profileObject = new Device();
                        profileObject.setName(getString(name, "cn"));
                        break;
                    case PRINTER:
                        profileObject = new Printer();
                        profileObject.setName(getString(name, "cn"));
                        break;
                    case LOCATION:
                        profileObject = new Location();
                        profileObject.setName(getString(name, "l"));
                        break;
                    default:
                        return new AbstractProfileObject(null){};
                }


                if (description != null) {
                    try {
                        profileObject.setDescription(description.get().toString());
                    } catch (NamingException e) {
                        logger.warn("Cannot get description", e);
                    }
                }

                // Add configuration
                try {
                    String configKey = null;
                    if (cn != null && cn.get() != null) {
                        configKey = cn.get().toString();
                    }
                    if (configKey != null && nismapentry != null && nismapentry.get() != null) {
                        profileObject.getConfiguration().setAdditionalProperty(configKey, nismapentry.get().toString());
                    }
                } catch (NamingException e) {
                    logger.debug("Cannot handle cn or nismapentry", e);
                }

                // add members

                    try {
                        if (uniquemember != null && uniquemember.get() != null) {
                            if (profileType == ProfileType.DEVICE) {
                                ((Device) profileObject).addMember(uniquemember.get().toString());
                            }
                            if (profileType == ProfileType.APPLICATION) {
                                ((Application) profileObject).addMember(uniquemember.get().toString());
                            }
                        }
                    } catch (NamingException e) {
                        logger.debug("Cannot handle uniquemember", e);
                    }


                // Do we need to handle objectClass?
                //                if (objectClass != null) {
                //                    NamingEnumeration<?> namingEnumeration = objectClass.getAll();
                //                    while (namingEnumeration.hasMoreElements()) {
                //                        profileObject.getConfiguration().setAdditionalProperty("objectClass", namingEnumeration.next().toString());
                //                    };
                //                }
                return profileObject;

            }


        });

        // Prepare file and reader
        InputStream inputStream = new FileInputStream(file);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (replacement == null) {
                out.append(line).append("\n");
            } else {
                out.append(line.replaceAll(replacement.getFirst(), replacement.getSecond())).append("\n");
            }
        }
        reader.setResource(new ByteArrayResource(out.toString().getBytes()));
        reader.open(new ExecutionContext());

        // read values
        List<AbstractProfileObject> parseResults = new ArrayList<>();
        AbstractProfileObject profileObject = reader.read();
        while (profileObject != null) {
            if (profileObject.getType() != null) {
                parseResults.add(profileObject);
                if (logger.isDebugEnabled()) {
                    ObjectMapper om = new ObjectMapper();
                    logger.debug(om.writeValueAsString(profileObject));
                }
            }
            profileObject = reader.read();
        }

        // consolidate result: merge object with same name
        Map<String, AbstractProfileObject> result = new HashMap<>();
        parseResults.forEach(apo -> {
            if (result.containsKey(apo.getName())) {
                AbstractProfileObject object = result.get(apo.getName());
                object.getConfiguration().setAdditionalProperties(apo.getConfiguration().getAdditionalProperties());
                if (object instanceof Device && apo instanceof  Device) {
                    ((Device) object).addMembers(((Device) apo).getMembers());
                }
                if (object instanceof Application && apo instanceof  Application) {
                    ((Application) object).addMembers(((Application) apo).getMembers());
                }
            } else {
                result.put(apo.getName(), apo);
            }
        });

        return result.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
    }

    /**
     * Returns the value for given 'name'-part, only one will be returned: last occurrence of 'part'
     * @param name the LdapName
     * @param part i.e. 'ou' or 'cn'
     * @return the value for given 'name'-part
     */
    private String getString(LdapName name, String part) {
        if (name == null || name.toString().indexOf(part) == -1) {
            return "";
        }
        int startIdx = name.toString().lastIndexOf(part + "=" , name.toString().indexOf(envDN) -1) + part.length() + 1;
        return name.toString().substring(startIdx, name.toString().indexOf(",", startIdx));
    }
}
