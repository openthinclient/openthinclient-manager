package org.openthinclient.api;

import javax.annotation.security.RolesAllowed;

import org.openthinclient.service.common.license.License;
import org.openthinclient.service.common.license.LicenseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@RestController
@RequestMapping(value = "/api/v2/license", method = RequestMethod.GET, produces = "application/json")
public class LicenseEndpoint {

  private static final Logger LOG = LoggerFactory.getLogger(LicenseEndpoint.class);

  @Autowired
  private LicenseManager licenseManager;

  @GetMapping
  @RolesAllowed("ADMINISTRATORS")
  public ResponseEntity<String> getLicenseInfo() {
    ObjectMapper mapper = JsonMapper.builder()
      .addModule(new JavaTimeModule())
      .defaultDateFormat(new StdDateFormat().withColonInTimeZone(true))
      .build();
    try {
      License license = licenseManager.getLicense();
      return ResponseEntity.ok(mapper.writeValueAsString(license));
    } catch (JsonProcessingException ex) {
      LOG.error("Could not serialize license information", ex);
      return ResponseEntity.internalServerError().build();
    }
  }

}
