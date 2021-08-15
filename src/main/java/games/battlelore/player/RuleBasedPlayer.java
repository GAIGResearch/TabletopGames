package games.battlelore.player;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.components.GridBoard;
import games.battlelore.BattleloreGameParameters;
import games.battlelore.BattleloreGameState;
import games.battlelore.actions.AttackUnitsAction;
import games.battlelore.actions.MoveUnitsAction;
import games.battlelore.actions.PlayCommandCardAction;
import games.battlelore.actions.SkipTurnAction;
import games.battlelore.cards.CommandCard;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;

import utilities.Utils;

import java.util.List;
import java.util.Random;

public class RuleBasedPlayer extends AbstractPlayer
{
    /**
     * A rule based agent
     */

    private final Random rnd;

    public RuleBasedPlayer()
    {
        this.rnd = new Random();
    }

    @Override
    public AbstractAction getAction(AbstractGameState observation, List<AbstractAction> actions)
    {
        BattleloreGameState state = (BattleloreGameState) observation;
        GridBoard<MapTile> board = state.getBoard();
        float playerUnitPower = 0.f;
        float enemyUnitPower = 0.f;
        int leftAreaPower = 0;
        int middleAreaPower = 0;
        int rightAreaPower = 0;

        for (int x = 0; x < board.getWidth(); x++)
        {
            for (int y = 0; y < board.getHeight(); y++)
            {
                MapTile tile = board.getElement(x, y);
                Unit.Faction playerFaction = observation.getCurrentPlayer() == Unit.Faction.Dakhan_Lords.ordinal() ? Unit.Faction.Dakhan_Lords : Unit.Faction.Uthuk_Yllan;
                if (tile != null && tile.GetUnits() != null && tile.GetUnits().size() > 0)
                {
                    if (tile.GetFaction() == playerFaction)
                    {
                        if(tile.IsInArea(MapTile.TileArea.left))
                        {
                            leftAreaPower += tile.GetUnits().get(0).getTotalStrength();
                        }
                        else if (tile.IsInArea(MapTile.TileArea.mid))
                        {
                            middleAreaPower += tile.GetUnits().get(0).getTotalStrength();
                        }
                        if (tile.IsInArea(MapTile.TileArea.right))
                        {
                            rightAreaPower += tile.GetUnits().get(0).getTotalStrength();
                        }


                        BattleloreGameState.BattleloreGamePhase a = (BattleloreGameState.BattleloreGamePhase)state.getGamePhase();
                        playerUnitPower += tile.GetUnits().size() * tile.GetUnits().get(0).getTotalStrength() * tile.GetUnits().get(0).getTotalHealth();
                        if ((BattleloreGameState.BattleloreGamePhase)state.getGamePhase() == BattleloreGameState.BattleloreGamePhase.MoveStep)//Checking the next step for result
                        {
                            //orderableUnitCount += tile.GetUnits().get(0).CanAttack() && tile.GetUnits().get(0).CanMove() ? 1 : 0;
                        }
                    }
                    else
                    {
                        enemyUnitPower += tile.GetUnits().size() * tile.GetUnits().get(0).getTotalStrength() * tile.GetUnits().get(0).getTotalHealth();
                    }
                }
            }
        }

        AbstractAction selectedAction;

            for (AbstractAction action : actions)
            {
                if (action instanceof AttackUnitsAction)
                {
                    if (playerUnitPower > enemyUnitPower) // Aggressive Gameplay
                    {
                        AttackUnitsAction act = (AttackUnitsAction) action;
                        if (act.GetAttacker().GetUnits().get(0).getTotalStrength() > act.GetDefender().GetUnits().get(0).getTotalStrength())
                        {
                            return action;
                        }
                    }
                }

                if (action instanceof MoveUnitsAction)
                {
                    MoveUnitsAction act = (MoveUnitsAction) action;

                    //if (act.GetAttacker().GetUnits().get(0).getTotalStrength() > act.GetDefender().GetUnits().get(0).getTotalStrength())
                    //{
                    //    return action;
                    //}
                }

                if (action instanceof PlayCommandCardAction)
                {
                    PlayCommandCardAction act = (PlayCommandCardAction) action;
                    if(leftAreaPower >= rightAreaPower && leftAreaPower >= middleAreaPower && act.GetCommandType() == CommandCard.CommandType.PatrolLeft)
                    {
                        return action;
                    }
                    if (middleAreaPower >= rightAreaPower && middleAreaPower >= leftAreaPower && act.GetCommandType() == CommandCard.CommandType.BattleMarch)
                    {
                        return action;
                    }
                    if (rightAreaPower >= middleAreaPower && rightAreaPower >= leftAreaPower && act.GetCommandType() == CommandCard.CommandType.AttackRight)
                    {
                        return action;
                    }

                }

            }


       // double playerScore = playerUnitPower * FACTOR_PLAYER_POWER + orderableUnitCount * FACTOR_ORDERABLE_UNITS;
       // double enemyPower = enemyUnitPower * FACTOR_ENEMY_POWER;

       // double clampedValue = Math.max(0.1, Math.min(0.9, (playerScore - enemyPower)/(playerScore + enemyPower)));
        //double value = Math. ((playerScore - enemyPower) / (1 - playerScore + enemyPower));





        int randomAction = rnd.nextInt(actions.size());
        return actions.get(randomAction);
    }

    @Override
    public String toString() {
        return "RuleBased";
    }
}

