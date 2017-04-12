package org.openthinclient.wizard.install;

import org.junit.Test;
import org.openthinclient.api.distributions.ImportItem;

import java.lang.reflect.Constructor;

import static org.junit.Assert.assertSame;

public class ImportItemTest {

  @Test
  public void testTargetTypesMatch() throws Exception {

    Class<?>[] importableTypes = { //
            ImportItem.Application.class, //
            ImportItem.Client.class, //
            ImportItem.Device.class, //
            ImportItem.HardwareType.class, //
            ImportItem.Location.class, //
            ImportItem.Printer.class //
    };

    for (Class<?> importableType : importableTypes) {

      final Constructor<?> defaultConstructur = importableType.getConstructor();
      final Constructor<?> pathConstructor = importableType.getConstructor(String.class);

      final ImportItem defaultInstance = (ImportItem) defaultConstructur.newInstance();

      final ImportItem pathInstance = (ImportItem) pathConstructor.newInstance("some/path/to.json");

      assertSame(defaultInstance.getTargetType(), pathInstance.getTargetType());

    }


  }
}