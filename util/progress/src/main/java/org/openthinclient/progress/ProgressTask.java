package org.openthinclient.progress;

import java.util.Locale;

public interface ProgressTask<V> {
  ProgressTaskDescription getDescription(Locale locale);

  V execute(ProgressReceiver progressReceiver) throws Exception;

  class ProgressTaskDescription {

    private final String name;
    private final String description;

    public ProgressTaskDescription(String name, String description) {
      this.name = name;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  }
}
