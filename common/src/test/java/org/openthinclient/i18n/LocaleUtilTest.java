package org.openthinclient.i18n;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Locale;

import org.junit.Test;

import ch.qos.cal10n.verifier.Cal10nError;
import ch.qos.cal10n.verifier.IMessageKeyVerifier;
import ch.qos.cal10n.verifier.MessageKeyVerifier;

public class LocaleUtilTest {

  @Test
  public void testNullSave() {
    Locale localeForMessages = LocaleUtil.getLocaleForMessages(null, null);
    assertNotNull(localeForMessages);
    assertEquals(Locale.ENGLISH, localeForMessages);
  }

  @Test
  public void testWithoutLocale() {
    Locale localeForMessages = LocaleUtil.getLocaleForMessages(Messages.class, null);
    assertNotNull(localeForMessages);
    assertEquals(Locale.ENGLISH, localeForMessages);
  }
  
  @Test
  public void testWithMessages() {
    Locale localeForMessages = LocaleUtil.getLocaleForMessages(Messages.class, Locale.GERMAN);
    assertNotNull(localeForMessages);
    assertEquals(Locale.GERMAN, localeForMessages);
    
    localeForMessages = LocaleUtil.getLocaleForMessages(Messages.class, Locale.ENGLISH);
    assertNotNull(localeForMessages);
    assertEquals(Locale.ENGLISH, localeForMessages);
  }  
  
  @Test
  public void testWithDefaults() {
    Locale localeForMessages = LocaleUtil.getLocaleForMessages(Messages.class, Locale.FRANCE);
    assertNotNull(localeForMessages);
    assertEquals(Locale.ENGLISH, localeForMessages);
  }  

  @Test
  public void en() {
    IMessageKeyVerifier mkv = new MessageKeyVerifier(Messages.class);
    List<Cal10nError> errorList = mkv.verify(Locale.ENGLISH);
    for(Cal10nError error: errorList) {
      System.out.println(error);
    }
    assertEquals(0, errorList.size());
  }

  @Test
  public void de() {
    IMessageKeyVerifier mkv = new MessageKeyVerifier(Messages.class);
    List<Cal10nError> errorList = mkv.verify(Locale.GERMAN);
    for(Cal10nError error: errorList) {
      System.out.println(error);
    }
    assertEquals(0, errorList.size());
  }
  
  
  
}
