package games.root.gui;

import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.RootBoardNodeWithRootEdges;
import games.root.components.RootEdge;
import games.root.components.RootGraphBoard;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class MapPanel extends JComponent {
    RootGameState gameState;
    private final Image backgroundImage;

    public MapPanel(RootGameState gameState){
        this.gameState = gameState;
        backgroundImage = ImageIO.GetInstance().getImage("data/root/img_1.png");
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        drawBackgroundImage(g);
        drawGraph(g, gameState);
    }

    private void drawBackgroundImage(Graphics g) {
        if (backgroundImage != null) {
            drawImage(g, backgroundImage, 0, 0, 800, 700);
        } else {
//            System.out.println("NO IMAGE");
        }
    }

    private void drawGraph(Graphics g, RootGameState gameState) {
        if (gameState.getGameMap() == null) return;

        RootGraphBoard graphBoard = gameState.getGameMap();
        for (RootBoardNodeWithRootEdges node : graphBoard.getBoardNodes()) {
            int x = node.getX();
            int y = node.getY();
            for (RootEdge edge : node.getRootEdges()) {
                RootBoardNodeWithRootEdges neighbour = node.getNeighbour(edge);
                int neighbourX = neighbour.getX();
                int neighbourY = neighbour.getY();
                g.setColor(new Color(51, 150, 51));
                g.drawLine(x, y, neighbourX, neighbourY);
            }
        }


        for (RootBoardNodeWithRootEdges node : graphBoard.getBoardNodes()) {
            matchClearingColour(g, node.getClearingType());
            int x = node.getX();
            int y = node.getY();
            g.fillOval(x - 30, y - 30, 60, 60);
            g.setColor(Color.white);
            g.fillRect(x - 40, y - 48, 80, 15);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Old English Text MT", Font.PLAIN, 16));
            g.drawString(node.identifier, x - node.identifier.length() * 4, y - 35);
            matchClearingRulerColour(g, gameState.getPlayerFaction(node.rulerID));
            g.drawOval(x - 31, y - 31, 62, 62);
            g.drawOval(x - 32, y - 32, 64, 64);

            int currentX = x - 30;
            int curerntY = y + 8;
            for (RootParameters.Factions faction : RootParameters.Factions.values()) {
                matchClearingRulerColour(g, faction);
                if (node.getWarrior(faction) > 0) {
                    g.fillOval(currentX, curerntY, 15, 10);
                    g.setColor(Color.BLACK);
                    g.drawString("" + node.getWarrior(faction), currentX + 2, curerntY);
                } else {
                    g.drawOval(currentX, curerntY, 15, 10);
                }
                currentX += 15;
            }
            currentX = x;
            curerntY = y - 15;
            HashMap<RootParameters.BuildingType, Integer> buildingsToDraw = node.getAllBuildings();
            for (int i = 0; i < node.getMaxBuildings(); i++) {
                if (i == 0) {
                    currentX -= 8;
                    currentX -= 10 * (node.getMaxBuildings() - 1);
                }

                for (RootParameters.BuildingType bt : buildingsToDraw.keySet()) {
                    if (buildingsToDraw.get(bt) > 0) {
                        g.setFont(new Font("Arial", Font.PLAIN, 10));
                        drawBuilding(g, bt, currentX, curerntY);
                        buildingsToDraw.put(bt, buildingsToDraw.get(bt) - 1);
                        break;
                    }
                }
                g.setColor(Color.BLACK);
                g.drawRect(currentX, curerntY, 16, 10);
                currentX += 20;
            }
            HashMap<RootParameters.TokenType, Integer> tokensToDraw = node.getAllTokens();
            g.setFont(new Font("Arial", Font.PLAIN, 10));
            if (tokensToDraw.get(RootParameters.TokenType.Keep) == 1){
                matchClearingRulerColour(g, RootParameters.Factions.MarquiseDeCat);
                g.fillOval(x-40,y-20,15,15);
                g.setColor(Color.BLACK);
                g.drawOval(x-40,y-20,15,15);
                g.drawString("K", x-35,y-10);
            }
            if (tokensToDraw.get(RootParameters.TokenType.Sympathy) == 1){
                matchClearingRulerColour(g, RootParameters.Factions.WoodlandAlliance);
                g.fillOval(x+25,y-20,15,15);
                g.setColor(Color.BLACK);
                g.drawOval(x+25,y-20,15,15);
                g.drawString("S", x+30,y+-10);
            }
            if (tokensToDraw.get(RootParameters.TokenType.Wood) > 0){
                matchClearingRulerColour(g, RootParameters.Factions.MarquiseDeCat);
                g.fillOval(x+25,y-5,15,15);
                g.setColor(Color.BLACK);
                g.drawOval(x+25,y-5,15,15);
                g.drawString(""+tokensToDraw.get(RootParameters.TokenType.Wood), x+30,y+5);
            }
        }
    }

    private void drawBuilding(Graphics g, RootParameters.BuildingType bt, int x, int y) {
        switch (bt) {
            case Ruins:
                g.setColor(Color.darkGray);
                g.fillRect(x, y, 16, 10);
                g.setColor(Color.BLACK);
                g.drawString("R", x + 5, y+8);
                break;
            case Roost:
                matchClearingRulerColour(g, RootParameters.Factions.EyrieDynasties);
                g.fillRect(x, y, 16, 10);
                g.setColor(Color.BLACK);
                g.drawString("R", x + 5, y + 8);
                break;
            case Sawmill:
                matchClearingRulerColour(g, RootParameters.Factions.MarquiseDeCat);
                g.fillRect(x, y, 16, 10);
                g.setColor(Color.BLACK);
                g.drawString("S", x + 5, y + 8);
                break;
            case Workshop:
                matchClearingRulerColour(g, RootParameters.Factions.MarquiseDeCat);
                g.fillRect(x, y, 16, 10);
                g.setColor(Color.BLACK);
                g.drawString("W", x + 5, y + 8);
                break;
            case Recruiter:
                matchClearingRulerColour(g, RootParameters.Factions.MarquiseDeCat);
                g.fillRect(x, y, 16, 10);
                g.setColor(Color.BLACK);
                g.drawString("R", x + 5, y + 8);
                break;
            case RabbitBase:
                matchClearingRulerColour(g, RootParameters.Factions.WoodlandAlliance);
                g.fillRect(x, y, 16, 10);
                g.setColor(Color.BLACK);
                g.drawString("RB", x + 5, y + 8);
                break;
            case MouseBase:
                matchClearingRulerColour(g, RootParameters.Factions.WoodlandAlliance);
                g.fillRect(x, y, 16, 10);
                g.setColor(Color.BLACK);
                g.drawString("MB", x + 5, y + 8);
                break;
            case FoxBase:
                matchClearingRulerColour(g, RootParameters.Factions.WoodlandAlliance);
                g.fillRect(x, y, 16, 10);
                g.setColor(Color.BLACK);
                g.drawString("FB", x + 5, y + 8);
                break;

        }
    }

    private void matchClearingColour(Graphics g, RootParameters.ClearingTypes t) {
        switch (t) {
            case Mouse:
                g.setColor(new Color(255, 178, 102));
                break;
            case Fox:
                g.setColor(new Color(255, 128, 128));
                break;
            case Rabbit:
                g.setColor(new Color(255, 255, 102));
                break;
            case Forrest:
                g.setColor(new Color(51, 128, 51));
                break;
        }
    }

    private void matchClearingRulerColour(Graphics g, RootParameters.Factions faction) {
        if (faction == null) {
            g.setColor(Color.BLACK);
        } else {
            switch (faction) {
                case MarquiseDeCat:
                    g.setColor(new Color(255, 128, 0));
                    break;
                case EyrieDynasties:
                    g.setColor(new Color(0, 128, 255));
                    break;
                case WoodlandAlliance:
                    g.setColor(new Color(0, 153, 0));
                    break;
                case Vagabond:
                    g.setColor(Color.gray);
                    break;
                default:
                    g.setColor(Color.white);
                    break;
            }
        }
    }

    private void drawImage(Graphics g, Image img, int x, int y, int width, int height) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        double scaleW = width * 1.0 / w;
        double scaleH = height * 1.0 / h;
        g.drawImage(img, x, y, (int) (w * scaleW), (int) (h * scaleH), null);
    }

}
