package org.openthinclient.web.support;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SystemReportGeneratorTest {

  @Test
  public void toMacAddressString() {

    byte[] address = {
            (byte) 0x00,
            (byte) 0x80,
            (byte) 0x41,
            (byte) 0xae,
            (byte) 0xfd,
            (byte) 0x7e,
    };

    final String s = SystemReportGenerator.toMacAddressString(address);

    assertEquals("00:80:41:ae:fd:7e", s);
  }
}