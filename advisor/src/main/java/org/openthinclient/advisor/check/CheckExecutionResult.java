package org.openthinclient.advisor.check;

public class CheckExecutionResult<T> {

  private final CheckResultType type;


  public CheckExecutionResult(CheckResultType type) {

    this.type = type;
  }

  public CheckResultType getType() {
    return type;
  }

  public enum CheckResultType {
    /**
     * The check executed all normal.
     */
    SUCCESS,
    /**
     * The check ran into one or more warnings.
     */
    WARNING,
    /**
     * The check execution failed.
     */
    FAILED
  }
}
