package application.karel.utils;

import java.awt.Component;
import javax.swing.JTextPane;
import javax.swing.text.StyledDocument;

public class NonWrappingTextPane extends JTextPane
{
  public NonWrappingTextPane()
  {
    super();
  }

  public NonWrappingTextPane(StyledDocument doc)
  {
    super(doc);
  }

  // Override getScrollableTracksViewportWidth
  // to preserve the full width of the text
    @Override public boolean getScrollableTracksViewportWidth()
    {
        Component parent = getParent();

        return parent != null ? (getUI().getPreferredSize(this).width <= parent.getSize().width) : true;
  }
}