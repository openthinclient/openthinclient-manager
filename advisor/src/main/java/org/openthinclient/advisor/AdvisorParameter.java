package org.openthinclient.advisor;

/**
 * AdvisorParameter to access runtime parameters
 * @author JN
 */
public interface AdvisorParameter {

  /**
   * Returns a string value for given key
   * @param key the key for a value, may be null
   * @return string value or null if key=null or key cannot be found
   */
  String getStringParam(String key);
  
}
