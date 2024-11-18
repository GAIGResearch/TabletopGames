package games.saboteur.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;

import java.util.Set;

public class SaboteurGUIManager extends AbstractGUIManager {
    //all subjected to change
    final static int playerAreaWidth = 300;
    final static int playerAreaHeight = 100;
    final static int cardWidth = 50;
    final static int cardHeight = 100;

    public SaboteurGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanID)
    {
        super(parent, game, ac, humanID);
        //add the rest later
    }
    @Override
    public int getMaxActionSpace() {
        return 0;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {

    }
}
