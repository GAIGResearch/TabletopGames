package games.puertorico.gui;

import games.puertorico.PRPlayerBoard;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.PuertoRicoParameters;
import games.puertorico.actions.*;
import games.puertorico.components.Building;
import games.puertorico.components.Plantation;
import games.puertorico.components.Ship;
import gui.IScreenHighlight;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static games.puertorico.gui.PRGUIUtils.*;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

public class PlayerBoard extends JComponent implements IScreenHighlight {
    PuertoRicoGameState gs;
    PuertoRicoGUI gui;
    PRPlayerBoard playerBoard;
    int playerId;
    Dimension size = new Dimension(250, 315);
    int borderPadY = 20, borderPadX = 5;
    int nPlantationSlots, nTownSlots, townGridWidth, townGridHeight;
    int[][] townGrid;
    Map<Rectangle, Building> buildingRectMap = new HashMap<>();
    Map<Rectangle, Plantation> plantationRectMap = new HashMap<>();
    Map<Rectangle, PuertoRicoConstants.Crop> cropRectMap = new HashMap<>();

    Building buildingClicked = null;
    Plantation plantationClicked = null;
    PuertoRicoConstants.Crop cropClicked = null;

    Map<Rectangle, String> invalidActionTooltipMap = new HashMap<>();

