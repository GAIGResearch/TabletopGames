package games.puertorico.gui;

import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.PuertoRicoParameters;
import games.puertorico.components.Ship;
import utilities.Pair;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static games.puertorico.gui.PRGUIUtils.*;

public class GSShipsAndMarket extends JComponent {
    PuertoRicoGameState gs;
    Dimension size;
    int marketCapacity;
    int nCols;
    int marketWidth;
    int marketHeight;

    public GSShipsAndMarket(PuertoRicoGameState gs) {
        this.gs = gs;
        this.marketCapacity = ((PuertoRicoParameters)gs.getGameParameters()).marketCapacity;
        this.nCols = marketCapacity / nSpacesOnLine;
        this.marketWidth = Math.max((nCols+1)*shipSpaceSize, PuertoRicoConstants.Crop.values().length * (barrelWidth + pad*4) + pad*2);
        this.marketHeight = (nSpacesOnLine+1) * shipSpaceSize + barrelHeight + textFontSize;
        size = new Dimension(marketWidth * 2, marketHeight*2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Ships
        int startX = pad;
        for (Ship s: gs.getShips()) {
            Pair<Integer, Integer> shipSize = drawShip((Graphics2D) g, s, startX, pad);
            startX += shipSize.a;
        }
        startX += pad*3;

        // Market
        // General container
        g.setColor(shipColor);
        g.fillRect(startX, pad, marketWidth, marketHeight);
        if (outline) {
            g.setColor(Color.black);
            g.drawRect(startX, pad, marketWidth, marketHeight);
        }

        // Title
        g.setFont(textFontBold);
        g.setColor(Color.black);
        g.drawString("Trading House", startX + pad, pad + textFontSize);

        // Spaces in market
        startX += shipSpaceSize/2;
        int x = startX;
        int y = pad + shipSpaceSize/2 - shipSpaceSize + textFontSize;
        List<PuertoRicoConstants.Crop> market = gs.getMarket();
        for (int i = 0; i < marketCapacity; i++) {
            if (i % nCols == 0) {
                y += shipSpaceSize;
                x = startX;
            }
            // Draw space
            g.setColor(spaceColor);
            g.fillRect(x, y, shipSpaceSize, shipSpaceSize);
            if (outline) {
                g.setColor(Color.black);
                g.drawRect(x, y, shipSpaceSize, shipSpaceSize);
            }
            // Draw crop
            if (i < market.size()) {
                drawBarrel((Graphics2D) g, x + shipSpaceSize/2 - barrelWidth/2, y + shipSpaceSize/2 - barrelHeight/2, cropColorMap.get(market.get(i)));
            }
            x += shipSpaceSize;
        }

        // Point values
        x = startX;
        y += shipSpaceSize + textFontSize + pad;
        g.setFont(textFontBold);
        for (PuertoRicoConstants.Crop crop: PuertoRicoConstants.Crop.values()) {
            PRGUIUtils.drawBarrel((Graphics2D) g, x, y-textFontSize, PRGUIUtils.cropColorMap.get(crop));
            x += PRGUIUtils.barrelWidth;
            String s1 = "=" + crop.price;
            g.setColor(Color.black);
            g.drawString(s1, x, y);
            x += pad*4;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }
    @Override
    public Dimension getMinimumSize() {
        return size;
    }
}
