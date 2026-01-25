package games.root.gui;

import core.components.Deck;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.RootCard;
import games.root.components.Item;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static games.root.gui.RootGUIManager.*;
import static games.root.gui.RootGUIManager.cardHeight;

public class EyriePlayerView extends JComponent {
    RootDeckView playerHand;
    RootDeckView craftedCards;
    List<String> DecreeRecruit = new ArrayList<>();
    List<String> DecreeMove = new ArrayList<>();
    List<String> DecreeBattle = new ArrayList<>();
    List<String> DecreeBuild = new ArrayList<>();
    int width, height;
    int humanID;
    int playerID;
    int warriors;
    int roosts;
    private List<String> craftedItems = new ArrayList<>();
    String dataPath;
    String ruler;
    int border = 5;
    int textHeight = 15;
    int handWidth = playerAreaWidth - cardWidth - border * 2;
    public EyriePlayerView(int playerID, int humanID, String dataPath, RootGameState state) {
        this.dataPath = dataPath;
        this.humanID = humanID;
        this.playerID = playerID;
        this.width = playerAreaWidth + border * 2;
        this.height = playerAreaHeight + textHeight + border;
        playerHand = new RootDeckView(humanID, state.getPlayerHand(playerID), false, dataPath, new Rectangle(0, 0, handWidth, cardHeight));
        craftedCards = new RootDeckView(humanID, state.getPlayerCraftedCards(playerID), true, dataPath, new Rectangle(0, 0, handWidth, cardHeight));
        this.setLayout(new GridLayout(3, 1));
        this.add(playerHand);
        this.add(craftedCards);
        if (state.getRuler() != null) {
            ruler = dataPath + state.getRulerName().toLowerCase() + ".png";
        } else{
            ruler = "data/root/back.png";
        }

    }

    protected void update(RootGameState state){
        RootParameters rp = (RootParameters) state.getGameParameters();
        roosts = state.getBuildingCount(RootParameters.BuildingType.Roost);
        warriors = state.getBirdWarriors();
        if (state.getRuler() != null) {
            ruler = rp.getDataPath() + state.getRulerName() + ".png";
        }
        List<Deck<RootCard>> decree = state.getDecree();
        DecreeRecruit.clear();
        DecreeMove.clear();
        DecreeBattle.clear();
        DecreeBuild.clear();
        for (int i = 0; i < decree.get(0).getSize(); i++){
            DecreeRecruit.add(dataPath + decree.get(0).get(i).cardType.toString() + decree.get(0).get(i).suit.toString() + ".png");
        }
        for (int i = 0; i < decree.get(1).getSize(); i++){
            DecreeMove.add(dataPath + decree.get(1).get(i).cardType.toString() + decree.get(1).get(i).suit.toString() + ".png");
        }
        for (int i = 0; i < decree.get(2).getSize(); i++){
            DecreeBattle.add(dataPath + decree.get(2).get(i).cardType.toString() + decree.get(2).get(i).suit.toString() + ".png");
        }
        for (int i = 0; i < decree.get(3).getSize(); i++){
            DecreeBuild.add(dataPath + decree.get(3).get(i).cardType.toString().toLowerCase() + decree.get(3).get(i).suit.toString().toLowerCase() + ".png");
        }
        craftedItems.clear();
        for (Item item: state.getPlayerCraftedItems(playerID)){
            craftedItems.add(dataPath + item.itemType.toString().toLowerCase());
        }

    }

    @Override
    protected void paintComponent(Graphics g){
        g.setFont(Font.getFont(Font.SANS_SERIF));
        g.setColor(Color.BLACK);
        g.drawString("warriors: " + warriors, 15,230);
        int xRoosts = 15;
        g.setColor(new Color(0, 128, 255));
        for (int i =0; i < roosts; i++){
            g.fillOval(xRoosts,236,20,20);
            xRoosts += 22;
        }
        g.drawImage(ImageIO.GetInstance().getImage(ruler),310,20,cardWidth,cardHeight ,null,null);

        int recruitY = 260;
        for (String string: DecreeRecruit){
            g.drawImage(ImageIO.GetInstance().getImage(string),10,recruitY,cardWidth/2,cardHeight/2 ,null,null);
            recruitY += 6;
        }
        int moveY = 260;
        for (String string: DecreeMove){
            g.drawImage(ImageIO.GetInstance().getImage(string),45,moveY,cardWidth/2,cardHeight/2 ,null,null);
            moveY += 6;
        }
        int battleY = 260;
        for (String string: DecreeBattle){
            g.drawImage(ImageIO.GetInstance().getImage(string),80,battleY,cardWidth/2,cardHeight/2 ,null,null);
            battleY += 6;
        }
        int buildY = 260;
        for (String string: DecreeBuild){
            g.drawImage(ImageIO.GetInstance().getImage(string),110,buildY,cardWidth/2,cardHeight/2 ,null,null);
            buildY += 6;
        }
        g.setColor(Color.BLACK);
        g.drawString("Crafted Items", 200, 230);
        int x = 200;
        int y = 240;
        for (String string: craftedItems){
            g.drawImage(ImageIO.GetInstance().getImage(string  + ".png"),x,y,30,30,null,null);
            x+=30;
        }
    }

    @Override
    public Dimension getPreferredSize(){return new Dimension(width-20, height-25);}
}
