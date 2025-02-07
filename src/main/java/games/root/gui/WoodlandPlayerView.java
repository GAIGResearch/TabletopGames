package games.root.gui;

import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.Item;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static games.root.gui.RootGUIManager.*;
import static games.root.gui.RootGUIManager.cardHeight;

public class WoodlandPlayerView extends JComponent {
    RootDeckView playerHand;
    RootDeckView craftedCards;
    RootDeckView supporters;
    String dataPath;
    int width, height;
    int humanID;
    int playerID;
    int warriors;
    int officers;
    int sympathyTokens;

    int MouseBase;
    int FoxBase;
    int RabbitBase;
    List<String> craftedItems = new ArrayList<>();
    int border = 5;
    int textHeight = 15;
    int handWidth = playerAreaWidth - cardWidth - border * 2;

    public WoodlandPlayerView(int playerID, int humanID, String dataPath, RootGameState state){
        this.dataPath = dataPath;
        this.humanID = humanID;
        this.playerID = playerID;
        this.width = playerAreaWidth + border * 2;
        this.height = playerAreaHeight + textHeight + border;
        playerHand = new RootDeckView(humanID, state.getPlayerHand(playerID), false, dataPath, new Rectangle(0,0,handWidth,cardHeight));
        craftedCards = new RootDeckView(humanID, state.getPlayerCraftedCards(playerID), true, dataPath, new Rectangle(0,0,handWidth/2,cardHeight));
        supporters = new RootDeckView(humanID, state.getSupporters(), false, dataPath, new Rectangle(0,0,handWidth/2, cardHeight));
        JPanel supporterAndCrafter = new JPanel();
        supporterAndCrafter.setLayout(new GridLayout(1,2));
        supporterAndCrafter.add(craftedCards);
        supporterAndCrafter.add(supporters);
        this.setLayout(new GridLayout(3,1));
        this.add(playerHand);
        this.add(supporterAndCrafter);
    }

    protected void update(RootGameState state){
        warriors = state.getWoodlandWarriors();
        sympathyTokens = state.getSympathyTokens();
        officers = state.getOfficers();
        craftedItems.clear();
        for (Item item: state.getPlayerCraftedItems(playerID)){
            craftedItems.add(dataPath + item.itemType.toString().toLowerCase());
        }
        FoxBase = state.getBuildingCount(RootParameters.BuildingType.FoxBase);
        MouseBase = state.getBuildingCount(RootParameters.BuildingType.MouseBase);
        RabbitBase = state.getBuildingCount(RootParameters.BuildingType.RabbitBase);


    }

    @Override
    protected void paintComponent(Graphics g){
        g.setFont(Font.getFont(Font.SANS_SERIF));
        g.setColor(Color.BLACK);
        g.drawString("warriors: " + warriors + "    officers: " + officers, 15,260);
        int xSympathy = 15;
        g.setColor(new Color(0, 153, 0));
        for (int i =0; i < sympathyTokens; i++){
            g.fillOval(xSympathy,265,20,20);
            xSympathy += 22;
        }
        g.setColor(new Color(255, 178, 102));
        if (MouseBase == 1){
            g.fillOval(15,285,20,20);
        }else {
            g.drawOval(15,285,20,20);
        }
        g.setColor(new Color(255, 255, 102));
        if (RabbitBase == 1){
            g.fillOval(37,285,20,20);
        }else{
            g.drawOval(37,285,20,20);
        }
        g.setColor(new Color(255, 128, 128));
        if(FoxBase == 1){
            g.fillOval(59,285,20,20);
        }else{
            g.drawOval(59,285,20,20);
        }
        g.setColor(Color.black);
        g.drawString("Crafted Items", 240, 260);
        int x = 240;
        int y = 260;
        for (String string: craftedItems){
            g.drawImage(ImageIO.GetInstance().getImage(string  + ".png"),x,y,30,30,null,null);
            x+=30;
        }
    }

    @Override
    public Dimension getPreferredSize(){return new Dimension(width-20, height-25);}
}
