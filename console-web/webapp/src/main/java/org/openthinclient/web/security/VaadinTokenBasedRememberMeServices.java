package org.openthinclient.web.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

/**
 * The TokenBasedRememberMeServices use httpRequest.getParameter to check where remember-me is requested or not.
 * We can only add request-attributes to httpRequest, so we need to overwrite method rememberMeRequested().
 *
 * @author jne
 *
 */
public class VaadinTokenBasedRememberMeServices extends TokenBasedRememberMeServices {

  public VaadinTokenBasedRememberMeServices(String key, UserDetailsService userDetailsService) {
    super(key, userDetailsService);
  }

  @Override
  protected boolean rememberMeRequested(HttpServletRequest request, String parameter) {

    if (request.getAttribute(DEFAULT_PARAMETER) != null
        && (boolean) request.getAttribute(DEFAULT_PARAMETER)) {

      request.removeAttribute(DEFAULT_PARAMETER);
      return true;
    }
    return super.rememberMeRequested(request, parameter);
  }
}
