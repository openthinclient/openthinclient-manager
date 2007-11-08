/*******************************************************************************
 * openthinclient.org ThinClient suite
 * 
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *******************************************************************************/

package org.openthinclient.console.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.jgoodies.forms.factories.ComponentFactory;
import com.jgoodies.forms.layout.Sizes;

/**
 * A singleton implementaton of the {@link ComponentFactory} interface that
 * creates UI components as required by the
 * {@link com.jgoodies.forms.builder.PanelBuilder}.
 * <p>
 * 
 * The texts used in methods <code>#createLabel(String)</code> and
 * <code>#createTitle(String)</code> can contain an optional mnemonic marker.
 * The mnemonic and mnemonic index are indicated by a single ampersand (<tt>&amp;</tt>).
 * For example <tt>&quot;&amp;Save&quot</tt>, or
 * <tt>&quot;Save&nbsp;&amp;as&quot</tt>. To use the ampersand itself
 * duplicate it, for example <tt>&quot;Look&amp;&amp;Feel&quot</tt>.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.24 $
 */

public final class DetailViewComponentFactory implements ComponentFactory {

  /**
   * Holds the single instance of this class.
   */
  private static final DetailViewComponentFactory INSTANCE = new DetailViewComponentFactory();

  /**
   * The character used to indicate the mnemonic position for labels.
   */
  private static final char MNEMONIC_MARKER = '&';

  // Instance *************************************************************

  private DetailViewComponentFactory() {
    // Suppresses default constructor, ensuring non-instantiability.
  }

  /**
   * Returns the sole instance of this factory class.
   * 
   * @return the sole instance of this factory class
   */
  public static DetailViewComponentFactory getInstance() {
    return INSTANCE;
  }

  // Component Creation ***************************************************

  /**
   * Creates and returns a label with an optional mnemonic.
   * <p>
   * 
   * <pre>
   * createLabel(&quot;Name&quot;); // No mnemonic
   * createLabel(&quot;N&amp;ame&quot;); // Mnemonic is 'a'
   * createLabel(&quot;Save &amp;as&quot;); // Mnemonic is the second 'a'
   * createLabel(&quot;Look&amp;&amp;Feel&quot;); // No mnemonic, text is Look&amp;Feel
   * </pre>
   * 
   * @param textWithMnemonic the label's text - may contain an ampersand (<tt>&amp;</tt>)
   *          to mark a mnemonic
   * @return an label with optional mnemonic
   */
  public JLabel createLabel(String textWithMnemonic) {
    JLabel label = new JLabel();
    setTextAndMnemonic(label, textWithMnemonic);
    return label;
  }

  private JLabel createSeparatorLabel(String textWithMnemonic) {
    JLabel label = new StandardLabel();
    setTextAndMnemonic(label, textWithMnemonic);
    label.setVerticalAlignment(SwingConstants.CENTER);

    return label;
  }

  /**
   * Creates and returns a title label that uses the foreground color and font
   * of a <code>TitledBorder</code>.
   * <p>
   * 
   * <pre>
   * createTitle(&quot;Name&quot;); // No mnemonic
   * createTitle(&quot;N&amp;ame&quot;); // Mnemonic is 'a'
   * createTitle(&quot;Save &amp;as&quot;); // Mnemonic is the second 'a'
   * createTitle(&quot;Look&amp;&amp;Feel&quot;); // No mnemonic, text is Look&amp;Feel
   * </pre>
   * 
   * @param textWithMnemonic the label's text - may contain an ampersand (<tt>&amp;</tt>)
   *          to mark a mnemonic
   * @return an emphasized title label
   */
  public JLabel createTitle(String textWithMnemonic) {
    JLabel label = new StandardLabel();
    setTextAndMnemonic(label, textWithMnemonic);
    label.setVerticalAlignment(SwingConstants.CENTER);

    return label;
  }

  /**
   * Creates and returns a labeled separator with the label in the left-hand
   * side. Useful to separate paragraphs in a panel; often a better choice than
   * a <code>TitledBorder</code>.
   * <p>
   * 
   * <pre>
   * createSeparator(&quot;Name&quot;); // No mnemonic
   * createSeparator(&quot;N&amp;ame&quot;); // Mnemonic is 'a'
   * createSeparator(&quot;Save &amp;as&quot;); // Mnemonic is the second 'a'
   * createSeparator(&quot;Look&amp;&amp;Feel&quot;); // No mnemonic, text is Look&amp;Feel
   * </pre>
   * 
   * @param textWithMnemonic the label's text - may contain an ampersand (<tt>&amp;</tt>)
   *          to mark a mnemonic
   * @return a title label with separator on the side
   */
  public JComponent createSeparator(String textWithMnemonic) {
    return createSeparator(textWithMnemonic, SwingConstants.LEFT);
  }

