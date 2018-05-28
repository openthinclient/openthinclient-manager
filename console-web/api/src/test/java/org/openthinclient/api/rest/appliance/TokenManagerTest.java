package org.openthinclient.api.rest.appliance;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import org.junit.Ignore;
import org.junit.Test;
import org.openthinclient.service.common.home.impl.ApplianceConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TokenManagerTest {

  @Test
  public void testCreateToken() {

    final TokenManager tm = createTokenManager();
    final String token = tm.createToken("127.0.0.1");

    assertNotNull(token);

    // try to load the jwt to ensure it is valid
    final DecodedJWT jwt = JWT.decode(token);

    final Claim clientIp = jwt.getClaim("clientIp");

    assertNotNull(clientIp);
    assertEquals("127.0.0.1", clientIp.asString());
  }

  private TokenManager createTokenManager() {
    return new TokenManager(new ApplianceConfiguration());
  }

  @Test
  public void testCreateAndValidateToken() {

    final TokenManager tm = createTokenManager();
    final String token = tm.createToken("127.0.0.1");

    assertTrue(tm.validateToken(token, "127.0.0.1"));

  }

  @Test
  // FIXME right now client ip address validation has been disabled.
  // the client ip validation should be enabled to provide further security.
  @Ignore
  public void testValidateWithIncorrectClientIP() {

    final TokenManager tm = createTokenManager();
    final String token = tm.createToken("127.0.0.1");

    assertFalse(tm.validateToken(token, "10.20.1.2"));

  }
}