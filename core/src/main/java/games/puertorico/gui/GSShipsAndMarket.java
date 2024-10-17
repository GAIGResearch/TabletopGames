package games.puertorico.gui;

import games.puertorico.actions.ShipCargo;
import games.puertorico.components.Ship;
import gui.IScreenHighlight;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.PuertoRicoParameters;
import utilities.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

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
        this.nCols = marketCapacity / PRGUIUtils.nSpacesOnLine;
        this.marketWidth = Math.max((nCols+1)* PRGUIUtils.shipSpaceSize, (PuertoRicoConstants.Crop.values().length-1) * (PRGUIUtils.barrelWidth + PRGUIUtils.pad*4) + PRGUIUtils.pad*2);
        this.marketHeight = (PRGUIUtils.nSpacesOnLine+1) * PRGUIUtils.shipSpaceSize + PRGUIUtils.barrelHeight + PRGUIUtils.textFontSize;
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
        int startX = PRGUIUtils.pad;
        for (int s = 0; s < gs.getShips().size(); s++) {
            Ship ship = gs.getShip(s);
            Pair<Integer, Integer> shipSize = PRGUIUtils.drawShip((Graphics2D) g, ship, startX, PRGUIUtils.pad);
            shipToRectMap.put(new Rectangle(startX, PRGUIUtils.pad, shipSize.a, shipSize.b), s);

            if (gs.getCurrentRole() == PuertoRicoConstants.Role.CAPTAIN && gui.getHumanPlayerId().contains(currentPlayer)) {
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
                    g.setColor(PRGUIUtils.highlightColor);
                    g.drawRect(startX + PRGUIUtils.pad, PRGUIUtils.pad*2, shipSize.a- PRGUIUtils.pad*2, shipSize.b- PRGUIUtils.pad*2);
                }

                if (capacityCondition && (emptyAndUniqueCondition || matchingCargoCondition)) {
                    // Draw shadow fillers according to player's amount that would be put on the ship
                    PuertoRicoConstants.Crop crop;
                    if (emptyAndUniqueCondition) {
                        crop = selectedCrop;
                    }
                    else crop = ship.getCurrentCargo();
                    int amount = Math.min(gs.getStoresOf(gs.getCurrentPlayer(), crop), ship.getAvailableCapacity());
                    PRGUIUtils.drawShip((Graphics2D) g, ship, startX, PRGUIUtils.pad, crop, ship.getSpacesFilled() + amount);

                    g.setColor(PRGUIUtils.highlightColor);
                    g.drawRect(startX, PRGUIUtils.pad, shipSize.a, shipSize.b);

                } else {
                    g.setColor(Color.red);
                    g.drawRect(startX, PRGUIUtils.pad, shipSize.a, shipSize.b);
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
                    shipTooltipUnavailableActionMap.put(new Rectangle(startX, PRGUIUtils.pad, shipSize.a, shipSize.b), reason);
                }
            }

            startX += shipSize.a;
        }
        startX += PRGUIUtils.pad*3;

        // Market
        // General container
        g.setColor(PRGUIUtils.shipColor);
        g.fillRect(startX, PRGUIUtils.pad, marketWidth, marketHeight);
        if (PRGUIUtils.outline) {
            g.setColor(Color.black);
            g.drawRect(startX, PRGUIUtils.pad, marketWidth, marketHeight);
        }

        // Title
        g.setFont(PRGUIUtils.textFontBold);
        g.setColor(Color.black);
        g.drawString("Trading House", startX + PRGUIUtils.pad, PRGUIUtils.pad + PRGUIUtils.textFontSize);

        // Spaces in market
        startX += PRGUIUtils.shipSpaceSize/2;
        int x = startX;
        int y = PRGUIUtils.pad + PRGUIUtils.shipSpaceSize/2 - PRGUIUtils.shipSpaceSize + PRGUIUtils.textFontSize;
        List<PuertoRicoConstants.Crop> market = gs.getMarket();
        for (int i = 0; i < marketCapacity; i++) {
            if (i % nCols == 0) {
                y += PRGUIUtils.shipSpaceSize;
                x = startX;
            }
            // Draw space
            g.setColor(PRGUIUtils.spaceColor);
            g.fillRect(x, y, PRGUIUtils.shipSpaceSize, PRGUIUtils.shipSpaceSize);
            if (PRGUIUtils.outline) {
                g.setColor(Color.black);
                g.drawRect(x, y, PRGUIUtils.shipSpaceSize, PRGUIUtils.shipSpaceSize);
            }
            // Draw crop
            if (i < market.size()) {
                PRGUIUtils.drawBarrel((Graphics2D) g, x + PRGUIUtils.shipSpaceSize/2 - PRGUIUtils.barrelWidth/2, y + PRGUIUtils.shipSpaceSize/2 - PRGUIUtils.barrelHeight/2, PRGUIUtils.cropColorMap.get(market.get(i)));
                cropToRectMap.put(new Rectangle(x + PRGUIUtils.shipSpaceSize/2 - PRGUIUtils.barrelWidth/2, y + PRGUIUtils.shipSpaceSize/2 - PRGUIUtils.barrelHeight/2, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight), market.get(i));
            }
            x += PRGUIUtils.shipSpaceSize;
        }

        // Point values
        x = startX;
        y += PRGUIUtils.shipSpaceSize + PRGUIUtils.textFontSize + PRGUIUtils.pad;
        g.setFont(PRGUIUtils.textFontBold);
        for (PuertoRicoConstants.Crop crop: PuertoRicoConstants.Crop.values()) {
            if (crop == PuertoRicoConstants.Crop.QUARRY) continue;
            PRGUIUtils.drawBarrel((Graphics2D) g, x, y- PRGUIUtils.textFontSize, PRGUIUtils.cropColorMap.get(crop));
            cropToRectMap.put(new Rectangle(x, y- PRGUIUtils.textFontSize, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight), crop);
            x += PRGUIUtils.barrelWidth;
            String s1 = "=" + crop.price;
            g.setColor(Color.black);
            g.drawString(s1, x, y);
            x += PRGUIUtils.pad*4;
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
        if (PRGUIUtils.showTooltips) {
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
