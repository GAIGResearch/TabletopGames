package gui;

import javax.swing.*;


@SuppressWarnings("rawtypes")
public class GUI extends JFrame {
    public static int defaultItemSize = 50;
    public static int defaultActionPanelHeight = 100;
    public static int defaultInfoPanelHeight = 180;
    public static int defaultCardWidth = 100, defaultCardHeight = 80;
    public static int defaultBoardWidth = 400, defaultBoardHeight = 300;
    public static int defaultDisplayWidth = 500, defaultDisplayHeight = 400;

    private WindowInput wi;

    public GUI() {
    }

    public void setFrameProperties() {
        // Frame properties
        this.wi = new WindowInput();
        addWindowListener(wi);
        revalidate();
        pack();
        this.setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        repaint();
    }

    /**
     * Checks if the window is open.
     * @return true if open, false otherwise
     */
    public final boolean isWindowOpen() {
        return !wi.windowClosed;
    }


}
