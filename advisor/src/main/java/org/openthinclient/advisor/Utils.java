package org.openthinclient.advisor;

import java.util.Enumeration;
import java.util.Iterator;

public class Utils {

  public static <T> Iterator<T> toIterator(final Enumeration<T> enumeration) {
    return new Iterator<T>() {
      @Override
      public boolean hasNext() {
        return enumeration.hasMoreElements();
      }

      @Override
      public T next() {
        return enumeration.nextElement();
      }
    };
  }
}
