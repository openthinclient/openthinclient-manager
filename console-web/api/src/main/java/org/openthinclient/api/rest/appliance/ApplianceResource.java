package org.openthinclient.api.rest.appliance;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api-internal/v1/appliance")
public class ApplianceResource {

  private final TokenManager tokenManager;

  public ApplianceResource(TokenManager tokenManager) {

    this.tokenManager = tokenManager;
  }

  @PostMapping("/token/validate")
  public ValidationResult validate(@RequestBody String token) {
    // FIXME client IP validation
    final ValidationResult validationResult = new ValidationResult();
    validationResult.setValid(tokenManager.validateToken(token, null));
    return validationResult;
  }

}
