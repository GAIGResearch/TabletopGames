package games.battleship.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;

import java.util.Set;

public class BattleshipGUIManager extends AbstractGUIManager {
    public BattleshipGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
    }

    @Override
    public int getMaxActionSpace() {
        return 0;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {

    }
}
