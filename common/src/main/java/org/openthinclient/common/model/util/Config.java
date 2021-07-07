package org.openthinclient.common.model.util;

import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.util.ConfigProperty.StringConfig;

/**
 * Utility interface and nested enums to access commonly used properties from {@link Profile model
 * objects}
 */
public interface Config {

  /**
   * Enumerates all {@link BootOptions} that the manager server has to be aware of.
   */
  interface BootOptions {

    ConfigProperty<String> NFSRootPath = new StringConfig("BootOptions.NFSRootPath");
    ConfigProperty<String> NFSRootserver = new StringConfig("BootOptions.NFSRootserver");
    ConfigProperty<String> TFTPBootserver = new StringConfig("BootOptions.TFTPBootserver");
    ConfigProperty<String> BootLoaderTemplate = new StringConfig("BootOptions.BootLoaderTemplate");
    ConfigProperty<String> BootfileName = new StringConfig("BootOptions.BootfileName");
    ConfigProperty<String> BootMode = new StringConfig("BootOptions.BootMode");
    ConfigProperty<PXEServicePolicyType> PXEServicePolicy = new ConfigProperty.EnumConfig<>("BootOptions.PXEServicePolicy", PXEServicePolicyType.class, PXEServicePolicyType.RegisteredOnly);

    enum PXEServicePolicyType {
      AnyClient,
      RegisteredOnly
    }
  }


}
