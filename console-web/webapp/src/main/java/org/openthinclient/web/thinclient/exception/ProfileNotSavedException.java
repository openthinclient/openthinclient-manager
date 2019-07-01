package org.openthinclient.web.thinclient.exception;


public class ProfileNotSavedException extends Exception {

  public ProfileNotSavedException(String message, Exception e) {
    super(message, e);
  }
}
