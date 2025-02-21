package games.battlelore;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;

public class BattleloreHeuristic extends TunableParameters implements IStateHeuristic {
    double FACTOR_PLAYER_POWER = 0.8;
    double FACTOR_ENEMY_POWER = 0.5f;
    double FACTOR_ORDERABLE_UNITS = 0.3f;

    public BattleloreHeuristic() {
        addTunableParameter("FACTOR_PLAYER_POWER", 0.8);
        addTunableParameter("FACTOR_ENEMY_POWER", 0.5);
        addTunableParameter("FACTOR_ORDERABLE_UNITS", 0.3);
    }

    @Override
    public void _reset() {
        FACTOR_PLAYER_POWER = (double) getParameterValue("FACTOR_PLAYER_POWER");
        FACTOR_ENEMY_POWER = (double) getParameterValue("FACTOR_ENEMY_POWER");
        FACTOR_ORDERABLE_UNITS = (double) getParameterValue("FACTOR_ORDERABLE_UNITS");
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        BattleloreGameState gameState = (BattleloreGameState) gs;
        CoreConstants.GameResult playerResult = gameState.getPlayerResults()[playerId];

        int playerUnitPower = 0;
        int orderableUnitCount = 0;
        int enemyUnitPower = 0;

        if (playerResult == CoreConstants.GameResult.LOSE_GAME) {
            return -1;
        }
        else if (playerResult == CoreConstants.GameResult.WIN_GAME) {
            return 1;
        }
        else if (playerResult == CoreConstants.GameResult.DRAW_GAME) {
            return 0;
        }

        for (int x = 0; x < gameState.gameBoard.getWidth(); x++) {
            for(int y = 0; y < gameState.gameBoard.getHeight(); y++) {

                MapTile tile = (MapTile) gameState.gameBoard.getElement(x, y);
                Unit.Faction playerFaction = playerId == Unit.Faction.Dakhan_Lords.ordinal() ? Unit.Faction.Dakhan_Lords : Unit.Faction.Uthuk_Yllan;

                if (tile != null && tile.GetUnits() != null && tile.GetUnits().size() > 0) {
                    if (tile.GetFaction() == playerFaction) {
                        playerUnitPower += tile.GetUnits().size() * tile.GetUnits().get(0).getTotalStrength() * tile.GetUnits().get(0).getTotalHealth();

                        //Checking the next step for result
                        if ((BattleloreGameState.BattleloreGamePhase)gameState.getGamePhase() == BattleloreGameState.BattleloreGamePhase.MoveStep) {
                            orderableUnitCount += tile.GetUnits().get(0).CanAttack() && tile.GetUnits().get(0).CanMove() ? 1 : 0;
                        }
                    }
                    else {
                        enemyUnitPower += tile.GetUnits().size() * tile.GetUnits().get(0).getTotalStrength() * tile.GetUnits().get(0).getTotalHealth();
                    }
                }
            }
        }

        double playerScore = playerUnitPower * FACTOR_PLAYER_POWER + orderableUnitCount * FACTOR_ORDERABLE_UNITS;
        double enemyPower = enemyUnitPower * FACTOR_ENEMY_POWER;
        double clampedValue = Math.max(0.1, Math.min(0.9, (playerScore - enemyPower)/(playerScore + enemyPower)));

        return clampedValue;
    }

    @Override
    public Object instantiate() {
        return this._copy();
    }

    @Override
    protected BattleloreHeuristic _copy() {
        BattleloreHeuristic retValue = new BattleloreHeuristic();
        retValue.FACTOR_PLAYER_POWER = FACTOR_PLAYER_POWER;
        retValue.FACTOR_ORDERABLE_UNITS = FACTOR_ORDERABLE_UNITS;
        retValue.FACTOR_ENEMY_POWER = FACTOR_ENEMY_POWER;
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof BattleloreHeuristic) {
            BattleloreHeuristic other = (BattleloreHeuristic) o;
            return other.FACTOR_ENEMY_POWER == FACTOR_ENEMY_POWER &&
                    other.FACTOR_PLAYER_POWER == FACTOR_PLAYER_POWER &&
                    other.FACTOR_ORDERABLE_UNITS == FACTOR_ORDERABLE_UNITS;
        }
        return false;
    }
}
