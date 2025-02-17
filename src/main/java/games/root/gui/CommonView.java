package games.root.gui;

import games.root.RootGameState;
import games.root.components.Item;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static games.root.gui.RootGUIManager.*;
import static games.root.gui.RootGUIManager.cardHeight;

public class CommonView extends JComponent {
    RootDeckView drawPile;
    RootDeckView discardPile;
    RootQuestDeckView questPile;
    RootQuestDeckView activeQuests;
    private List<String> craftabledItems = new ArrayList<>();
    String dataPath;
    int border = 5;
    int textHeight = 15;
    int handWidth = playerAreaWidth - cardWidth - border * 2;
    public CommonView(int playerID, int humanID, String dataPath, RootGameState state) {
        this.dataPath = dataPath;
            discardPile = new RootDeckView(-1, state.getDiscardPile(), true, dataPath, new Rectangle(0, 10, cardWidth, cardHeight));
            drawPile = new RootDeckView(-1, state.getDrawPile(), false, dataPath, new Rectangle(0, 10, cardWidth, cardHeight));
            if (state.getNPlayers() > 3) {
            questPile = new RootQuestDeckView(-1, state.getQuestDrawPile(), false, dataPath, new Rectangle(0, 10, handWidth / 2, cardHeight));
            activeQuests = new RootQuestDeckView(-1, state.getActiveQuests(), true, dataPath, new Rectangle(0, 10, handWidth / 2, cardHeight));
        }
        this.setLayout(new GridLayout(1, 4));
        this.add(drawPile);
        this.add(discardPile);
        if (state.getNPlayers() > 3) {
            this.add(questPile);
            this.add(activeQuests);
        }

    }

    protected void update(RootGameState state){
        craftabledItems.clear();
        for (Item item: state.getCraftableItems()){
            craftabledItems.add(dataPath + item.itemType.toString().toLowerCase() + ".png");
        }
    }

    @Override
    protected void paintComponent(Graphics g){
        g.setFont(Font.getFont(Font.SANS_SERIF));
        g.setColor(Color.BLACK);
        g.drawString("AvailableItems: ", 15,150);
        int x = 15;
        int y = 160;
        for (String string: craftabledItems){
            g.drawImage(ImageIO.GetInstance().getImage(string),x,y,30,30,null,null);
            x+=30;
        }
    }
}
