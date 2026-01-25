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

public class VagabondPlayerView extends JComponent {
    RootDeckView playerHand;

    String dataPath;
    RootDeckView craftedCards;
    int width, height;
    int humanID;
    int playerID;

    int FoxQuests;
    int MouseQuests;
    int RabbitQuests;

    String relationship = "";

    List<String> ready = new ArrayList<>();
    List<String> exhausted = new ArrayList<>();
    List<String> damaged = new ArrayList<>();
    List<String> damagedExhausted = new ArrayList<>();
    List<String> coins = new ArrayList<>();
    List<String> bags = new ArrayList<>();
    List<String> teas = new ArrayList<>();
    String vagabondCharacter;
    int border = 5;
    int textHeight = 15;
    int handWidth = playerAreaWidth - cardWidth - border * 2;
    public VagabondPlayerView(int playerID, int humanID, String dataPath, RootGameState state){
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
        if (state.getVagabondType().equals("Vagabond")) {
            vagabondCharacter = "data/root/back.png";
        } else{
            vagabondCharacter = dataPath + state.getVagabondType().toLowerCase() + ".png";
        }
    }

    protected void update(RootGameState state){
        if (state.getVagabondType().equals("Vagabond")) {
            vagabondCharacter = "data/root/back.png";
        } else{
            vagabondCharacter = dataPath + state.getVagabondType().toLowerCase() + ".png";
        }
        FoxQuests = state.getCompletedQuests(RootParameters.ClearingTypes.Fox);
        MouseQuests = state.getCompletedQuests(RootParameters.ClearingTypes.Mouse);
        RabbitQuests = state.getCompletedQuests(RootParameters.ClearingTypes.Rabbit);
        relationship = "Cat: " + state.getRelationship(RootParameters.Factions.MarquiseDeCat).toString() + ", Eyrie: " + state.getRelationship(RootParameters.Factions.EyrieDynasties).toString() + ", Woodland: " + state.getRelationship(RootParameters.Factions.WoodlandAlliance).toString();
        ready.clear();
        exhausted.clear();
        damaged.clear();
        damagedExhausted.clear();
        coins.clear();
        bags.clear();
        teas.clear();
        for (Item item: state.getSatchel()){
            if (item.refreshed && !item.damaged){
                ready.add(dataPath+item.itemType.toString().toLowerCase()+".png");
            }else if (!item.refreshed && !item.damaged){
                exhausted.add(dataPath+item.itemType.toString().toLowerCase()+".png");
            } else if (item.refreshed) {
                damaged.add(dataPath+item.itemType.toString().toLowerCase()+".png");
            } else {
                damagedExhausted.add(dataPath+item.itemType.toString().toLowerCase()+".png");

            }
        }
        for (Item bag: state.getBags()){
            if (bag.refreshed && !bag.damaged){
                bags.add(dataPath+bag.itemType.toString().toLowerCase()+".png");
            } else if (!bag.refreshed && !bag.damaged) {
                exhausted.add(dataPath+bag.itemType.toString().toLowerCase()+".png");;
            } else if (bag.refreshed && bag.damaged) {
                damaged.add(dataPath+bag.itemType.toString().toLowerCase()+".png");
            } else{
                damagedExhausted.add(dataPath+bag.itemType.toString().toLowerCase()+".png");;
            }
        }
        for (Item item: state.getTeas()){
            if (item.refreshed && !item.damaged){
                teas.add(dataPath+item.itemType.toString().toLowerCase()+".png");
            }else if (!item.refreshed && !item.damaged){
                exhausted.add(dataPath+item.itemType.toString().toLowerCase()+".png");
            } else if (item.refreshed) {
                damaged.add(dataPath+item.itemType.toString().toLowerCase()+".png");
            } else {
                damagedExhausted.add(dataPath+item.itemType.toString().toLowerCase()+".png");

            }
        }
        for (Item item: state.getCoins()){
            if (item.refreshed && !item.damaged){
                coins.add(dataPath+item.itemType.toString().toLowerCase()+".png");
            }else if (!item.refreshed && !item.damaged){
                exhausted.add(dataPath+item.itemType.toString().toLowerCase()+".png");
            } else if (item.refreshed) {
                damaged.add(dataPath+item.itemType.toString().toLowerCase()+".png");
            } else {
                damagedExhausted.add(dataPath+item.itemType.toString().toLowerCase()+".png");

            }
        }
    }

    @Override
    protected void paintComponent(Graphics g){
        g.setFont(Font.getFont(Font.SANS_SERIF));
        g.setColor(Color.BLACK);

        g.drawString("R:", 15, 230);
        int x = 40;
        int y = 215;
        for (String string: ready){
            g.drawImage(ImageIO.GetInstance().getImage(string),x,y,20,20,null,null);
            x+=22;
        }
        g.drawString("E:", 15, 255);
         x = 40;
         y = 240;
        for (String string: exhausted){
            g.drawImage(ImageIO.GetInstance().getImage(string),x,y,20,20,null,null);
            x+=22;
        }
        g.drawString("DR:", 15, 280);
        x = 40;
        y = 265;
        for (String string: damaged){
            g.drawImage(ImageIO.GetInstance().getImage(string),x,y,20,20,null,null);
            x+=22;
        }
        g.drawString("DE:", 15, 305);
        x = 40;
        y = 290;
        for (String string: damagedExhausted){
            g.drawImage(ImageIO.GetInstance().getImage(string),x,y,20,20,null,null);
            x+=22;
        }
        g.setColor(new Color(255, 178, 102));
        g.fillOval(240,215,20,20);
        g.setColor(new Color(255, 128, 128));
        g.fillOval(240,240,20,20);
        g.setColor(new Color(255, 255, 102));
        g.fillOval(240,265,20,20);
        g.setColor(Color.black);
        g.drawString(MouseQuests+"",247,230);
        g.drawString(FoxQuests+"",247,255);
        g.drawString(RabbitQuests+"",247,280);
        g.drawString("C:", 270, 230);
        x = 295;
        y = 215;
        for (String string: coins){
            g.drawImage(ImageIO.GetInstance().getImage(string),x,y,20,20,null,null);
            x+=22;
        }
        g.drawString("B:", 270, 255);
        x = 295;
        y = 240;
        for (String string: bags){
            g.drawImage(ImageIO.GetInstance().getImage(string),x,y,20,20,null,null);
            x+=22;
        }
        g.drawString("T:", 270, 280);
        x = 295;
        y = 265;
        for (String string: teas){
            g.drawImage(ImageIO.GetInstance().getImage(string),x,y,20,20,null,null);
            x+=22;
        }
        g.drawString(relationship,120,320);
        g.drawImage(ImageIO.GetInstance().getImage(vagabondCharacter),310,5,cardWidth,cardHeight ,null,null);

    }
    @Override
    public Dimension getPreferredSize(){return new Dimension(width-20, height-25);}
}
