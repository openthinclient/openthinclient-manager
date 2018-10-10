package org.openthinclient.manager.util.http.config;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class EncryptedStringXmlAdapter extends XmlAdapter<String,String> {
 
    /**
     * Encrypts the value to be placed back in XML
     */
    @Override
    public String marshal(String plaintext) {
      return "%%ENC:" + PasswordUtil.encryptDES(plaintext);
    }

    /**
     * Decrypts the string value
     */
    @Override
    public String unmarshal(String cyphertext) {
      if (cyphertext.startsWith("%%ENC:")) {
        return PasswordUtil.decryptDES(cyphertext.substring(6));
      } else {
        return cyphertext;
      }
    }
 
}