/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-11 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import java.awt.event.*;
import java.awt.font.*;
import java.awt.*;
import java.net.*;
import java.text.*;

import processing.app.Library.LibraryInfo;
import processing.app.Library.LibraryInfo.Author;
import processing.app.LibraryListing.LibraryChangeListener;

public class LibraryListPanel extends JPanel implements Scrollable, LibraryChangeListener {
  
  private static HyperlinkListener nullHyperlinkListener = new HyperlinkListener() {
    
    public void hyperlinkUpdate(HyperlinkEvent e) {
    }
  };
  
  HashMap<LibraryPanel, Integer> rowForEachPanel;
  
  @SuppressWarnings("unused")
  private PreferredViewPositionListener preferredViewPositionListener;

  LibraryManager libraryManager;
  JProgressBar setupProgressBar;
  ArrayList<LibraryPanel> libPanels;
  
  public LibraryListPanel(LibraryManager libraryManager, LibraryListing libraryListing) {
    super();
    
    this.libraryManager = libraryManager;
    
    rowForEachPanel = new HashMap<LibraryPanel, Integer>();
    
    preferredViewPositionListener = new PreferredViewPositionListener() {
      
      public void handlePreferredLocation(Point p) {
      }

    };
    
    setLayout(new GridBagLayout());
    setFocusable(true);
    setOpaque(true);
    
    if (Base.isLinux()) {
      // Thanks to a bug with GNOME, getColor returns the wrong value for
      // List.background. We'll just assume its white. The intersection
      // of people using Linux and people using a weird inverted color theme
      // should be small enough.
      setBackground(Color.white);
    } else {
      setBackground(UIManager.getColor("List.background"));
    }
    
    libPanels = new ArrayList<LibraryPanel>();
    
    addMouseListener(new MouseAdapter() {

      public void mousePressed(MouseEvent mouseEvent) {
        requestFocusInWindow();
      }
    });
    
    if (libraryListing == null) {
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 1;
      c.weighty = 1;
      c.anchor = GridBagConstraints.CENTER;
      
      setupProgressBar = new JProgressBar();
      setupProgressBar.setString("");
      setupProgressBar.setStringPainted(true);
      add(setupProgressBar, c);
    } else {
      // Add all the libraries in libraryListing to this panel
      for (LibraryInfo info : libraryListing.getAllLibararies()) {
        libraryAdded(info);
      }
    }
    
  }
  
  public void libraryAdded(LibraryInfo libraryInfo) {
    
    LibraryPanel newPanel = new LibraryPanel(libraryInfo);
    
    synchronized (libPanels) {
      libPanels.add(newPanel);
      Collections.sort(libPanels);
    }
    
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 1;
    c.gridx = 0;
    c.gridy = getRowForPanel(newPanel);
    rowForEachPanel.put(newPanel, c.gridy);
    
    add(newPanel, c);
    updateColors();
  }
  
  private int getRowForPanel(LibraryPanel newPanel) {
    int currentIndex = 0;
    while (currentIndex < libPanels.size()) {
      LibraryPanel currentPanel = libPanels.get(currentIndex);
      if (currentPanel == newPanel) {
        break;
      }
      currentIndex++;
    }
    
    int lastIndex = currentIndex - 1;
    int nextIndex = currentIndex + 1;
    LibraryPanel last = null, next = null;
    if (0 <= lastIndex && lastIndex < libPanels.size())
      last = libPanels.get(lastIndex);
    if (0 <= nextIndex && nextIndex < libPanels.size())
      next = libPanels.get(nextIndex);
    
    Integer lastRow = rowForEachPanel.get(last);
    if (lastRow == null) lastRow = 0;
    Integer nextRow = rowForEachPanel.get(next);
    if (nextRow == null) nextRow = 16384;
    
    return lastRow + ((nextRow - lastRow) / 2);
  }

  public void libraryRemoved(LibraryInfo libraryInfo) {
    
    synchronized (libPanels) {
      Iterator<LibraryPanel> it = libPanels.iterator();
      while (it.hasNext()) {
        LibraryPanel panel = it.next();
        if (panel.info == libraryInfo) {
          rowForEachPanel.remove(panel);
          remove(panel);
          it.remove();
        }
      }
    }
    
    updateUI();
  }
  
