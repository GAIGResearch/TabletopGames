package games.puertorico.gui;

import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.PuertoRicoParameters;
import games.puertorico.actions.ShipCargo;
import games.puertorico.components.Ship;
import gui.IScreenHighlight;
import utilities.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static games.puertorico.gui.PRGUIUtils.*;

public class GSShipsAndMarket extends JComponent implements IScreenHighlight {
    PuertoRicoGameState gs;
    PuertoRicoGUI gui;
    Dimension size;
    int marketCapacity;
    int nCols;
    int marketWidth;
    int marketHeight;
    Map<Rectangle, PuertoRicoConstants.Crop> cropToRectMap = new HashMap<>();
    Map<Rectangle, Integer> shipToRectMap = new HashMap<>();
    Map<Rectangle, String> shipTooltipUnavailableActionMap = new HashMap<>();
    int shipClicked = -1;

    public GSShipsAndMarket(PuertoRicoGUI gui, PuertoRicoGameState gs) {
        this.gs = gs;
        this.gui = gui;
        this.marketCapacity = ((PuertoRicoParameters)gs.getGameParameters()).marketCapacity;
        this.nCols = marketCapacity / nSpacesOnLine;
        this.marketWidth = Math.max((nCols+1)*shipSpaceSize, (PuertoRicoConstants.Crop.values().length-1) * (barrelWidth + pad*4) + pad*2);
        this.marketHeight = (nSpacesOnLine+1) * shipSpaceSize + barrelHeight + textFontSize;
        size = new Dimension(marketWidth * 2, marketHeight*2);
        ToolTipManager.sharedInstance().registerComponent(this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                shipClicked = -1;
                for (Rectangle r : shipToRectMap.keySet()) {
                    if (r.contains(e.getPoint())) {
                        shipClicked = shipToRectMap.get(r);
                        // if crop selected on current player's board,
                        //  or crop exists on the ship and can be shipped by current player, submit the action
                        Set<PuertoRicoConstants.Crop> loadedCrops = gs.getShips().stream()
                                .map(Ship::getCurrentCargo)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());

                        boolean capacityCondition = gs.getShip(shipClicked).getAvailableCapacity() > 0;
                        boolean emptyAndClickedUniqueCondition = gs.getShip(shipClicked).getCurrentCargo() == null
                                && (gui.playerBoards[gs.getCurrentPlayer()].cropClicked != null
                                && !loadedCrops.contains(gui.playerBoards[gs.getCurrentPlayer()].cropClicked))
                                && gs.getStoresOf(gs.getCurrentPlayer(), gui.playerBoards[gs.getCurrentPlayer()].cropClicked) > 0;
                        boolean matchingCargoCondition = gs.getShip(shipClicked).getCurrentCargo() != null
                                && gs.getStoresOf(gs.getCurrentPlayer(), gs.getShip(shipClicked).getCurrentCargo()) > 0;

                        if (emptyAndClickedUniqueCondition || capacityCondition && matchingCargoCondition) {

                            PuertoRicoConstants.Crop crop;
                            if (emptyAndClickedUniqueCondition) {
                                crop = gui.playerBoards[gs.getCurrentPlayer()].cropClicked;
                            }
                            else crop = gs.getShip(shipClicked).getCurrentCargo();
                            int amount = Math.min(gs.getStoresOf(gs.getCurrentPlayer(), crop), gs.getShip(shipClicked).getAvailableCapacity());

                            gui.getAC().addAction(new ShipCargo(crop, shipClicked, amount));
                        }
                        break;
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        cropToRectMap.clear();
        shipTooltipUnavailableActionMap.clear();
        shipToRectMap.clear();
        int currentPlayer = gs.getCurrentPlayer();

        Set<PuertoRicoConstants.Crop> loadedCrops = gs.getShips().stream()
                .map(Ship::getCurrentCargo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Ships
        int startX = pad;
        for (int s = 0; s < gs.getShips().size(); s++) {
            Ship ship = gs.getShip(s);
            Pair<Integer, Integer> shipSize = drawShip((Graphics2D) g, ship, startX, pad);
            shipToRectMap.put(new Rectangle(startX, pad, shipSize.a, shipSize.b), s);

            if (gs.getCurrentRole() == PuertoRicoConstants.Role.CAPTAIN && gui.getHumanPlayerIds().contains(currentPlayer)) {
                PuertoRicoConstants.Crop selectedCrop = gui.playerBoards[currentPlayer].cropClicked;
                boolean playerHasMatchingCargo = false;
                boolean playerHasUniqueCargo = false;
                boolean playerHasAnyCargo = false;
                if (selectedCrop == null) {
                    for (PuertoRicoConstants.Crop c : PuertoRicoConstants.Crop.values()) {
                        if (gs.getStoresOf(currentPlayer, c) > 0) {
                            playerHasAnyCargo = true;
                            if (!playerHasMatchingCargo) playerHasMatchingCargo = ship.getCurrentCargo() == c;
                            if (!playerHasUniqueCargo) playerHasUniqueCargo = !loadedCrops.contains(c);
                        }
                    }
                }

                boolean capacityCondition = ship.getAvailableCapacity() > 0;
                boolean emptyAndUniqueCondition = ship.getCurrentCargo() == null
                        && (selectedCrop == null && playerHasUniqueCargo || !loadedCrops.contains(selectedCrop));
                boolean matchingCargoCondition = ship.getCurrentCargo() != null
                        && gs.getStoresOf(currentPlayer, ship.getCurrentCargo()) > 0;

                if (shipClicked == s) {
                    g.setColor(highlightColor);
                    g.drawRect(startX + pad, pad*2, shipSize.a-pad*2, shipSize.b-pad*2);
                }

                if (capacityCondition && (emptyAndUniqueCondition || matchingCargoCondition)) {
                    // Draw shadow fillers according to player's amount that would be put on the ship
                    PuertoRicoConstants.Crop crop;
                    if (emptyAndUniqueCondition) {
                        crop = selectedCrop;
                    }
                    else crop = ship.getCurrentCargo();
                    int amount = Math.min(gs.getStoresOf(gs.getCurrentPlayer(), crop), ship.getAvailableCapacity());
                    drawShip((Graphics2D) g, ship, startX, pad, crop, ship.getSpacesFilled() + amount);

                    g.setColor(highlightColor);
                    g.drawRect(startX, pad, shipSize.a, shipSize.b);

                } else {
                    g.setColor(Color.red);
                    g.drawRect(startX, pad, shipSize.a, shipSize.b);
                    String reason = "";
                    if (playerHasAnyCargo) {
                        if (!matchingCargoCondition) {
                            reason += "<li>You don't have any " + ship.getCurrentCargo() + ".</li>";
                        }
                        if (!emptyAndUniqueCondition) {
                            reason += "<li>Ship is empty and you have no crops that are not already loaded on other ships.</li>";
                        }
                    } else {
                        reason += "<li>You have no crops in store. Try producing first!</li>";
                    }
                    if (!capacityCondition) {
                        reason += "<li>Ship is full.</li>";
                    }
                    shipTooltipUnavailableActionMap.put(new Rectangle(startX, pad, shipSize.a, shipSize.b), reason);
                }
            }

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
                cropToRectMap.put(new Rectangle(x + shipSpaceSize/2 - barrelWidth/2, y + shipSpaceSize/2 - barrelHeight/2, barrelWidth, barrelHeight), market.get(i));
            }
            x += shipSpaceSize;
        }

        // Point values
        x = startX;
        y += shipSpaceSize + textFontSize + pad;
        g.setFont(textFontBold);
        for (PuertoRicoConstants.Crop crop: PuertoRicoConstants.Crop.values()) {
            if (crop == PuertoRicoConstants.Crop.QUARRY) continue;
            PRGUIUtils.drawBarrel((Graphics2D) g, x, y-textFontSize, PRGUIUtils.cropColorMap.get(crop));
            cropToRectMap.put(new Rectangle(x, y-textFontSize, barrelWidth, barrelHeight), crop);
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


    @Override
    public String getToolTipText(MouseEvent event) {
        if (showTooltips) {
            for (Map.Entry<Rectangle, PuertoRicoConstants.Crop> e : cropToRectMap.entrySet()) {
                if (e.getKey().contains(event.getPoint())) {
                    return e.getValue().name();
                }
            }
            for (Map.Entry<Rectangle, String> e : shipTooltipUnavailableActionMap.entrySet()) {
                if (e.getKey().contains(event.getPoint())) {
                    return "<html>Can't load on this ship. Reasons: <ul>" + e.getValue() + "</ul></html>";
                }
            }
            for (Map.Entry<Rectangle, Integer> e : shipToRectMap.entrySet()) {
                if (e.getKey().contains(event.getPoint())) {
                    return gs.getShip(e.getValue()).toString();
                }
            }
        }
        return super.getToolTipText(event);
    }

    @Override
    public void clearHighlights() {
        shipClicked = -1;
    }
}
