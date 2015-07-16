package org.openthinclient.wizard.ui.steps.net;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HostnameValidatorTest {

  @Test
  public void testSimpleHostname() throws Exception {

    final HostnameValidator validator = new HostnameValidator("");
    assertTrue(validator.isValid("www.google.de"));
    assertTrue(validator.isValid("192.168.20.12"));

  }

  @Test
  public void testInvalidHostName() throws Exception {

    final HostnameValidator validator = new HostnameValidator("");
    assertFalse(validator.isValid("___www.google.de"));

  }
}