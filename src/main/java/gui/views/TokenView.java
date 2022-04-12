package gui.views;

import core.components.Token;

import javax.swing.*;
import java.awt.*;

import static gui.GUI.defaultItemSize;

public class TokenView extends JComponent {
    protected Token token;
    protected int size;

    public TokenView(Token c) {
        updateToken(c);
        size = defaultItemSize;
    }

    public void updateToken(Token c) {
        this.token = c;
        if (c != null) {
            setToolTipText("Component ID: " + c.getComponentID());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawToken((Graphics2D) g);
    }

    public void drawToken(Graphics2D g) {
        drawToken(g, 0, 0, size, token);
    }

    public static void drawToken(Graphics2D g, Token token, Rectangle r) {
        drawToken(g, r.x, r.y, r.width, token);
    }

    public static void drawToken(Graphics2D g, int x, int y, int size, Token token) {
        int fontSize = g.getFont().getSize();
        int counterSize = size - fontSize;
        int counterX = x + size/2 - counterSize/2;
        int counterY = y + size/2 - counterSize/2;

        // Draw background
        g.setColor(Color.lightGray);
        g.fillOval(counterX, counterY, counterSize-1, counterSize-1);
        g.setColor(Color.black);

        // Draw border
        g.drawOval(counterX, counterY, counterSize-1, counterSize-1);

        // Draw token name underneath
        String name = "Token";
        if (token != null) {
            name = token.getComponentName();
        }
        g.drawString(name, x, y + counterSize + fontSize);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(size, size);
    }

    public Token getToken() {
        return token;
    }
}