  public void libraryChanged(LibraryInfo oldLib, LibraryInfo newLib) {
    synchronized (libPanels) {
      for (LibraryPanel panel : libPanels) {
        if (panel.info == oldLib) {
          panel.info = newLib;
          panel.updateState();
        }
      }
    }
  }
  
  public void filterLibraries(List<LibraryInfo> filteredLibraries) {

    if (libPanels != null) {
      synchronized (libPanels) {

        List<LibraryPanel> hiddenPanels = new ArrayList(libPanels);
        for (LibraryInfo lib : filteredLibraries) {

          for (LibraryPanel libPanel : libPanels) {
            if (libPanel.info == lib) {
              libPanel.setVisible(true);
              hiddenPanels.remove(libPanel);
            }
          }
        }

        for (LibraryPanel libPanel : hiddenPanels) {
          libPanel.setVisible(false);
        }
      }
    }
  }

  /**
   * Sets the foreground color for a component and all of its subcompenents
   * recursively.
   */
  private static void cascadeForgroundColor(Component component, Color color) {
    if (component instanceof Container) {
      for (Component c : ((Container) component).getComponents()) {
        cascadeForgroundColor(c, color);
      }
    }
    if (component instanceof JLabel || component instanceof JTextComponent) {
      component.setForeground(color);
    }
    
    if (component instanceof JEditorPane) {
      JEditorPane editorPane = (JEditorPane) component;
      Document doc = editorPane.getDocument();
      
      if (doc instanceof HTMLDocument) {
        HTMLDocument html = (HTMLDocument) doc;
        StyleSheet stylesheet = html.getStyleSheet();
        stylesheet.addRule("body {color:" + toHex(color) + ";}");
      }
      
      // This call improves the response time, but appears to cause the
      // "AWT-EventQueue-0" thread to throw a NullPointerException:
      // editorPane.updateUI();
    }
  }
  
  /**
   * Counts the numbers of lines needed to display the text in a JTextArea given
   * a width as a contained. This assumed that both word wrap and wrap-style
   * word are enabled for the JTextArea.
   */
  private static int lineCount(JTextArea textArea, int width) {
    try {
      AttributedString text = new AttributedString(textArea.getText());
      FontRenderContext frc = textArea.getFontMetrics(textArea.getFont())
          .getFontRenderContext();
      AttributedCharacterIterator charIt = text.getIterator();
      LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(charIt, frc);
      lineMeasurer.setPosition(charIt.getBeginIndex());
    
      // Get lines from lineMeasurer until the entire
      // paragraph has been displayed.
      int noLines = 0;
      while (lineMeasurer.getPosition() < charIt.getEndIndex()) {
        lineMeasurer.nextLayout(width);
        noLines++;
      }
      return noLines;
      
    } catch (IllegalArgumentException e) {
      return 1;
    }
    
  }

  /**
   * Updates the colors of all library panels that are visible.
   */
  private void updateColors() {

    int count = 0;
    synchronized (libPanels) {
      for (LibraryPanel libPanel : libPanels) {

          if (libPanel.isVisible() && libPanel.isSelected) {
            libPanel.setBackground(UIManager.getColor("List.selectionBackground"));
            cascadeForgroundColor(libPanel, UIManager.getColor("List.selectionForeground"));
            libPanel.setBorder(UIManager.getBorder("List.focusCellHighlightBorder"));
            count++;
            
          } else {
            Border border = null;
            if (Base.isMacOS() && libPanel.isVisible()) {
              if (count % 2 == 1) {
                border = UIManager.getBorder("List.evenRowBackgroundPainter");
              } else {
                border = UIManager.getBorder("List.oddRowBackgroundPainter");
              }
              count++;
            }

            if (border == null) {
              border = BorderFactory.createEmptyBorder(1, 1, 1, 1);
            }

            libPanel.setBorder(border);

            libPanel.setBackground(LibraryListPanel.this.getBackground());
            cascadeForgroundColor(libPanel,
                                  UIManager.getColor("List.foreground"));
          }

        
        
        libPanel.updateHyperLinkStyles();
      }
    }
  }

