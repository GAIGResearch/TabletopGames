package games.battlelore;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.GridBoard;
import games.battlelore.actions.AttackUnitsAction;
import games.battlelore.actions.MoveUnitsAction;
import games.battlelore.actions.PlayCommandCardAction;
import games.battlelore.actions.SkipTurnAction;
import games.battlelore.cards.CommandCard;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BattleloreForwardModel extends AbstractForwardModel {

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
        gameState.gameBoard = new GridBoard<>(hexWidth, hexHeight);
        gameState.unitTypes = new ArrayList<>();
        gameState.unitTypes = _data.getUnits();

        for (int x = 0; x < gameState.gameBoard.getWidth(); x++) {
            for (int y = 0; y < gameState.gameBoard.getHeight(); y++) {
                gameState.gameBoard.setElement(x, y, new MapTile(x, y, new ArrayList<>()));
            }
        }

        PutLearningScenarioUnits(gameState);
        gameState.setGamePhase(BattleloreGameState.BattleloreGamePhase.CommandAndOrderStep);
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        BattleloreGameState state = (BattleloreGameState) currentState;
        action.execute(currentState);

        int playerId = state.getCurrentPlayer();
        Unit.Faction playerFaction = playerId == Unit.Faction.Dakhan_Lords.ordinal() ?
                Unit.Faction.Dakhan_Lords : Unit.Faction.Uthuk_Yllan;

        int maxTurnsToPlay = ((BattleloreGameParameters)currentState.getGameParameters()).maxTurnsToPlay;
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
                    state.getTurnOrder().endPlayerTurn(state);
                    currentState.setGamePhase(BattleloreGameState.BattleloreGamePhase.CommandAndOrderStep);
                }
                break;
            default:
                break;
        }

        if (checkGameEnd((BattleloreGameState) currentState, playerId)) {
            registerWinner(state, playerId);
        } else if (state.getTurnOrder().getRoundCounter() > maxTurnsToPlay) {
            if (state.getGameScore(0) == state.getGameScore(1))
                registerWinner(state, -1);
            else
                registerWinner(state, state.getGameScore(0) >= state.getGameScore(1) ? 0 : 1);
        }
    }

    private void PutLearningScenarioUnits(BattleloreGameState gameState) {
        gameState.gameBoard.getElement(1, 2).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.ViperLegion));
        gameState.gameBoard.getElement(3, 1).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.ViperLegion));
        gameState.gameBoard.getElement(3, 2).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.BloodHarvester));
        gameState.gameBoard.getElement(3, 3).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.BloodHarvester));
        gameState.gameBoard.getElement(5, 3).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.BloodHarvester));
        gameState.gameBoard.getElement(7, 3).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.BloodHarvester));
        gameState.gameBoard.getElement(7, 1).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.ViperLegion));
        gameState.gameBoard.getElement(8, 2).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.BloodHarvester));
        gameState.gameBoard.getElement(10, 2).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.ViperLegion));

        gameState.gameBoard.getElement(1, 6).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.YeomanArcher));
        gameState.gameBoard.getElement(3, 7).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.YeomanArcher));
        gameState.gameBoard.getElement(3, 5).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.CitadelGuard));
        gameState.gameBoard.getElement(3, 6).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.CitadelGuard));
        gameState.gameBoard.getElement(5, 5).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.CitadelGuard));
        gameState.gameBoard.getElement(7, 5).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.CitadelGuard));
        gameState.gameBoard.getElement(7, 7).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.YeomanArcher));
        gameState.gameBoard.getElement(8, 6).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.CitadelGuard));
        gameState.gameBoard.getElement(10, 6).AddUnit(gameState.GetUnitFromType(BattleloreGameState.UnitType.YeomanArcher));
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        BattleloreGameState state = (BattleloreGameState) gameState;
        int player = gameState.getTurnOrder().getCurrentPlayer(gameState);
        Unit.Faction playerFaction = player == Unit.Faction.Dakhan_Lords.ordinal() ? Unit.Faction.Dakhan_Lords : Unit.Faction.Uthuk_Yllan;

        ArrayList<AbstractAction> actions = new ArrayList<>();

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
            ArrayList<MapTile> moveableUnitTiles = state.GetMoveableUnitsFromTile(playerFaction);
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
                MapTile tile = gameState.gameBoard.getElement(x, y);
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

    @Override
    protected AbstractForwardModel _copy() {
        return new BattleloreForwardModel();
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


    @Override
    protected void endGame(AbstractGameState gameState) {
        if (gameState.getCoreGameParameters().verbose) {
            System.out.println(Arrays.toString(gameState.getPlayerResults()));
        }
    }


    private void registerWinner(BattleloreGameState gameState, int winnerID) {
        gameState.setGameStatus(Utils.GameResult.GAME_END);
        if (winnerID != -1) {
            gameState.setPlayerResult(Utils.GameResult.WIN, winnerID);
            gameState.setPlayerResult(Utils.GameResult.LOSE, winnerID == 0 ? 1 : 0);
        } else {
            gameState.setPlayerResult(Utils.GameResult.DRAW, 0);
            gameState.setPlayerResult(Utils.GameResult.DRAW, 1);
        }
    }
}
