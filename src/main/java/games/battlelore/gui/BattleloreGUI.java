package games.battlelore.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import players.human.ActionController;

public class BattleloreGUI extends AbstractGUI
{
    public BattleloreGUI(ActionController ac, int maxActionSpace)
    {
        super(ac, maxActionSpace);
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState)
    {

    }
}
