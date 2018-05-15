package org.openthinclient.web.thinclient.model;

import org.openthinclient.web.thinclient.model.Item.Type;

/**
 *
 */
public class RepoDummy {


  public static Item findSingleDevice() {

    Item display = new Item("Display: Dualview 1280x1024 (model 1780)", Type.DEVICE);
    display.setDescription("for openthinclient TC 1780 model");
    display.addConfig(new ItemConfiguration("secondscreen.connect", "DisplayPort-0"));
    display.addConfig(new ItemConfiguration("secondscreen.rotation", "normal"));
    display.addConfig(new ItemConfiguration("firstscreen.resolution", "1280x1024"));
    display.addConfig(new ItemConfiguration("secondscreen.positioning", "--right-of"));
    display.addConfig(new ItemConfiguration("firstscreen.connect", "DVI-0"));
    display.addConfig(new ItemConfiguration("secondscreen.resolution", "1280x1024"));
    display.addConfig(new ItemConfiguration("firstscreen.rotation", "normal"));

    return display;
  }

  public static void save(Item config, ItemConfiguration p) {

  }
}