  /**
   * Creates and returns a labeled separator. Useful to separate paragraphs in a
   * panel, which is often a better choice than a <code>TitledBorder</code>.
   * <p>
   * 
   * <pre>
   * final int LEFT = SwingConstants.LEFT;
   * createSeparator(&quot;Name&quot;, LEFT); // No mnemonic
   * createSeparator(&quot;N&amp;ame&quot;, LEFT); // Mnemonic is 'a'
   * createSeparator(&quot;Save &amp;as&quot;, LEFT); // Mnemonic is the second 'a'
   * createSeparator(&quot;Look&amp;&amp;Feel&quot;, LEFT); // No mnemonic, text is Look&amp;Feel
   * </pre>
   * 
   * @param textWithMnemonic the label's text - may contain an ampersand (<tt>&amp;</tt>)
   *          to mark a mnemonic
   * @param alignment text alignment, one of <code>SwingConstants.LEFT</code>,
   *          <code>SwingConstants.CENTER</code>,
   *          <code>SwingConstants.RIGHT</code>
   * @return a separator with title label
   */
  public JComponent createSeparator(String textWithMnemonic, int alignment) {
    if (textWithMnemonic == null || textWithMnemonic.length() == 0) {
      return new JSeparator();
    }
    JLabel title = createSeparatorLabel(textWithMnemonic);
    title.setHorizontalAlignment(alignment);
    return createSeparator(title);
  }

  /**
   * Creates and returns a labeled separator. Useful to separate paragraphs in a
   * panel, which is often a better choice than a <code>TitledBorder</code>.
   * <p>
   * 
   * The label's position is determined by the label's horizontal alignment.
   * <p>
   * 
   * TODO: Since this method has been marked public in version 1.0.6, we nned to
   * precisely describe the semantic of this method.
   * <p>
   * 
   * TODO: Check if we need to switch separator and label if the label's
   * horizontal alignment is "right" instead of "left".
   * 
   * @param label the title label component
   * @return a separator with title label
   * @throws NullPointerException if the label is <code>null</code>
   */
  public JComponent createSeparator(JLabel label) {
    if (label == null)
      throw new NullPointerException("The label must not be null."); //$NON-NLS-1$

    JPanel panel = new JPanel(new TitledSeparatorLayout(!isLafAqua()));
    panel.setOpaque(false);
    panel.add(label);
    panel.add(new JSeparator());
    if (label.getHorizontalAlignment() == SwingConstants.CENTER) {
      panel.add(new JSeparator());
    }
    return panel;

    // if (label == null)
    // throw new NullPointerException("The label must not be null.");
    //
    // SectionTitleBar stb = new SectionTitleBar();
    // stb.add(label);
    // return stb;
  }

  // Helper Code ***********************************************************

  /**
   * Sets the text of the given label and optionally a mnemonic. The given text
   * may contain an ampersand (<tt>&amp;</tt>) to mark a mnemonic and its
   * position. Such a marker indicates that the character that follows the
   * ampersand shall be the mnemonic. If you want to use the ampersand itself
   * duplicate it, for example <tt>&quot;Look&amp;&amp;Feel&quot</tt>.
   * 
   * @param label the label that gets a mnemonic
   * @param textWithMnemonic the text with optional mnemonic marker
   */
  private static void setTextAndMnemonic(JLabel label, String textWithMnemonic) {
    int markerIndex = textWithMnemonic.indexOf(MNEMONIC_MARKER);
    // No marker at all
    if (markerIndex == -1) {
      label.setText(textWithMnemonic);
      return;
    }
    int mnemonicIndex = -1;
    int begin = 0;
    int end;
    int length = textWithMnemonic.length();
    int quotedMarkers = 0;
    StringBuffer buffer = new StringBuffer();
    do {
      // Check whether the next index has a mnemonic marker, too
      if ((markerIndex + 1 < length)
          && (textWithMnemonic.charAt(markerIndex + 1) == MNEMONIC_MARKER)) {
        end = markerIndex + 1;
        quotedMarkers++;
      } else {
        end = markerIndex;
        if (mnemonicIndex == -1) {
          mnemonicIndex = markerIndex - quotedMarkers;
        }
      }
      buffer.append(textWithMnemonic.substring(begin, end));
      begin = end + 1;
      markerIndex = begin < length ? textWithMnemonic.indexOf(MNEMONIC_MARKER,
          begin) : -1;
    } while (markerIndex != -1);
    buffer.append(textWithMnemonic.substring(begin));

    String text = buffer.toString();
    label.setText(text);
    if ((mnemonicIndex != -1) && (mnemonicIndex < text.length())) {
      label.setDisplayedMnemonic(text.charAt(mnemonicIndex));
      label.setDisplayedMnemonicIndex(mnemonicIndex);
    }
  }

