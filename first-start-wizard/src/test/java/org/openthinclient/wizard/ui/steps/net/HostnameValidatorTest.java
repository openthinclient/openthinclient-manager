package org.openthinclient.wizard.ui.steps.net;

import com.vaadin.data.ValueContext;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HostnameValidatorTest {

  @Test
  public void testSimpleHostname() throws Exception {

    final HostnameValidator validator = new HostnameValidator("");
    assertFalse(validator.apply("www.google.de", new ValueContext()).isError());
    assertFalse(validator.apply("192.168.20.12", new ValueContext()).isError());

  }

  @Test
  public void testInvalidHostName() throws Exception {

    final HostnameValidator validator = new HostnameValidator("");
    assertTrue(validator.apply("___www.google.de", new ValueContext()).isError());
  }
}