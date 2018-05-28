package org.openthinclient.web.filebrowser;

import com.vaadin.icons.VaadinIcons;

import org.junit.Test;
import org.openthinclient.meta.Bookmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FileBrowserViewTest {

  @Test
  public void testResolveIcon() {

    assertEquals(VaadinIcons.COG, FileBrowserView.resolveIcon(createBookmark("vaadin:cog")));
    assertEquals(VaadinIcons.TRASH, FileBrowserView.resolveIcon(createBookmark("vaadin:trash")));
    assertNull(FileBrowserView.resolveIcon(createBookmark("vaadin:non_existing")));
    assertEquals(VaadinIcons.TREE_TABLE, FileBrowserView.resolveIcon(createBookmark("vaadin:tree-table")));

  }

  private Bookmark createBookmark(String icon) {
    final Bookmark b = new Bookmark();
    b.setIcon(icon);
    return b;
  }
}