  /**
   * A layout for the title label and separator(s) in titled separators.
   */
  private static final class TitledSeparatorLayout implements LayoutManager {

    private final boolean centerSeparators;

    /**
     * Constructs a TitledSeparatorLayout that either centers the separators or
     * aligns them along the font baseline of the title label.
     * 
     * @param centerSeparators true to center, false to align along the font
     *          baseline of the title label
     */
    private TitledSeparatorLayout(boolean centerSeparators) {
      this.centerSeparators = centerSeparators;
    }

    /**
     * Does nothing. This layout manager looks up the components from the layout
     * container and used the component's index in the child array to identify
     * the label and separators.
     * 
     * @param name the string to be associated with the component
     * @param comp the component to be added
     */
    public void addLayoutComponent(String name, Component comp) {
      // Does nothing.
    }

    /**
     * Does nothing. This layout manager looks up the components from the layout
     * container and used the component's index in the child array to identify
     * the label and separators.
     * 
     * @param comp the component to be removed
     */
    public void removeLayoutComponent(Component comp) {
      // Does nothing.
    }

    /**
     * Computes and returns the minimum size dimensions for the specified
     * container. Forwards this request to <code>#preferredLayoutSize</code>.
     * 
     * @param parent the component to be laid out
     * @return the container's minimum size.
     * @see #preferredLayoutSize(Container)
     */
    public Dimension minimumLayoutSize(Container parent) {
      return preferredLayoutSize(parent);
    }

    /**
     * Computes and returns the preferred size dimensions for the specified
     * container. Returns the title label's preferred size.
     * 
     * @param parent the component to be laid out
     * @return the container's preferred size.
     * @see #minimumLayoutSize(Container)
     */
    public Dimension preferredLayoutSize(Container parent) {
      Component label = getLabel(parent);
      Dimension labelSize = label.getPreferredSize();
      Insets insets = parent.getInsets();
      int width = labelSize.width + insets.left + insets.right;
      int height = labelSize.height + insets.top + insets.bottom;
      return new Dimension(width, height);
    }

    /**
     * Lays out the specified container.
     * 
     * @param parent the container to be laid out
     */
    public void layoutContainer(Container parent) {
      synchronized (parent.getTreeLock()) {
        // Look up the parent size and insets
        Dimension size = parent.getSize();
        Insets insets = parent.getInsets();
        int width = size.width - insets.left - insets.right;

        // Look up components and their sizes
        JLabel label = getLabel(parent);
        Dimension labelSize = label.getPreferredSize();
        int labelWidth = labelSize.width;
        int labelHeight = labelSize.height;
        Component separator1 = parent.getComponent(1);
        int separatorHeight = separator1.getPreferredSize().height;

        FontMetrics metrics = label.getFontMetrics(label.getFont());
        int ascent = metrics.getMaxAscent();
        int hGapDlu = centerSeparators ? 3 : 1;
        int hGap = Sizes.dialogUnitXAsPixel(hGapDlu, label);
        int vOffset = centerSeparators
            ? 1 + (labelHeight - separatorHeight) / 2
            : ascent - separatorHeight / 2;

        int alignment = label.getHorizontalAlignment();
        int y = insets.top;
        if (alignment == JLabel.LEFT) {
          int x = insets.left;
          label.setBounds(x, y, labelWidth, labelHeight);
          x += labelWidth;
          x += hGap;
          int separatorWidth = size.width - insets.right - x;
          separator1.setBounds(x, y + vOffset, separatorWidth, separatorHeight);
        } else if (alignment == JLabel.RIGHT) {
          int x = insets.left + width - labelWidth;
          label.setBounds(x, y, labelWidth, labelHeight);
          x -= hGap;
          x--;
          int separatorWidth = x - insets.left;
          separator1.setBounds(insets.left, y + vOffset, separatorWidth,
              separatorHeight);
        } else {
          int xOffset = (width - labelWidth - 2 * hGap) / 2;
          int x = insets.left;
          separator1.setBounds(x, y + vOffset, xOffset - 1, separatorHeight);
          x += xOffset;
          x += hGap;
          label.setBounds(x, y, labelWidth, labelHeight);
          x += labelWidth;
          x += hGap;
          Component separator2 = parent.getComponent(2);
          int separatorWidth = size.width - insets.right - x;
          separator2.setBounds(x, y + vOffset, separatorWidth, separatorHeight);
        }
      }
    }