  static String toHex(Color c) {
    StringBuilder hex = new StringBuilder();
    hex.append(Integer.toString(c.getRed(), 16));
    hex.append(Integer.toString(c.getGreen(), 16));
    hex.append(Integer.toString(c.getBlue(), 16));
    return hex.toString();
  }

  /**
   * Calculates the height in pixels of the text in a JTextArea given a width
   * as a contained. This assumed that both word wrap and wrap-style word are
   * enabled for the JTextArea.
   */
  @SuppressWarnings("unused")
  private static int calculateHeight(JTextArea textArea, int width) {
    Font font = textArea.getFont();
    FontMetrics fontMetrics = textArea.getFontMetrics(font);
    int lineHeight = fontMetrics.getAscent() + fontMetrics.getDescent();
    return lineHeight * lineCount(textArea, width);
  }
  
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  /**
   * Amount to scroll to reveal a new page of items
   */
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    if (orientation == SwingConstants.VERTICAL) {
      int blockAmount = visibleRect.height;
      if (direction > 0) {
        visibleRect.y += blockAmount;
      } else {
        visibleRect.y -= blockAmount;
      }
      
      blockAmount += getScrollableUnitIncrement(visibleRect, orientation, direction);
      return blockAmount;
    }
    return 0;
  }

  /**
   * Amount to scroll to reveal the rest of something we are on or a new item
   */
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    if (orientation == SwingConstants.VERTICAL) {

      int lastHeight = 0, height = 0;
      int bottomOfScrollArea = visibleRect.y + visibleRect.height;

      for (Component c : getComponents()) {
        if (c.isVisible()) {
          if (c instanceof LibraryPanel) {
            Dimension d = c.getPreferredSize();

            int nextHeight = height + d.height;

            if (direction > 0) {
              // scrolling down
              if (nextHeight > bottomOfScrollArea) {
                return nextHeight - bottomOfScrollArea;
              }
            } else {
              // scrolling up
              if (nextHeight > visibleRect.y) {
                if (visibleRect.y != height) {
                  return visibleRect.y - height;
                } else {
                  return visibleRect.y - lastHeight;
                }
              }
            }

            lastHeight = height;
            height = nextHeight;
          }
        }
      }
    }
    
    return 0;
  }
  
  public boolean getScrollableTracksViewportHeight() {
    return false;
  }

  public boolean getScrollableTracksViewportWidth() {
    return true;
  }

  public void setPreferredViewPositionListener(PreferredViewPositionListener preferredViewPositionListener) {
    this.preferredViewPositionListener = preferredViewPositionListener;
  }

  public JProgressBar getSetupProgressBar() {
    return setupProgressBar;
  }
  
  /**
   * Panel that expands and gives a brief overview of a library when clicked.
   */
  class LibraryPanel extends JPanel implements Comparable<LibraryPanel> {
    
    private static final int BUTTON_WIDTH = 100;

    boolean okayToOpenHyperLink;
    
    class ConditionalHyperlinkListener implements HyperlinkListener {
      
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          if (okayToOpenHyperLink) {
            Base.openURL(e.getURL().toString());
          }
        }
      }
    }
    HyperlinkListener hyperlinkOpener;
    
    ActionListener removeAction;
    ActionListener installLibAction;
    
    LibraryInfo info;
    
    JTextPane headerLabel;
    
    JLabel categoryLabel;
    
    JTextPane descriptionText;

    JProgressBar installProgressBar;
    
    JButton installOrRemove;

    boolean isSelected;
    
    private LibraryPanel(LibraryInfo libInfo) {
      this.info = libInfo;
      
      okayToOpenHyperLink = false;
      hyperlinkOpener = new ConditionalHyperlinkListener();
      
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      
      createAuthorString();

      createInstallRemoveActions();
      addPaneComponents();
      addProgressBarAndButton();

      setBackground(LibraryListPanel.this.getBackground());
      setOpaque(true);
      setFocusable(true);
      setSelected(false);
      
      MouseAdapter expandPanelMouseListener = new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          
          okayToOpenHyperLink = isSelected;
          
          if (!isSelected) {
            for (Component c : LibraryListPanel.this.getComponents()) {
              if (c instanceof LibraryPanel) {
                LibraryPanel lp = (LibraryPanel) c;
                if (lp.isSelected) {
                  lp.setSelected(false);
                  break;
                }
              }
            }
  
            setSelected(true);
            updateColors();
            getParent().requestFocusInWindow();
          }
        }

      };
      
      addMouseListener(expandPanelMouseListener);
      stripListeners(headerLabel);
      stripListeners(descriptionText);
      headerLabel.addMouseListener(expandPanelMouseListener);
      categoryLabel.addMouseListener(expandPanelMouseListener);
      descriptionText.addMouseListener(expandPanelMouseListener);
    }
    
    /**
     * @return true if the install action was added, false if it was already
     *         being used.
     */
    public boolean useInstalledAction() {
      if (Arrays.asList(installOrRemove.getActionListeners())
          .contains(installLibAction)) {
        return false;
      } else {
        installOrRemove.removeActionListener(removeAction);
        installOrRemove.setText("Install");
        installOrRemove.addActionListener(installLibAction);
        return true;
      }
    }
    
    /**
     * @return true if the remove action was added, false if it was already
     *         being used.
     */
    public boolean useRemoveAction() {
      if (Arrays.asList(installOrRemove.getActionListeners())
          .contains(removeAction)) {
        return false;
      } else {
        installOrRemove.removeActionListener(installLibAction);
        installOrRemove.setText("Remove");
        installOrRemove.addActionListener(removeAction);
        return true;
      }
    }

    private void createInstallRemoveActions() {
      
     removeAction = new ActionListener() {
        
        public void actionPerformed(ActionEvent arg) {
          installOrRemove.setEnabled(false);
          
          installProgressBar.setVisible(true);
          libraryManager.uninstallLibrary(info.library,
            new JProgressMonitor(installProgressBar) {
              
              public void finishedAction() {
                // Finished uninstalling the library
                resetInstallProgressBarState();
                installOrRemove.setEnabled(true);
              }
            }
          );
          
        }
      };
      
      installLibAction = new ActionListener() {
        
        public void actionPerformed(ActionEvent arg) {
          installOrRemove.setEnabled(false);
          
          try {
            URL url = new URL(info.link);
            
            installProgressBar.setVisible(true);
            
            libraryManager.installLibraryFromUrl(url, LibraryPanel.this,
              new JProgressMonitor(installProgressBar) {
  
                public void finishedAction() {
                  // Finished downloading library
                }
              },
              new JProgressMonitor(installProgressBar) {
  
                public void finishedAction() {
                  // Finished installing library
                  resetInstallProgressBarState();
                  installOrRemove.setEnabled(true);
                }
              }
            );
            
          } catch (MalformedURLException e) {
            Base.showWarning("Install Failed",
                             "The link fetched from Processing.org is invalid.\n" +
                             "You can still intall this library manually by visiting\n" +
                             "the library's website.", e);
            installOrRemove.setEnabled(true);
          }
        }
      };
    }

    void stripListeners(JEditorPane editorPane) {
      for (MouseListener l : editorPane.getMouseListeners()) {
        if (!l.getClass().getName().endsWith("LinkController")) {
          editorPane.removeMouseListener(l);
        }
      }
    }

    private String createAuthorString() {
      StringBuilder authors = new StringBuilder();
      
      if (info.authorList != null && !info.authorList.isEmpty()) {
        authors.append(" by ");
        
        for (int i = 0; i < info.authorList.size(); i++) {
          Author author = info.authorList.get(i);
          if (author.url == null) {
            authors.append(author.name);
          } else {
            authors.append("<a href=\"" + author.url + "\">" + author.name + "</a>");
          }
          if (i + 2 < info.authorList.size()) {
            authors.append(", ");
          } else if (i + 2 == info.authorList.size()) {
            if (info.authorList.size() > 2) {
              authors.append(", and ");
            } else {
              authors.append(" and ");
            }
          }
        }
        
      }
      
      return authors.toString();
    }

    void setHtmlTextStyle(JTextPane textPane, boolean justified) {
      
      textPane.setHighlighter(null);
      textPane.setOpaque(false);
      textPane.setEditable(true);
      
      Font font = UIManager.getFont("Label.font");

      if (justified) {
        StyledDocument sdoc = textPane.getStyledDocument();
        SimpleAttributeSet sa = new SimpleAttributeSet();
        StyleConstants.setAlignment(sa, StyleConstants.ALIGN_JUSTIFIED);
        sdoc.setParagraphAttributes(0, 1, sa, false);
      }
      
      Document doc = textPane.getDocument();
      
      if (doc instanceof HTMLDocument) {
        HTMLDocument html = (HTMLDocument) doc;
        StyleSheet stylesheet = html.getStyleSheet();
        
        stylesheet.addRule("body {font-family:"+font.getFamily()+";" + 
                           "font-size:"+font.getSize()+"pt;}");
      }
      
      updateHyperLinkStyle(textPane);
    }
    
    void updateHyperLinkStyles() {
      updateHyperLinkStyle(headerLabel);
      updateHyperLinkStyle(descriptionText);
    }
    
    private void updateHyperLinkStyle(JTextPane textPane) {
      Document doc = textPane.getDocument();
      
      if (doc instanceof HTMLDocument) {

        HTMLDocument html = (HTMLDocument) doc;
        
        StyleSheet stylesheet = html.getStyleSheet();
        
        if (isSelected) {
          // Use hyperlink color and underline
          // XXX: Get the hyperlink color from the system
          Color linkColor = UIManager.getColor("List.selectionForeground");
          
          stylesheet.addRule("a {color:" + toHex(linkColor) + ";"
                              + "text-decoration:underline}");
        } else {
          Color foreground = UIManager.getColor("List.foreground");
          
          // Use normal forground color and do not underline
          stylesheet.addRule("a {color:" + toHex(foreground) + ";"
                              + "text-decoration:none}");
        }
      }
    }
    
    /**
     * Create the widgets for the header panel which is visible when the library
     * panel is not clicked
     */
    private void addPaneComponents() {
      setFocusable(true);
      setLayout(new GridBagLayout());

      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.weightx = 1;
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.WEST;
      
      headerLabel = new JTextPane();
      headerLabel.setContentType("text/html");
//      nameLabel.getDocument().
//      ((HTMLDocument)nameLabel.getDocument()).getStyleSheet().addRule("a {font-family:"+font.getFamily()+";}");
      
      StringBuilder header = new StringBuilder();
      header.append("<html><body>");
      header.append("<b>");
      if (info.url == null) {
        header.append(info.name);
      } else {
        header.append("<a href=\"" + info.url + "\">" + info.name + "</a>");
      }
      header.append("</b>");
      header.append(createAuthorString());
      header.append("</body></html>");
      headerLabel.setText(header.toString());
      
      setHtmlTextStyle(headerLabel, false);
      headerLabel.addHyperlinkListener(nullHyperlinkListener);
      add(headerLabel, c);
      
      c = new GridBagConstraints();
      c.gridx = 1;
      c.gridy = 0;
      c.anchor = GridBagConstraints.EAST;
      categoryLabel = new JLabel("[" + info.category + "]");
      categoryLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 7));
      add(categoryLabel, c);
      
