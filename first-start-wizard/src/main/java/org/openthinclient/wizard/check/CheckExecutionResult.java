package org.openthinclient.wizard.check;

public class CheckExecutionResult<T> {

  private final CheckResultType type;
  private final T value;


  public CheckExecutionResult(CheckResultType type) {
    this(type, null);
  }

  public CheckExecutionResult(CheckResultType type, T value) {
    this.type = type;
    this.value = value;
  }

  public T getValue() {
    return value;
  }

  public CheckResultType getType() {
    return type;
  }

  @Override
  public String toString() {
    return "CheckExecutionResult{" +
            "type=" + type +
            ", value=" + value +
            '}';
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
