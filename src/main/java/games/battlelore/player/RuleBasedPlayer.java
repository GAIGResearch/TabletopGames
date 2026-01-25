package games.battlelore.player;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.components.GridBoard;
import games.battlelore.BattleloreGameState;
import games.battlelore.actions.AttackUnitsAction;
import games.battlelore.actions.PlayCommandCardAction;
import games.battlelore.cards.CommandCard;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;

import java.util.List;
import java.util.Random;

public class RuleBasedPlayer extends AbstractPlayer
{
    /**
     * A rule based agent
     */

    private final Random rnd;

    public RuleBasedPlayer() {
        super(null, "RuleBasedPlayer");
        this.rnd = new Random();
    }

    @Override
    public AbstractAction _getAction(AbstractGameState observation, List<AbstractAction> actions) {
        BattleloreGameState state = (BattleloreGameState) observation;
        GridBoard board = state.getBoard();
        float playerUnitPower = 0.f;
        float enemyUnitPower = 0.f;
        int leftAreaPower = 0;
        int middleAreaPower = 0;
        int rightAreaPower = 0;

        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                MapTile tile = (MapTile) board.getElement(x, y);
                Unit.Faction playerFaction = observation.getCurrentPlayer() == Unit.Faction.Dakhan_Lords.ordinal()
                        ? Unit.Faction.Dakhan_Lords : Unit.Faction.Uthuk_Yllan;

                if (tile != null && tile.GetUnits() != null && tile.GetUnits().size() > 0) {
                    if (tile.GetFaction() == playerFaction) {
                        if(tile.IsInArea(MapTile.TileArea.left)) {
                            leftAreaPower += tile.GetUnits().get(0).getTotalStrength();
                        }
                        else if (tile.IsInArea(MapTile.TileArea.mid)) {
                            middleAreaPower += tile.GetUnits().get(0).getTotalStrength();
                        }
                        if (tile.IsInArea(MapTile.TileArea.right)) {
                            rightAreaPower += tile.GetUnits().get(0).getTotalStrength();
                        }

                        BattleloreGameState.BattleloreGamePhase a = (BattleloreGameState.BattleloreGamePhase)state.getGamePhase();
                        playerUnitPower += tile.GetUnits().size() * tile.GetUnits().get(0).getTotalStrength() * tile.GetUnits().get(0).getTotalHealth();
                    }
                    else {
                        enemyUnitPower += tile.GetUnits().size() * tile.GetUnits().get(0).getTotalStrength() * tile.GetUnits().get(0).getTotalHealth();
                    }
                }
            }
        }

        AbstractAction selectedAction;
            for (AbstractAction action : actions) {
                if (action instanceof AttackUnitsAction) {
                    //Aggressive Gameplay
                    if (playerUnitPower > enemyUnitPower)  {
                        AttackUnitsAction act = (AttackUnitsAction) action;
                        if (act.GetAttacker(observation).GetUnits().get(0).getTotalStrength() > act.GetDefender(observation).GetUnits().get(0).getTotalStrength()) {
                            return action;
                        }
                    }
                }

                if (action instanceof PlayCommandCardAction) {
                    PlayCommandCardAction act = (PlayCommandCardAction) action;
                    if (leftAreaPower >= rightAreaPower && leftAreaPower >= middleAreaPower && act.GetCommandType() == CommandCard.CommandType.PatrolLeft) {
                        return action;
                    }
                    if (middleAreaPower >= rightAreaPower && middleAreaPower >= leftAreaPower && act.GetCommandType() == CommandCard.CommandType.BattleMarch) {
                        return action;
                    }
                    if (rightAreaPower >= middleAreaPower && rightAreaPower >= leftAreaPower && act.GetCommandType() == CommandCard.CommandType.AttackRight) {
                        return action;
                    }
                }
            }

        int randomAction = rnd.nextInt(actions.size());
        return actions.get(randomAction);
    }

    @Override
    public String toString() {
        return "RuleBased";
    }

    @Override
    public AbstractPlayer copy() {
        return this;
    }
}

