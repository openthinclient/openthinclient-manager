package org.openthinclient.service.common;

import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

public class ServerIDFactoryTest {

  @Test
  public void testCreateMultipleServerIDs() throws Exception {

    final String sid1 = ServerIDFactory.create();
    final String sid2 = ServerIDFactory.create();
    final String sid3 = ServerIDFactory.create();
    final String sid4 = ServerIDFactory.create();

    Assert.assertThat(sid1, not(equalTo(sid2)));
    Assert.assertThat(sid1, not(equalTo(sid3)));
    Assert.assertThat(sid1, not(equalTo(sid4)));

  }


  @Test
  public void testParseAsUUID() throws Exception {
    final String sid1 = ServerIDFactory.create();
    Assert.assertNotNull(UUID.fromString(sid1));
  }
}