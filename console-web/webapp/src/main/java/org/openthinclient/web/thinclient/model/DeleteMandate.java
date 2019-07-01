package org.openthinclient.web.thinclient.model;

public class DeleteMandate {

  private final boolean delete;
  private final String message;

  public DeleteMandate(boolean delete, String message) {
    this.delete = delete;
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public boolean checkDelete() {
    return delete;
  }
}