//      c = new GridBagConstraints();
//      c.gridx = 2;
//      c.gridy = 0;
//      c.anchor = GridBagConstraints.NORTHEAST;
//      JLabel categoryLabel = new JLabel(libInfo.categoryName + "  ");
//      font = categoryLabel.getFont();
//      font = font.deriveFont(font.getSize() * 0.7f);
//      categoryLabel.setFont(font);
//      headerPanel.add(categoryLabel, c);
      
      c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 1;
      c.weightx = 1;
      c.gridwidth = 3;
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.WEST;

      descriptionText = new JTextPane();
      descriptionText.setContentType("text/html");
      StringBuilder description = new StringBuilder();
      description.append("<html><body>");
      
      if (info.sentence != null)
        description.append(info.sentence);
      
      if (info.sentence != null && info.paragraph != null)
        description.append(" ");
      
      if (info.sentence != null && info.paragraph != null)
        description.append(info.paragraph);
      
      description.append("</body></html>");
      descriptionText.setText(description.toString());
      descriptionText.addHyperlinkListener(nullHyperlinkListener);
      descriptionText.setMargin(new Insets(0, 25, 10, 5));
      setHtmlTextStyle(descriptionText, true);
      add(descriptionText, c);
    }
    
    public void addProgressBarAndButton() {
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 3;
      c.gridy = 0;
      c.weighty = 1;
      c.gridheight = 2;
      c.fill = GridBagConstraints.VERTICAL;
      c.anchor = GridBagConstraints.NORTH;
      JPanel rightPane = new JPanel();
      rightPane.setOpaque(false);
      rightPane.setLayout(new BoxLayout(rightPane, BoxLayout.Y_AXIS));
      add(rightPane, c);
      
      installProgressBar = new JProgressBar();
      installProgressBar.setStringPainted(true);
      resetInstallProgressBarState();
      Dimension d = installProgressBar.getPreferredSize();
      d.width = BUTTON_WIDTH;
      installProgressBar.setPreferredSize(d);
      installProgressBar.setMaximumSize(d);
      installProgressBar.setMinimumSize(d);
      installProgressBar.setOpaque(false);
      rightPane.add(installProgressBar);
      installProgressBar.setAlignmentX(CENTER_ALIGNMENT);
      
      rightPane.add(Box.createVerticalGlue());
      
      installOrRemove = new JButton();
      updateState();

      Dimension installButtonDimensions = installOrRemove.getPreferredSize();
      installButtonDimensions.width = BUTTON_WIDTH;
      installOrRemove.setPreferredSize(installButtonDimensions);
      installOrRemove.setMaximumSize(installButtonDimensions);
      installOrRemove.setMinimumSize(installButtonDimensions);
      installOrRemove.setOpaque(false);
      rightPane.add(installOrRemove);
      installOrRemove.setAlignmentX(CENTER_ALIGNMENT);
      
      // Set the minimum size of this pane to be the sum of the height of the
      // progress bar and install button
      d = installProgressBar.getPreferredSize();
      Dimension d2 = installOrRemove.getPreferredSize();
      d.width = BUTTON_WIDTH;
      d.height = d.height+d2.height;
      rightPane.setMinimumSize(d);
      rightPane.setPreferredSize(d);
    }
    
    public void updateState() {
      if (info.isInstalled()) {
        useRemoveAction();
      } else {
        useInstalledAction();
      }
    }
    
    private void resetInstallProgressBarState() {
      installProgressBar.setString("Starting");
      installProgressBar.setIndeterminate(true);
      installProgressBar.setValue(0);
      installProgressBar.setVisible(false);
    }

    public void setSelected(boolean doShow) {
      isSelected = doShow;
      installOrRemove.setVisible(doShow);
      
      updateLiseners(headerLabel);
      updateLiseners(descriptionText);
      
    }

    private void updateLiseners(JEditorPane editorPane) {
      if (isSelected) {
        editorPane.removeHyperlinkListener(nullHyperlinkListener);
        editorPane.addHyperlinkListener(hyperlinkOpener);
        editorPane.setEditable(false);
      } else {
        editorPane.removeHyperlinkListener(hyperlinkOpener);
        editorPane.addHyperlinkListener(nullHyperlinkListener);
        editorPane.setEditable(true);
      }
    }

    /**
     * Updates the sizes of components in this panel given width as a constraint
     */
    public void updateSize(int width) {
      Dimension d = getPreferredSize();
      d.width = width;

      setMaximumSize(d);
      setMinimumSize(d);
      setPreferredSize(d);
      setSize(d);
      
      revalidate();
    }

    public int compareTo(LibraryPanel o) {
      return info.name.toLowerCase().compareTo(o.info.name.toLowerCase());
    }
    
  }

  public static interface PreferredViewPositionListener {
    
    void handlePreferredLocation(Point p);
    
  }

}
