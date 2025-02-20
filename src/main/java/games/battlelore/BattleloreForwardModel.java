package games.battlelore;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.GridBoard;
import games.battlelore.actions.AttackUnitsAction;
import games.battlelore.actions.MoveUnitsAction;
import games.battlelore.actions.PlayCommandCardAction;
import games.battlelore.actions.SkipTurnAction;
import games.battlelore.cards.CommandCard;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BattleloreForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState initialState) {
        BattleloreGameParameters gameParams = (BattleloreGameParameters) initialState.getGameParameters();
        BattleloreGameState gameState = (BattleloreGameState) initialState;
        BattleloreData _data = new BattleloreData();
        _data.load(gameParams.getDataPath());

        if (gameState.getNPlayers() != 2) {
            throw new IllegalArgumentException("3 or more players are not supported");
        }

        //Init player hands
        int hexHeight = gameParams.hexHeight;
        int hexWidth = gameParams.hexWidth;

        //Game Area Initialization
        gameState.gameBoard = new GridBoard(hexWidth, hexHeight);
        gameState.unitTypes = new ArrayList<>();
        gameState.unitTypes = _data.getUnits();
        gameState.playerScores = new int[gameState.getNPlayers()];

        for (int x = 0; x < gameState.gameBoard.getWidth(); x++) {
            for (int y = 0; y < gameState.gameBoard.getHeight(); y++) {
                gameState.gameBoard.setElement(x, y, new MapTile(x, y, new ArrayList<>()));
            }
        }

        PutLearningScenarioUnits(gameState);
        gameState.setGamePhase(BattleloreGameState.BattleloreGamePhase.CommandAndOrderStep);
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        BattleloreGameState state = (BattleloreGameState) currentState;

        int playerId = state.getCurrentPlayer();
        Unit.Faction playerFaction = playerId == Unit.Faction.Dakhan_Lords.ordinal() ?
                Unit.Faction.Dakhan_Lords : Unit.Faction.Uthuk_Yllan;

        int maxRounds = currentState.getGameParameters().getMaxRounds();
        switch ((BattleloreGameState.BattleloreGamePhase) state.getGamePhase()) {
            case CommandAndOrderStep:
                currentState.setGamePhase(BattleloreGameState.BattleloreGamePhase.MoveStep);
                break;
            case MoveStep:
                if (state.GetMoveableUnitsFromTile(playerFaction).isEmpty()) {
                    currentState.setGamePhase(BattleloreGameState.BattleloreGamePhase.AttackStep);
                }
                break;
            case AttackStep:
                if (state.GetReadyForAttackUnitsFromTile(playerFaction).isEmpty()) {
                    endPlayerTurn(currentState);
                    if (currentState.getCurrentPlayer() == 0)
                        endRound(currentState);
                    currentState.setGamePhase(BattleloreGameState.BattleloreGamePhase.CommandAndOrderStep);
                }
                break;
            default:
                break;
        }

        if (checkGameEnd((BattleloreGameState) currentState, playerId) || state.getRoundCounter() >= maxRounds) {
            endGame(currentState);
        }
    }

    private void PutLearningScenarioUnits(BattleloreGameState gameState) {
        gameState.AddUnit(1, 2, gameState.GetUnitFromType(BattleloreGameState.UnitType.ViperLegion));
        gameState.AddUnit(3, 1,gameState.GetUnitFromType(BattleloreGameState.UnitType.ViperLegion));
        gameState.AddUnit(3, 2,gameState.GetUnitFromType(BattleloreGameState.UnitType.BloodHarvester));
        gameState.AddUnit(3, 3,gameState.GetUnitFromType(BattleloreGameState.UnitType.BloodHarvester));
        gameState.AddUnit(5, 3,gameState.GetUnitFromType(BattleloreGameState.UnitType.BloodHarvester));
        gameState.AddUnit(7, 3,gameState.GetUnitFromType(BattleloreGameState.UnitType.BloodHarvester));
        gameState.AddUnit(7, 1,gameState.GetUnitFromType(BattleloreGameState.UnitType.ViperLegion));
        gameState.AddUnit(8, 2,gameState.GetUnitFromType(BattleloreGameState.UnitType.BloodHarvester));
        gameState.AddUnit(10, 2,gameState.GetUnitFromType(BattleloreGameState.UnitType.ViperLegion));

        gameState.AddUnit(1, 6,gameState.GetUnitFromType(BattleloreGameState.UnitType.YeomanArcher));
        gameState.AddUnit(3, 7,gameState.GetUnitFromType(BattleloreGameState.UnitType.YeomanArcher));
        gameState.AddUnit(3, 5,gameState.GetUnitFromType(BattleloreGameState.UnitType.CitadelGuard));
        gameState.AddUnit(3, 6,gameState.GetUnitFromType(BattleloreGameState.UnitType.CitadelGuard));
        gameState.AddUnit(5, 5,gameState.GetUnitFromType(BattleloreGameState.UnitType.CitadelGuard));
        gameState.AddUnit(7, 5,gameState.GetUnitFromType(BattleloreGameState.UnitType.CitadelGuard));
        gameState.AddUnit(7, 7,gameState.GetUnitFromType(BattleloreGameState.UnitType.YeomanArcher));
        gameState.AddUnit(8, 6,gameState.GetUnitFromType(BattleloreGameState.UnitType.CitadelGuard));
        gameState.AddUnit(10, 6,gameState.GetUnitFromType(BattleloreGameState.UnitType.YeomanArcher));
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        BattleloreGameState state = (BattleloreGameState) gameState;
        int player = gameState.getCurrentPlayer();
        Unit.Faction playerFaction = player == Unit.Faction.Dakhan_Lords.ordinal() ? Unit.Faction.Dakhan_Lords : Unit.Faction.Uthuk_Yllan;

        List<AbstractAction> actions = new ArrayList<>();

        if (gameState.getGamePhase() == BattleloreGameState.BattleloreGamePhase.CommandAndOrderStep) {
            if (CheckUnitRemainingAtRight(state, player, MapTile.TileArea.right)) {
                actions.add(new PlayCommandCardAction(CommandCard.CommandType.AttackRight, playerFaction, player));
            }
            if (CheckUnitRemainingAtRight(state, player, MapTile.TileArea.mid)) {
                actions.add(new PlayCommandCardAction(CommandCard.CommandType.BattleMarch, playerFaction, player));
            }
            if (CheckUnitRemainingAtRight(state, player, MapTile.TileArea.left)) {
                actions.add(new PlayCommandCardAction(CommandCard.CommandType.PatrolLeft, playerFaction, player));
            }
        }
        if (gameState.getGamePhase() == BattleloreGameState.BattleloreGamePhase.MoveStep) {
            List<MapTile> moveableUnitTiles = state.GetMoveableUnitsFromTile(playerFaction);
            int[][] possibleLocations;

            if (!moveableUnitTiles.isEmpty()) {
                for (MapTile tile : moveableUnitTiles) {
                    possibleLocations = state.GetPossibleLocationsForUnits(tile);
                    //check possible locations size
                    for (int i = 0; i < state.getBoard().getWidth(); i++) {
                        if (possibleLocations[i][0] != -1 || possibleLocations[i][1] != -1) {
                            actions.add(new MoveUnitsAction(tile.getComponentID(), playerFaction, possibleLocations[i][0], possibleLocations[i][1], player));
                        }
                    }
                    if (actions.isEmpty()) {
                        actions.add(new SkipTurnAction(tile.getComponentID(), playerFaction, true, false, player));
                    }
                }
            }
            //if moveable unit count has reached the end, state is finished
        }
        if (gameState.getGamePhase() == BattleloreGameState.BattleloreGamePhase.AttackStep) {
            ArrayList<MapTile> readyToAttackUnits = state.GetReadyForAttackUnitsFromTile(playerFaction);
            int[][] possibleLocations;

            if (!readyToAttackUnits.isEmpty()) {
                for (MapTile attacker : readyToAttackUnits) {
                    possibleLocations = state.GetPossibleTargetUnits(attacker);

                    for (int i = 0; i < state.getBoard().getWidth(); i++) {
                        if (possibleLocations[i][0] != -1 || possibleLocations[i][1] != -1) {
                            actions.add(new AttackUnitsAction(attacker.getComponentID(), state.getBoard().getElement(possibleLocations[i][0], possibleLocations[i][1]).getComponentID(),
                                    attacker.GetFaction(), player));
                        }
                    }
                    if (actions.isEmpty()) {
                        actions.add(new SkipTurnAction(attacker.getComponentID(), playerFaction, false, true, player));
                    }
                }
            }
        }

        if (actions.isEmpty()) {
            actions.add(new SkipTurnAction(playerFaction, player));
        }
        return actions;
    }

    private boolean CheckUnitRemainingAtRight(BattleloreGameState gameState, int playerId, MapTile.TileArea area) {
        boolean allyUnitsRemainInArea = false;
        boolean enemyUnitsRemainInArea = false;

        for (int x = 0; x < gameState.gameBoard.getWidth(); x++) {
            for (int y = 0; y < gameState.gameBoard.getHeight(); y++) {
                MapTile tile = (MapTile) gameState.gameBoard.getElement(x, y);
                Unit.Faction playerFaction = playerId == Unit.Faction.Dakhan_Lords.ordinal() ? Unit.Faction.Dakhan_Lords : Unit.Faction.Uthuk_Yllan;

                if (tile != null && tile.GetUnits() != null && tile.GetUnits().size() > 0 && tile.IsInArea(area)) {
                    if (tile.GetFaction() == playerFaction) {
                        allyUnitsRemainInArea = true;
                    }
                    if (tile.GetFaction() != playerFaction) {
                        enemyUnitsRemainInArea = true;
                    }
                }
            }
        }
        return allyUnitsRemainInArea && enemyUnitsRemainInArea;
    }

    /**
     * Checks if the game ended.
     *
     * @param gameState - game state to check game end.
     */
    private boolean checkGameEnd(BattleloreGameState gameState, int playerId) {
        BattleloreGameParameters parameters = (BattleloreGameParameters) gameState.getGameParameters();
        return gameState.getGameScore(playerId) >= parameters.WIN_SCORE;
    }

}
