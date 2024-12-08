package games.puertorico.gui;

import games.puertorico.PRPlayerBoard;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.PuertoRicoParameters;
import games.puertorico.actions.Build;
import games.puertorico.components.Building;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import static games.puertorico.gui.PRGUIUtils.*;
import static java.util.stream.Collectors.toSet;

public class GSBuildings extends JComponent {
    PuertoRicoGameState gs;
    PuertoRicoGUI gui;
    Set<Integer> discounts = PuertoRicoConstants.BuildingType.getQuarryDiscounts();
    Dimension size = new Dimension(pad*2+(buildingWidth + pad)*(discounts.size()), 300);
    Map<Rectangle, Building> buildingRectMap = new HashMap<>();
    Map<Rectangle, String> buildingTooltipUnavailableActionMap = new HashMap<>();

    public GSBuildings(PuertoRicoGUI gui, PuertoRicoGameState gs) {
        this.gs = gs;
        this.gui = gui;
        ToolTipManager.sharedInstance().registerComponent(this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Building clicked = null;
                for (Rectangle r : buildingRectMap.keySet()) {
                    if (r.contains(e.getPoint())) {
                        clicked = buildingRectMap.get(r);
                        break;
                    }
                }
                if (clicked != null && gs.getCurrentRole() == PuertoRicoConstants.Role.BUILDER && gui.getHumanPlayerIds().contains(gs.getCurrentPlayer())) {
                    // Clicked on a building when it's our choice of building, check if the clicked building is a legal action

                    PuertoRicoParameters params = (PuertoRicoParameters) gs.getGameParameters();
                    int currentPlayer = gs.getCurrentPlayer();
                    int budget = gs.getDoubloons(currentPlayer);
                    PRPlayerBoard playerBoard = gs.getPlayerBoard(currentPlayer);
                    int numberOfQuarries = (int) playerBoard.getPlantations().stream()
                            .filter(p -> p.crop == PuertoRicoConstants.Crop.QUARRY && p.isOccupied()).count();
                    int roleDiscount = gs.getRoleOwner() == currentPlayer ? 1 : 0;
                    // we look for all the buildings that the player can afford, and which are available and which we have space for
                    int townSize = playerBoard.getTownSize();
                    if (townSize == params.townSlotsOnBoard) {
                        return;
                    }
                    Set<PuertoRicoConstants.BuildingType> currentBuildings = playerBoard
                            .getBuildings().stream().map(b -> b.buildingType).collect(toSet());
                    int currentLargeBuildings = (int) playerBoard.getBuildings().stream()
                            .filter(b -> b.buildingType.size == 2).count();

                    PuertoRicoConstants.BuildingType b = clicked.buildingType;
                    if (b.cost <= budget + roleDiscount + Math.min(numberOfQuarries, b.nMaxQuarryDiscount) &&
                        townSize + b.size <= params.townSlotsOnBoard &&
                        !currentBuildings.contains(b) &&
                        (b.size == 1 || currentLargeBuildings < params.townGridWidth)) {

                        gui.getAC().addAction(new Build(b, Math.max(0, b.cost - roleDiscount - Math.min(numberOfQuarries, b.vp))));
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics gg) {
        Graphics2D g = (Graphics2D) gg;
        buildingTooltipUnavailableActionMap.clear();

        PuertoRicoParameters params = (PuertoRicoParameters) gs.getGameParameters();
        int currentPlayer = gs.getCurrentPlayer();
        int budget = gs.getDoubloons(currentPlayer);
        PRPlayerBoard playerBoard = gs.getPlayerBoard(currentPlayer);
        int numberOfQuarries = (int) playerBoard.getPlantations().stream()
                .filter(p -> p.crop == PuertoRicoConstants.Crop.QUARRY && p.isOccupied()).count();
        int roleDiscount = gs.getRoleOwner() == currentPlayer ? 1 : 0;
        // we look for all the buildings that the player can afford, and which are available and which we have space for
        int townSize = playerBoard.getTownSize();
        Set<PuertoRicoConstants.BuildingType> currentBuildings = playerBoard
                .getBuildings().stream().map(b -> b.buildingType).collect(toSet());
        int currentLargeBuildings = (int) playerBoard.getBuildings().stream()
                .filter(b -> b.buildingType.size == 2).count();

        // Buildings available for purchase
        int col = 0;
        FontMetrics fm = g.getFontMetrics();
        for (int discount : discounts) {
            int x = pad + col * (buildingWidth + pad);
            List<PuertoRicoConstants.BuildingType> buildings = PuertoRicoConstants.BuildingType.getBuildingTypesWithQuarryDiscount(discount);
            int y = pad;

            // Draw discount at top of column
            g.setColor(secondaryColor);
            g.setFont(PRGUIUtils.textFontBold);
            g.drawString(String.valueOf(discount), x + buildingWidth/2 - fm.stringWidth(String.valueOf(discount))/2, y + textFontSize);
            g.setColor(Color.black);
            y += textFontSize + pad;

            // Draw buildings
            for (PuertoRicoConstants.BuildingType buildingType: buildings) {
                int nAvailable = gs.getBuildingsOfType(buildingType);
                Building b = null;
                if (nAvailable > 0) b = buildingType.instantiate();
                drawBuilding(g, b, x, y, nAvailable);
                int size = 1;
                if (b != null) size = b.buildingType.size;
                buildingRectMap.put(new Rectangle(x, y, buildingWidth, buildingHeight * size), b);

                // Highlight buildings that can be built by current player
                if (gs.getCurrentRole() == PuertoRicoConstants.Role.BUILDER && gui.getHumanPlayerIds().contains(gs.getCurrentPlayer())) {
                    Stroke s = g.getStroke();
                    g.setStroke(new BasicStroke(3));
                    if (b != null) {
                        boolean townSizeCondition = townSize < params.townSlotsOnBoard;
                        boolean costCondition = b.buildingType.cost <= budget + roleDiscount + Math.min(numberOfQuarries, b.buildingType.nMaxQuarryDiscount);
                        boolean fitsOnBoardCondition = townSize + b.buildingType.size <= params.townSlotsOnBoard && (b.buildingType.size == 1 || currentLargeBuildings < params.townGridWidth);
                        boolean uniqueCondition = !currentBuildings.contains(b.buildingType);

                        if (townSizeCondition && costCondition && fitsOnBoardCondition && uniqueCondition) {
                            g.setColor(highlightColor);
                        } else {
                            g.setColor(Color.red);
                            String tooltip = "";
                            if (!townSizeCondition) tooltip += "<li>Town is full</li>";
                            if (!costCondition) tooltip += "<li>Not enough money</li>";
                            if (!fitsOnBoardCondition) tooltip += "<li>Not enough space</li>";
                            if (!uniqueCondition) tooltip += "<li>Already built</li>";
                            buildingTooltipUnavailableActionMap.put(new Rectangle(x, y, buildingWidth, buildingHeight * size), tooltip);
                        }
                        g.drawRect(x, y, buildingWidth, buildingHeight * size);
                    }
                    g.setStroke(s);
                }

                y += buildingHeight;
            }
            col++;
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
            for (Rectangle r : buildingRectMap.keySet()) {
                if (r.contains(event.getPoint())) {
                    Building b = buildingRectMap.get(r);
                    if (b != null) {
                        if (buildingTooltipUnavailableActionMap.containsKey(r))
                            return "<html><p>" + b.buildingType.tooltip + "</p><hr>Cannot build because:<ul>"
                                    + buildingTooltipUnavailableActionMap.get(r) + "</ul></html>";
                        return b.buildingType.tooltip;
                    }
                }
            }
        }
        return super.getToolTipText(event);
    }
}