    public PlayerBoard(PuertoRicoGUI gui, PuertoRicoGameState gs, int playerId) {
        this.gs = gs;
        this.gui = gui;
        this.playerId = playerId;
        this.playerBoard = gs.getPlayerBoard(playerId);
        this.nPlantationSlots = ((PuertoRicoParameters)gs.getGameParameters()).plantationSlotsOnBoard;
        this.nTownSlots = ((PuertoRicoParameters)gs.getGameParameters()).townSlotsOnBoard;
        this.townGridWidth = ((PuertoRicoParameters)gs.getGameParameters()).townGridWidth;
        this.townGridHeight = ((PuertoRicoParameters)gs.getGameParameters()).townGridHeight;
        this.townGrid = new int[townGridHeight][townGridWidth];

        size = new Dimension(Math.max(townGridWidth * buildingWidth, nPlantationSlots * plantationSize / 2) + borderPadX*2, 315);

        ToolTipManager.sharedInstance().registerComponent(this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                buildingClicked = null;
                plantationClicked = null;
                cropClicked = null;
                int currentPlayer = gs.getCurrentPlayer();
                if (currentPlayer != playerId && !gui.getHumanPlayerIds().contains(playerId)) return;
                PuertoRicoConstants.Role currentRole = gs.getCurrentRole();

                for (Rectangle r : buildingRectMap.keySet()) {
                    if (r.contains(e.getPoint())) {
                        buildingClicked = buildingRectMap.get(r);
                        if (currentRole == PuertoRicoConstants.Role.MAYOR) {
                            if (buildingClicked.getOccupation() < buildingClicked.buildingType.capacity) {
                                gui.getAC().addAction(new OccupyBuilding(buildingClicked.buildingType));
                            }
                        }
                        break;
                    }
                }
                if (buildingClicked == null) {
                    for (Rectangle r : plantationRectMap.keySet()) {
                        if (r.contains(e.getPoint())) {
                            plantationClicked = plantationRectMap.get(r);
                            if (currentRole == PuertoRicoConstants.Role.MAYOR) {
                                if (!plantationClicked.isOccupied()) {
                                    gui.getAC().addAction(new OccupyPlantation(plantationClicked.crop));
                                }
                            }
                            break;
                        }
                    }
                    if (plantationClicked == null) {
                        for (Rectangle r : cropRectMap.keySet()) {
                            if (r.contains(e.getPoint())) {
                                cropClicked = cropRectMap.get(r);
                                if (currentRole == PuertoRicoConstants.Role.TRADER) {
                                    // Create action to sell good selected, if valid action
                                    boolean hasOffice = gs.hasActiveBuilding(playerId, PuertoRicoConstants.BuildingType.OFFICE);
                                    boolean hasAny = gs.getStoresOf(playerId, cropClicked) > 0;
                                    boolean notInMarket = !gs.getMarket().contains(cropClicked);

                                    if (hasAny && (hasOffice || notInMarket)) {
                                        int marketBonus = gs.hasActiveBuilding(playerId, PuertoRicoConstants.BuildingType.SMALL_MARKET) ? 1 : 0;
                                        marketBonus += gs.hasActiveBuilding(playerId, PuertoRicoConstants.BuildingType.LARGE_MARKET) ? 2 : 0;
                                        int bonus = marketBonus + (playerId == gs.getRoleOwner() ? 1 : 0);
                                        gui.getAC().addAction(new Sell(cropClicked, cropClicked.price + bonus));
                                    }
                                } else if (currentRole == PuertoRicoConstants.Role.CAPTAIN) {
                                    // Create action to ship good selected if ship selected in market
                                    if (gui.shipsAndMarket.shipClicked != -1) {
                                        Set<PuertoRicoConstants.Crop> loadedCrops = gs.getShips().stream()
                                                .map(Ship::getCurrentCargo)
                                                .filter(Objects::nonNull)
                                                .collect(Collectors.toSet());
                                        boolean emptyAndClickedUniqueCondition = gs.getShip(gui.shipsAndMarket.shipClicked).getCurrentCargo() == null
                                                && cropClicked != null && !loadedCrops.contains(cropClicked);
                                        if (emptyAndClickedUniqueCondition) {
                                            int amount = Math.min(gs.getStoresOf(playerId, cropClicked), gs.getShip(gui.shipsAndMarket.shipClicked).getAvailableCapacity());
                                            gui.getAC().addAction(new ShipCargo(cropClicked, gui.shipsAndMarket.shipClicked, amount));
                                        }
                                    }
                                } else //noinspection StatementWithEmptyBody
                                    if (currentRole == PuertoRicoConstants.Role.DISCARD) {
                                    // Handle this through buttons still, 2 options and we can't tell which of those is available from the game state
                                } else if (currentRole == PuertoRicoConstants.Role.CRAFTSMAN) {
                                    // Add action to gain crop selected, if can be produced by player
                                    Map<PuertoRicoConstants.Crop, Long> production = playerBoard.getPlantations().stream()
                                            .filter(Plantation::isOccupied)
                                            .map(p -> p.crop)
                                            .collect(groupingBy(c -> c, counting()));
                                    boolean playerProduces = production.get(cropClicked) != null && production.get(cropClicked) > 0;
                                    boolean inSupply = gs.getSupplyOf(cropClicked) > 0;
                                    if (playerProduces && inSupply) {
                                        gui.getAC().addAction(new GainCrop(cropClicked, 1));
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics gg) {
        buildingRectMap.clear();
        plantationRectMap.clear();
        cropRectMap.clear();
        invalidActionTooltipMap.clear();

        Graphics2D g = (Graphics2D) gg;

        boolean highlightCropStore = playerId == gs.getCurrentPlayer() && gui.getHumanPlayerIds().contains(playerId) &&
                (gs.getCurrentRole() == PuertoRicoConstants.Role.TRADER ||
                        gs.getCurrentRole() == PuertoRicoConstants.Role.CAPTAIN ||
                        gs.getCurrentRole() == PuertoRicoConstants.Role.DISCARD ||
                        gs.getCurrentRole() == PuertoRicoConstants.Role.CRAFTSMAN);
        boolean highlightPlantationOrBuilding = playerId == gs.getCurrentPlayer() && gui.getHumanPlayerIds().contains(playerId) &&
                gs.getCurrentRole() == PuertoRicoConstants.Role.MAYOR;

        int width = size.width-borderPadX*2;
        int height = size.height-borderPadY;
        g.drawRect(borderPadX, borderPadY, width, height);
        g.setFont(PRGUIUtils.textFontBold);
        FontMetrics fm = g.getFontMetrics();

        // VP, doubloons, # colonists, crops
        g.setColor(secondaryColor);
        g.drawString("VP: " + playerBoard.getVP(), pad + borderPadX, textFontSize + pad + borderPadY);
        g.setColor(Color.black);
        g.drawString("Doubloons: " + playerBoard.getDoubloons(), pad + borderPadX, textFontSize*2 + pad*2 + borderPadY);
        g.drawString("Colonists: " + playerBoard.getUnassignedColonists(), pad + borderPadX, textFontSize*3 + pad*3 + borderPadY);

        // Crops in supply
        int x = pad + borderPadX;
        int y = textFontSize*4 + pad*6 + borderPadY;
        for (PuertoRicoConstants.Crop crop: PuertoRicoConstants.Crop.values()) {
            if (crop == PuertoRicoConstants.Crop.QUARRY) continue;

            int n = playerBoard.getStoresOf(crop);
            String s1 = n + " ";
            g.drawString(s1, x, y);
            x += fm.stringWidth(s1);

            PRGUIUtils.drawBarrel(g, x, y-textFontSize, PRGUIUtils.cropColorMap.get(crop));
            cropRectMap.put(new Rectangle(x, y-textFontSize, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight), crop);

            if (cropClicked == crop) {
                Stroke s = g.getStroke();
                g.setStroke(new BasicStroke(2));
                g.setColor(PRGUIUtils.highlightColor);
                g.drawRect(x-pad, y-textFontSize-pad, PRGUIUtils.barrelWidth+pad*2, PRGUIUtils.barrelHeight+pad*2);
                g.setStroke(s);
            }

            if (highlightCropStore) {
                // For trader needs to be more than 0 and not already in market, or has office
                if (gs.getCurrentRole() == PuertoRicoConstants.Role.TRADER) {
                    boolean hasOffice = gs.hasActiveBuilding(playerId, PuertoRicoConstants.BuildingType.OFFICE);
                    boolean hasAny = gs.getStoresOf(playerId, crop) > 0;
                    boolean notInMarket = !gs.getMarket().contains(crop);

                    if (hasAny && (hasOffice || notInMarket)) {
                        g.setColor(PRGUIUtils.highlightColor);
                    } else {
                        g.setColor(Color.red);
                        if (!hasAny) {
                            invalidActionTooltipMap.put(new Rectangle(x, y-textFontSize, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight), "<li>No " + crop.name() + " to sell.</li>");
                        }
                        if (!hasOffice && !notInMarket) {
                            invalidActionTooltipMap.put(new Rectangle(x, y-textFontSize, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight), "<li>" + crop.name() + " is already in the market, and you do not have an office.");
                        }
                    }
                }
                else if (gs.getCurrentRole() == PuertoRicoConstants.Role.CRAFTSMAN) {
                    Map<PuertoRicoConstants.Crop, Long> production = playerBoard.getPlantations().stream()
                            .filter(Plantation::isOccupied)
                            .map(p -> p.crop)
                            .collect(groupingBy(c -> c, counting()));
                    boolean playerProduces = production.get(crop) != null && production.get(crop) > 0;
                    boolean inSupply = gs.getSupplyOf(crop) > 0;
                    if (playerProduces && inSupply) {
                        g.setColor(PRGUIUtils.highlightColor);
                    } else {
                        g.setColor(Color.red);
                        if (!playerProduces) {
                            invalidActionTooltipMap.put(new Rectangle(x, y-textFontSize, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight), "<li>You do not produce " + crop.name() + ".</li>");
                        }
                        if (!inSupply) {
                            invalidActionTooltipMap.put(new Rectangle(x, y-textFontSize, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight), "<li>There are no more " + crop.name() + " left in the supply.</li>");
                        }
                    }
                }
                else if (gs.getCurrentRole() == PuertoRicoConstants.Role.CAPTAIN) {
                    boolean hasAny = gs.getStoresOf(playerId, crop) > 0;

                    if (gui.shipsAndMarket.shipClicked != -1) {
                        // A ship is selected. If the ship had other stuff on it, we have no choice.
                        // So we assume this ship is empty. Need to load something that we have, and that is not already on another ship.
                        Set<PuertoRicoConstants.Crop> loadedCrops = gs.getShips().stream()
                                .map(Ship::getCurrentCargo)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                        boolean emptyAndClickedUniqueCondition = gs.getShip(gui.shipsAndMarket.shipClicked).getCurrentCargo() == null
                                && crop != null && !loadedCrops.contains(crop);
                        if (hasAny && emptyAndClickedUniqueCondition) {
                            g.setColor(PRGUIUtils.highlightColor);
                        } else {
                            g.setColor(Color.red);
                            if (crop != null) {
                                if (!hasAny) {
                                    invalidActionTooltipMap.put(new Rectangle(x, y - textFontSize, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight), "<li>You do not have any " + crop.name() + " to load.</li>");
                                }
                                if (!emptyAndClickedUniqueCondition) {
                                    invalidActionTooltipMap.put(new Rectangle(x, y - textFontSize, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight), "<li>You cannot load " + crop.name() + " onto the selected ship.</li>");
                                }
                            }
                        }
                    } else {
                        // No ship selected yet. This good is valid to load if there is an empty ship available (then we have the conditions above),
                        // Or if it matches a good on another ship that is not yet at capacity.
                        Set<PuertoRicoConstants.Crop> loadedCrops = gs.getShips().stream()
                                .map(Ship::getCurrentCargo)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                        boolean emptyShipAvailable = gs.getShips().stream()
                                .map(Ship::getCurrentCargo)
                                .noneMatch(Objects::nonNull);
                        boolean uniqueCondition = crop != null && !loadedCrops.contains(crop);
                        boolean matchesGoodOnAnotherShipWithCapacity = gs.getShips().stream()
                                .filter(s -> s.getAvailableCapacity() > 0)
                                .map(Ship::getCurrentCargo)
                                .filter(Objects::nonNull)
                                .anyMatch(c -> c == crop);
                        if (hasAny && (emptyShipAvailable && uniqueCondition || matchesGoodOnAnotherShipWithCapacity)) {
                            g.setColor(PRGUIUtils.highlightColor);
                        } else {
                            g.setColor(Color.red);
                            if (crop != null) {
                                if (!hasAny) {
                                    invalidActionTooltipMap.put(new Rectangle(x, y - textFontSize, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight), "<li>You do not have any " + crop.name() + " to load.</li>");
                                }
                                if (!uniqueCondition && !matchesGoodOnAnotherShipWithCapacity) {
                                    invalidActionTooltipMap.put(new Rectangle(x, y - textFontSize, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight), "<li>Ships carry " + crop.name() + " already, but those are full.</li>");
                                }
                            }
                        }
                    }
                }
                else if (gs.getCurrentRole() == PuertoRicoConstants.Role.DISCARD) {
                    boolean hasAny = gs.getStoresOf(playerId, crop) > 0;
                    if (hasAny) {
                        g.setColor(PRGUIUtils.highlightColor);
                    } else {
                        g.setColor(Color.red);
                        invalidActionTooltipMap.put(new Rectangle(x, y-textFontSize, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight), "<li>You do not have any " + crop.name() + " to store.</li>");
                    }
                }

                Stroke s = g.getStroke();
                g.setStroke(new BasicStroke(2));
                g.drawRect(x, y-textFontSize, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight);
                g.setColor(Color.black);
                g.setStroke(s);
            }

            x += PRGUIUtils.barrelWidth + PRGUIUtils.pad*2;
            g.setColor(Color.black);
        }

        // Buildings

        // Adjust building slot width and height if needed
        if (width / townGridWidth < buildingWidth) {
            buildingWidth = width / townGridWidth;
        }
        if (height / townGridHeight < buildingHeight) {
            buildingHeight = height / townGridHeight;
        }

        TreeMap<Integer, List<Integer>> sizeToBuildings = new TreeMap<>();
        for (int i = 0; i < playerBoard.getBuildings().size(); i++) {
            Building b = playerBoard.getBuildings().get(i);
            if (!sizeToBuildings.containsKey(b.buildingType.size)) {
                sizeToBuildings.put(b.buildingType.size, new ArrayList<>());
            }
            sizeToBuildings.get(b.buildingType.size).add(i);
        }

        // Place buildings in town grid, starting from the largest
        for (int[] row: townGrid) {
            Arrays.fill(row, -1);
        }
        int i = 0;
        int j = 0;
        for (int size: sizeToBuildings.descendingKeySet()) {
            List<Integer> buildings = sizeToBuildings.get(size);
            for (int b: buildings) {
                // Find an empty spot to start drawing this building from
                boolean fail = false;
                while (townGrid[i][j] != -1) {
                    j++;
                    if (j % townGridWidth == 0) {
                        i++;
                        j = 0;
                    }
                    if (i >= townGridHeight) {
                        fail = true;
                        break;
                    }
                }
                if (fail) {
                    System.out.println("fail");
                    break;
                }
                for (int k = 0; k < size; k++) {
                    townGrid[i+k][j] = b;
                }
                j++;
                if (j % townGridWidth == 0) {
                    i += size;
                    j = 0;
                }
            }
        }

        // Draw the town grid
        y = textFontSize*4 + pad*6 + barrelHeight + borderPadY;
        for (i = 0; i < townGridHeight; i++) {
            x = borderPadX;
            for (j = 0; j < townGridWidth; j++) {
                if (townGrid[i][j] != -1 && i > 0 && townGrid[i-1][j] == townGrid[i][j]) {
                    // Skip this square
                    x += buildingWidth;
                    continue;
                }
                Building b = null;
                int h = buildingHeight;
                if (townGrid[i][j] != -1) {
                    b = playerBoard.getBuildings().get(townGrid[i][j]);
                    h *= b.buildingType.size;
                }
                drawBuilding(g, b, x, y, 0);
                buildingRectMap.put(new Rectangle(x, y, buildingWidth, h), b);

                if (highlightPlantationOrBuilding && b != null) {
                    Stroke s = g.getStroke();
                    g.setStroke(new BasicStroke(3));
                    if (b.getOccupation() < b.buildingType.capacity) {
                        g.setColor(PRGUIUtils.highlightColor);
                    } else {
                        g.setColor(Color.red);
                        invalidActionTooltipMap.put(new Rectangle(x, y, buildingWidth, h), "<li>Can't place any more colonists, building is full.</li>");
                    }
                    g.drawRect(x, y, buildingWidth, h);
                    g.setStroke(s);
                    g.setColor(Color.black);
                }

                x += buildingWidth;
            }
            y += buildingHeight;
        }

        // Plantations
        y += pad - plantationSize;
        int nColsP = width / plantationSize;
        x = borderPadX;
        for (i = 0; i < nPlantationSlots; i++) {
            if (i % nColsP == 0) {
                x = borderPadX;
                y += plantationSize;
            }
            Plantation p = null;
            if (i < playerBoard.getPlantations().size()) {
                p = playerBoard.getPlantations().get(i);
            }
            drawPlantation(g, p, x, y);
            plantationRectMap.put(new Rectangle(x, y, plantationSize, plantationSize), p);


            if (highlightPlantationOrBuilding && p != null) {
                Stroke s = g.getStroke();
                g.setStroke(new BasicStroke(3));
                if (!p.isOccupied()) {
                    g.setColor(PRGUIUtils.highlightColor);
                } else {
                    g.setColor(Color.red);
                    invalidActionTooltipMap.put(new Rectangle(x, y, plantationSize, plantationSize), "<li>Can't place a colonist here, plantation is already occupied.</li>");
                }
                g.drawRect(x, y, plantationSize, plantationSize);
                g.setStroke(s);
                g.setColor(Color.black);
            }

            x += plantationSize;
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (showTooltips) {
            for (Map.Entry<Rectangle, String> e : invalidActionTooltipMap.entrySet()) {
                if (e.getKey().contains(event.getPoint())) {
                    return "<html>Invalid action. Reasons: <ul>" + e.getValue() + "</ul></html>";
                }
            }
            for (Rectangle r : buildingRectMap.keySet()) {
                if (r.contains(event.getPoint())) {
                    Building b = buildingRectMap.get(r);
                    if (b != null) {
                        return b.buildingType.tooltip + " (" + b.getOccupation() + " filled)";
                    }
                }
            }
            for (Rectangle r : plantationRectMap.keySet()) {
                if (r.contains(event.getPoint())) {
                    Plantation p = plantationRectMap.get(r);
                    if (p != null) {
                        return p.crop.name() + (p.isOccupied() ? " (producing)" : " (unoccupied)");
                    }
                }
            }
            for (Rectangle r : cropRectMap.keySet()) {
                if (r.contains(event.getPoint())) {
                    PuertoRicoConstants.Crop crop = cropRectMap.get(r);
                    return crop.name() + " (" + playerBoard.getStoresOf(crop) + ")";
                }
            }
        }
        return super.getToolTipText(event);
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
    public void clearHighlights() {
        buildingClicked = null;
        plantationClicked = null;
        cropClicked = null;
    }
}
