package games.root.gui;
import java.util.List;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.Item;
import utilities.ImageIO;

import javax.swing.*;

import java.awt.*;
import java.util.ArrayList;

import static games.root.gui.RootGUIManager.*;

public class MarquisePlayerView extends JComponent {
    RootDeckView playerHand;
    RootDeckView craftedCards;

    String dataPath;

    int width, height;
    int humanID;
    int playerID;
    int wood;
    int recruiters;
    int workshops;
    int sawmills;
    int warriors;

    private List<String> craftedItems = new ArrayList<>();

    int border = 5;
    int textHeight = 15;
    int handWidth = playerAreaWidth - cardWidth - border * 2;

    public MarquisePlayerView(int playerID, int humanID, String dataPath, RootGameState state){
        this.dataPath = dataPath;
        this.humanID = humanID;
        this.playerID = playerID;
        this.width = playerAreaWidth + border * 2;
        this.height = playerAreaHeight + textHeight + border;
        playerHand = new RootDeckView(humanID, state.getPlayerHand(playerID), false, dataPath, new Rectangle(0,0,handWidth,cardHeight));
        craftedCards = new RootDeckView(humanID, state.getPlayerCraftedCards(playerID), true, dataPath, new Rectangle(0,0,handWidth,cardHeight));
        this.setLayout(new GridLayout(3,1));
        this.add(playerHand);
        this.add(craftedCards);

    }

    @Override
    protected void paintComponent(Graphics g){
        g.setFont(Font.getFont(Font.SANS_SERIF));
        g.setColor(Color.BLACK);
        g.drawString("warriors: " + warriors, 15,230);
        int xWood = 15;
        g.setColor(new Color(88,57,39));
        for (int i =0; i < wood; i++){
            g.fillOval(xWood,236,20,20);
            xWood += 22;
        }
        int xRecruiter = 15;
        for (int i =0; i< recruiters; i++){
            g.setColor(new Color(255, 128, 0));
            g.fillOval(xRecruiter, 258,20,20);
            g.setColor(Color.BLACK);
            g.drawString("R",xRecruiter+6, 272);
            xRecruiter += 22;
        }
        int xWorkshop = 15;
        for (int i =0; i< workshops; i++){
            g.setColor(new Color(255, 128, 0));
            g.fillOval(xWorkshop, 280,20,20);
            g.setColor(Color.BLACK);
            g.drawString("W",xWorkshop+6, 294);
            xWorkshop += 22;
        }
        int xSawmill = 15;
        for (int i =0; i< sawmills; i++){
            g.setColor(new Color(255, 128, 0));
            g.fillOval(xSawmill, 302,20,20);
            g.setColor(Color.BLACK);
            g.drawString("S",xSawmill+6, 316);
            xSawmill += 22;
        }
        g.drawString("Crafted Items", 210, 230);
        int x = 210;
        int y = 240;
        for (String string: craftedItems){
            g.drawImage(ImageIO.GetInstance().getImage(string  + ".png"),x,y,30,30,null,null);
            x+=30;
        }
    }

    protected void update(RootGameState state){
        warriors = state.getCatWarriors();
        wood = state.getWood();
        sawmills = state.getBuildingCount(RootParameters.BuildingType.Sawmill);
        workshops = state.getBuildingCount(RootParameters.BuildingType.Workshop);
        recruiters = state.getBuildingCount(RootParameters.BuildingType.Recruiter);
        craftedItems.clear();
        for (Item item: state.getPlayerCraftedItems(playerID)){
            craftedItems.add(dataPath + item.itemType.toString().toLowerCase());
        }

    }

    @Override
    public Dimension getPreferredSize(){return new Dimension(width-20, height-25);}
}
