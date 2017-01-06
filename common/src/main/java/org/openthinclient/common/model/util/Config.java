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

    ConfigProperty<String> Debug = new StringConfig("BootOptions.Debug");
    ConfigProperty<String> ExtraOptions = new StringConfig("BootOptions.ExtraOptions");
    ConfigProperty<String> FirmwareImage = new StringConfig("BootOptions.FirmwareImage");
    ConfigProperty<String> GpuModule = new StringConfig("BootOptions.GpuModule");
    ConfigProperty<String> InitrdName = new StringConfig("BootOptions.InitrdName");
    ConfigProperty<String> KernelName = new StringConfig("BootOptions.KernelName");
    ConfigProperty<String> NFSRootPath = new StringConfig("BootOptions.NFSRootPath");
    ConfigProperty<String> NFSRootserver = new StringConfig("BootOptions.NFSRootserver");
    ConfigProperty<String> BootLoaderTemplate = new StringConfig("BootOptions.BootLoaderTemplate");
    ConfigProperty<PXEServicePolicyType> PXEServicePolicy = new ConfigProperty.EnumConfig<>("BootOptions.PXEServicePolicy", PXEServicePolicyType.class, PXEServicePolicyType.RegisteredOnly);

    enum PXEServicePolicyType {
      AnyClient,
      RegisteredOnly
    }
  }


}
