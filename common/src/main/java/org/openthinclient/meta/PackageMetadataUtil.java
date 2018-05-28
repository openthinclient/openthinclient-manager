package org.openthinclient.meta;

import java.util.List;
import java.util.Locale;

public class PackageMetadataUtil {

  /**
   * Resolves the most sensible {@link Label} based on the provided {@link Locale}.
   */
  public static String resolveLabel(Locale locale, List<Label> labels) {
    Label enLabel = null;
    for (Label label : labels) {
      if ("en".equals(label.getLang())) {
        // track the english label (if any) in case we have no matching label
        enLabel = label;
      }

      if (locale != null && locale.getLanguage().equals(label.getLang())) {
        return label.getValue();
      }
    }

    // no matching label found. Try to fallback to the en, if present
    if (enLabel != null) {
      return enLabel.getValue();
    }

    // return the very first label
    if (labels.size() > 0)
      return labels.get(0).getValue();
    return null;
  }

  /**
   * Resolves the most sensible {@link Label} based on the provided {@link Locale}. This method is
   * similar to {@link #resolveLabel(Locale, List)} but will default to the
   * {@link Bookmark#getPath()} if no label could be found.
   */
  public static String resolveLabel(Locale locale, Bookmark bookmark) {
    final List<Label> labels = bookmark.getLabel();
    String label = resolveLabel(locale, labels);

    if (label != null)
      return label;

    // last resort: just print the path
    return bookmark.getPath();
  }
}