    private JLabel getLabel(Container parent) {
      return (JLabel) parent.getComponent(0);
    }

  }

  /**
   * A label that uses the TitleBorder font and color.
   */
  private static final class StandardLabel extends JLabel {
    private StandardLabel() {
      // Just invoke the super constructor.
    }

    /**
     * TODO: Consider asking a <code>TitledBorder</code> instance for its font
     * and font color use <code>#getTitleFont</code> and
     * <code>#getTitleColor</code> for the Synth-based looks.
     */
    public void updateUI() {
      super.updateUI();
      Color foreground = UIManager.getColor("TitledBorder.titleColor"); //$NON-NLS-1$
      if (foreground != null)
        setForeground(foreground);
      setFont(getTitleFont());
    }

    /**
     * Looks up and returns the font used for title labels. Since Mac Aqua uses
     * an inappropriate titled border font, we use a bold label font instead.
     * Actually if the title is used in a titled separator, the bold weight is
     * questionable. It seems that most native Aqua tools use a plain label in
     * titled separators.
     * 
     * @return the font used for title labels
     */
    private Font getTitleFont() {
      return isLafAqua() ? UIManager.getFont("Label.font") //$NON-NLS-1$
          .deriveFont(Font.BOLD) : UIManager.getFont("InternalFrame.titleFont"); //$NON-NLS-1$
    }
  }

  /**
   * A label that uses the TitleBorder font and color.
   */
//  private static final class TitleLabel extends JLabel {
//    private TitleLabel() {
//      // Just invoke the super constructor.
//    }
//
//    private TitleLabel(String text) {
//      super(text);
//    }
//
//    /**
//     * TODO: Consider asking a <code>TitledBorder</code> instance for its font
//     * and font color use <code>#getTitleFont</code> and
//     * <code>#getTitleColor</code> for the Synth-based looks.
//     */
//    public void updateUI() {
//      super.updateUI();
//      Color foreground = UIManager.getColor("textHighlight");
//      if (foreground != null)
//        setForeground(foreground);
//      setFont(getTitleFont());
//    }
//
//    /**
//     * Looks up and returns the font used for title labels. Since Mac Aqua uses
//     * an inappropriate titled border font, we use a bold label font instead.
//     * Actually if the title is used in a titled separator, the bold weight is
//     * questionable. It seems that most native Aqua tools use a plain label in
//     * titled separators.
//     * 
//     * @return the font used for title labels
//     */
//    private Font getTitleFont() {
//      return isLafAqua() ? UIManager.getFont("Label.font")
//          .deriveFont(Font.BOLD) : UIManager.getFont("InternalFrame.titleFont")
//          .deriveFont(Font.BOLD, AffineTransform.getScaleInstance(1.5, 1.5));
//    }
//  }

  /**
   * Holds the cached result of the Aqua l&amp;f check. Is invalidated by the
   * <code>LookAndFeelChangeHandler</code> if the look&amp;feel changes.
   */
  private static Boolean cachedIsLafAqua;

  /**
   * Describes whether the <code>LookAndFeelChangeHandler</code> has been
   * registered with the <code>UIManager</code> or not. It is registered
   * lazily when the first cached l&amp;f state is computed.
   */
  private static boolean changeHandlerRegistered = false;

  private static synchronized void ensureLookAndFeelChangeHandlerRegistered() {
    if (!changeHandlerRegistered) {
      UIManager.addPropertyChangeListener(new LookAndFeelChangeHandler());
      changeHandlerRegistered = true;
    }
  }

  /**
   * Lazily checks and answers whether the Aqua look&amp;feel is active.
   * 
   * @return true if the current look&amp;feel is Aqua
   */
  private static boolean isLafAqua() {
    if (cachedIsLafAqua == null) {
      cachedIsLafAqua = Boolean.valueOf(computeIsLafAqua());
      ensureLookAndFeelChangeHandlerRegistered();
    }
    return cachedIsLafAqua.booleanValue();
  }

  /**
   * Computes and answers whether the Aqua look&amp;feel is active.
   * 
   * @return true if the current look&amp;feel is Aqua
   */
  private static boolean computeIsLafAqua() {
    LookAndFeel laf = UIManager.getLookAndFeel();
    return laf.getName().startsWith("Mac OS X Aqua"); //$NON-NLS-1$
  }

  // Listens to changes of the Look and Feel and invalidates the cache
  private static class LookAndFeelChangeHandler
      implements
        PropertyChangeListener {

    /**
     * Invalidates the cached laf states if the look&amp;feel changes.
     * 
     * @param evt describes the property change
     */
    public void propertyChange(PropertyChangeEvent evt) {
      cachedIsLafAqua = null;
    }
  }

}
