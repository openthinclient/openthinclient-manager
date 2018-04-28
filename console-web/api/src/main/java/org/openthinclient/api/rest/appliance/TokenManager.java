package org.openthinclient.api.rest.appliance;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import org.openthinclient.service.common.home.impl.ApplianceConfiguration;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class TokenManager {

  private final Algorithm algorithm;
  private final Duration expiry;

  public TokenManager(ApplianceConfiguration applianceConfiguration) {

    // generate a secret that will be valid during runtime of this instance
    final byte[] secret = new byte[20];
    new SecureRandom().nextBytes(secret);

    algorithm = Algorithm.HMAC512(secret);

    expiry = Duration.ofSeconds(applianceConfiguration.getNoVNCTicketExpirySeconds());

  }

  /**
   * Create a new token. The generated token will be associated with the given {@code clientIp} and
   * expire after a predefined expiry amount. The expiry in seconds can be specified using
   * {@link ApplianceConfiguration#getNoVNCTicketExpirySeconds() appliance properties}.
   */
  public String createToken(String clientIp) {
    return JWT.create() //
            .withClaim("clientIp", clientIp) //
            .withExpiresAt(Date.from(Instant.now().plus(expiry))) //
            .sign(algorithm) //
            ;

  }

  public boolean validateToken(String token, String clientIp) {

    try {
      validateAndDecode(token);
      return true;
    } catch (JWTVerificationException e) {
      return false;
    }

  }

  public DecodedJWT validateAndDecode(String token) throws JWTVerificationException {
    final JWTVerifier verifier = JWT.require(algorithm) //
            .acceptLeeway(1) //
            .build();

    return verifier.verify(token);
  }
}
