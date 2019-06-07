package org.openthinclient.service.common.license;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import java.util.Base64;
import java.util.Calendar;
import java.util.Map;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import net.razorvine.pickle.Unpickler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LicenseDecrypter {
  private static final Logger LOG = LoggerFactory.getLogger(LicenseDecrypter.class);

  private static final String publicKeyBase64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAr9inZYrg5lcNqXgZ0d7yM0OYVR5HoopeidQ00qFRU8JmesWwKtpM12o+SJlSro+ZKwjYq5YQvNNSNgjAJxvGlcge4lhAbnKrDVMUyNUWNf3jDqO+yiEHn+7gIo1zeI1iHuXC4+Zt7sV7o53A2hxjY1/+eN/cfYLVknilQJ9dNHvyJkoLg9VK/nKNe4IGOuf1e4Ta8uYMgKWIA5ZZw/7ZpBYASdeeQszy2iPs7YRccCb2Tblm47W5jytFbaFbzeSSK71K2bIPMFKRVSg7KAcBVHzjgJ5E2M1ckenepFNasrh0WA/FcpzZEnRxqByDpQdlnzvYTujCqpUo+W6fIQmpDQIDAQABMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAr9inZYrg5lcNqXgZ0d7yM0OYVR5HoopeidQ00qFRU8JmesWwKtpM12o+SJlSro+ZKwjYq5YQvNNSNgjAJxvGlcge4lhAbnKrDVMUyNUWNf3jDqO+yiEHn+7gIo1zeI1iHuXC4+Zt7sV7o53A2hxjY1/+eN/cfYLVknilQJ9dNHvyJkoLg9VK/nKNe4IGOuf1e4Ta8uYMgKWIA5ZZw/7ZpBYASdeeQszy2iPs7YRccCb2Tblm47W5jytFbaFbzeSSK71K2bIPMFKRVSg7KAcBVHzjgJ5E2M1ckenepFNasrh0WA/FcpzZEnRxqByDpQdlnzvYTujCqpUo+W6fIQmpDQIDAQAB";

  private Cipher rsaCipher;
  private Cipher aesCipher;
  private PublicKey publicKey;

  LicenseDecrypter() throws Exception {
    try {
      byte[] keyData = Base64.getDecoder().decode(publicKeyBase64.getBytes());
      X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(keyData);
      rsaCipher = Cipher.getInstance("RSA");
      aesCipher = Cipher.getInstance("AES");
      publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpecX509);
    } catch(Exception ex) {
      LOG.error("Could not initialze license decrypter:", ex);
      throw ex;
    }
  }

  public LicenseData decrypt(EncryptedLicense encryptedLicense) throws Exception {
    byte[] pickledLicense = decryptLicense(encryptedLicense);
    return unpickleLicense(pickledLicense);
  }

  private byte[] decryptLicense(EncryptedLicense encryptedLicense) throws Exception {
    byte[] license = Base64.getDecoder().decode(encryptedLicense.license);
    byte[] encryption_key = Base64.getDecoder().decode(encryptedLicense.encryption_key);

    rsaCipher.init(Cipher.DECRYPT_MODE, publicKey);
    byte[] aesKey = Base64.getDecoder().decode(rsaCipher.doFinal(encryption_key));
    SecretKey key = new SecretKeySpec(aesKey, 0, aesKey.length, "AES");
    aesCipher.init(Cipher.DECRYPT_MODE, key);
    return aesCipher.doFinal(license);
  }

  private LicenseData unpickleLicense(byte[] pickledLicense) throws Exception {
    Unpickler unpickler = new Unpickler();
    Map<String, Object> licenseMap = (Map<String, Object>) unpickler.loads(pickledLicense);
    unpickler.close();
    LicenseData license = new LicenseData();

    license.server = (String)licenseMap.get("server");
    license.name = (String)licenseMap.get("name");
    license.email = (String)licenseMap.get("email");
    license.details = (String)licenseMap.get("details");
    license.count = (Integer)licenseMap.get("count");
    license.softExpiredDate = datetime(licenseMap.get("softExpiredDate"));
    license.expiredDate = datetime(licenseMap.get("expiredDate"));
    license.createdDate = datetime(licenseMap.get("createdDate"));

    return license;
  }

  private static LocalDate datetime(Object in) {
    return LocalDateTime.ofInstant(((Calendar)in).toInstant(), ZoneId.systemDefault()).toLocalDate();
  }
}
