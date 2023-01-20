package gametemplate;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;

public class GTGUIManager extends AbstractGUIManager {
    public GTGUIManager(GamePanel parent, Game game, ActionController ac, int human) {
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
