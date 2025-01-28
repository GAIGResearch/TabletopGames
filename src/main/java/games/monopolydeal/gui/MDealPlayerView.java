package games.monopolydeal.gui;

import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.MonopolyDealParameters;
import games.monopolydeal.cards.SetType;

import javax.swing.*;

import java.awt.*;
import java.util.Set;

import static games.monopolydeal.gui.MonopolyDealGUIManager.*;

public class MDealPlayerView extends JComponent {
    public final int playerID;

    MonopolyDealDeckView hand;
    MonopolyDealDeckView bank;
    MonopolyDealDeckView[] properties;

    Dimension size;

    public MDealPlayerView(int playerID, Set<Integer> humanID, MonopolyDealParameters mdp, MonopolyDealGameState mdgs) {
        this.playerID = playerID;

        // todo add labels for hand (# cards), bank (value), properties (# sets)

        hand = new MonopolyDealDeckView(playerID, mdgs.getPlayerHand(playerID), humanID.contains(playerID) || mdgs.getCoreGameParameters().alwaysDisplayFullObservable,
                mdp.getDataPath(), new Rectangle(0, 0, MonopolyDealCardWidth*3, MonopolyDealCardHeight), MonopolyDealCardWidth, MonopolyDealCardHeight);
        bank = new MonopolyDealDeckView(playerID, mdgs.getPlayerBank(playerID), true, mdp.getDataPath(), new Rectangle(0, 0, MonopolyDealCardWidth*3, MonopolyDealCardHeight), MonopolyDealCardWidth, MonopolyDealCardHeight);

        properties = new MonopolyDealDeckView[SetType.values().length];
        for(int j = 0; j< SetType.values().length; j++){
            MonopolyDealDeckView playerProperty = new MonopolyDealDeckView(playerID, null,true,mdp.getDataPath(),  new Rectangle(0, 0, MonopolyDealCardWidth, (int)(MonopolyDealCardHeight*1.5)), MonopolyDealCardWidth, MonopolyDealCardHeight);
            properties[j] = playerProperty;
            playerProperty.setVisible(false);
        }

        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.add(hand);
        this.add(Box.createRigidArea(new Dimension(5,0)));//spacer
        this.add(bank);
        this.add(Box.createRigidArea(new Dimension(5,0)));//spacer
        for (int i = 0; i < 10; i++) {
            this.add(properties[i]);
            this.add(Box.createRigidArea(new Dimension(2,0)));//spacer
        }

        size = new Dimension(playerAreaWidth, MonopolyDealCardHeight*2);
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }
}
