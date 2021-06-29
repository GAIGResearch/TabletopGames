package games.battlelore;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.GridBoard;

import core.interfaces.IGamePhase;
import games.battlelore.actions.AttackUnitsAction;
import games.battlelore.actions.MoveUnitsAction;
import games.battlelore.actions.PlayCommandCardAction;
import games.battlelore.actions.SkipTurnAction;
import games.battlelore.cards.CommandCard;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;

import utilities.Utils;

import java.util.*;

import static core.CoreConstants.VERBOSE;

public class BattleloreForwardModel extends AbstractForwardModel
{
    @Override
    protected void _setup(AbstractGameState initialState)
    {
        Random random = new Random(initialState.getGameParameters().getRandomSeed());
        BattleloreGameParameters gameParams = (BattleloreGameParameters) initialState.getGameParameters();
        BattleloreGameState gameState = (BattleloreGameState)initialState;
        BattleloreData _data = gameState.getData();

        for (int i = 0; i  < gameState.playerCount; i++)
        {
            if (gameState.playerCount == 2)
            {
                //Set Player Faction
            }
            else
            {
                System.out.println("3 and more players are not supported");
            }
        }

        //Init player hands

        int hexHeight = gameParams.hexHeight;
        int hexWidth = gameParams.hexWidth;


        //MapTile[][] mapTiles= new MapTile[hexWidth][hexHeight];

        //Game Area Initialization
        gameState.gameBoard = new GridBoard<MapTile>(hexWidth, hexHeight);
        gameState.unitTypes = new ArrayList<>();
        gameState.unitTypes = _data.getUnits();

        int tileId = 0;
        for (int x = 0; x < gameState.gameBoard.getWidth(); x++)
        {
            for(int y = 0; y < gameState.gameBoard.getHeight(); y++)
            {
                gameState.gameBoard.setElement(x, y, new MapTile(x, y, new ArrayList<Unit>(), null, tileId));
                tileId++;
            }
        }

        PutLearningScenarioUnits(gameState);

        gameState.setGamePhase(BattleloreGameState.BattleloreGamePhase.CommandAndOrderStep);


//        gameState.setGamePhase(BattleloreGameState.BattleloreGamePhase.OrderStep);

  //      gameState.setGamePhase(BattleloreGameState.BattleloreGamePhase.MoveStep);
    //    gameState.setGamePhase(BattleloreGameState.BattleloreGamePhase.AttackStep);
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action)
    {
        BattleloreGameState state = (BattleloreGameState) currentState;
        BattleloreGameParameters gameParams = (BattleloreGameParameters) currentState.getGameParameters();
        action.execute(currentState);//todo

        int playerId = state.getCurrentPlayer();
        Unit.Faction playerFaction = playerId == Unit.Faction.Dakhan_Lords.ordinal() ? Unit.Faction.Dakhan_Lords : Unit.Faction.Uthuk_Yllan;

        switch (state.getGamePhase().toString())
        {
            case "CommandAndOrderStep":
                currentState.setGamePhase(BattleloreGameState.BattleloreGamePhase.MoveStep);
                break;
            case "MoveStep":
                if (state.GetMoveableUnitsFromTile(playerFaction).isEmpty())
                {
                    currentState.setGamePhase(BattleloreGameState.BattleloreGamePhase.AttackStep);
                }
                break;
            case "AttackStep":
                if (state.GetReadyForAttackUnitsFromTile(playerFaction).isEmpty())
                {
                    //state.getTurnOrder().endPlayerTurn(state);
                    int player = state.getCurrentPlayer();
                    state.getTurnOrder().endPlayerTurn(state);

                    currentState.setGamePhase(BattleloreGameState.BattleloreGamePhase.CommandAndOrderStep);
                }
                break;

            /*
            case "VictoryPointStep":
                break;
            case "DrawStep":
                break;
            case "LoreStep":
                break;
            */
            default:
                break;
        }

        if (checkGameEnd((BattleloreGameState) currentState, playerId))
        {

            currentState.setGameStatus(Utils.GameResult.GAME_END);
            //Unit.Faction playerFaction = playerId == Unit.Faction.Dakhan_Lords.ordinal() ? Unit.Faction.Dakhan_Lords : Unit.Faction.Uthuk_Yllan;
            registerWinner(state, playerId);
            return;
        }

        //currentState.getTurnOrder().endPlayerTurn(currentState);
    }



    private void PutLearningScenarioUnits(BattleloreGameState gameState)
    {
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
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState)
    {
        BattleloreGameState state = (BattleloreGameState) gameState;
        int player = gameState.getTurnOrder().getCurrentPlayer(gameState);
        Unit.Faction playerFaction = player == Unit.Faction.Dakhan_Lords.ordinal() ? Unit.Faction.Dakhan_Lords : Unit.Faction.Uthuk_Yllan;

        ArrayList<AbstractAction> actions = new ArrayList<>();
        IGamePhase a = gameState.getGamePhase();

        if (gameState.getGamePhase() == BattleloreGameState.BattleloreGamePhase.CommandAndOrderStep)
        {
            actions.add(new PlayCommandCardAction(CommandCard.CommandType.AttackRight, playerFaction, player));
            actions.add(new PlayCommandCardAction(CommandCard.CommandType.BattleMarch, playerFaction, player));
            actions.add(new PlayCommandCardAction(CommandCard.CommandType.PatrolLeft, playerFaction, player));
        }
        if (gameState.getGamePhase() == BattleloreGameState.BattleloreGamePhase.MoveStep)
        {
            ArrayList<MapTile> moveableUnitTiles = state.GetMoveableUnitsFromTile(playerFaction);
            int[][] possibleLocations = new int[state.getBoard().getWidth()][2];

            if (!moveableUnitTiles.isEmpty())
            {
                for (MapTile tile : moveableUnitTiles)
                {
                    possibleLocations = state.GetPossibleLocationsForUnits(tile);
                    //check possible locations size
                    for (int i = 0 ; i< state.getBoard().getWidth(); i++)
                    {
                        if (possibleLocations[i][0] != -1 || possibleLocations[i][1] != -1)
                        {
                            actions.add(new MoveUnitsAction(tile, playerFaction, possibleLocations[i][0], possibleLocations[i][1]));
                        }
                    }
                    if (actions.isEmpty())
                    {
                        actions.add(new SkipTurnAction(tile, playerFaction, true, false));
                    }
                }
            }

            //if moveable unit count has reached the end, finish the state
        }
        if (gameState.getGamePhase() == BattleloreGameState.BattleloreGamePhase.AttackStep)
        {
            ArrayList<MapTile> readyToAttackUnits = state.GetReadyForAttackUnitsFromTile(playerFaction);
            int[][] possibleLocations = new int[state.getBoard().getWidth()][2];

            if (!readyToAttackUnits.isEmpty())
            {
                for (MapTile attacker : readyToAttackUnits)
                {
                    possibleLocations = state.GetPossibleTargetUnits(attacker);

                    for (int i = 0 ; i< state.getBoard().getWidth(); i++)
                    {
                        if (possibleLocations[i][0] != -1 || possibleLocations[i][1] != -1)
                        {
                            actions.add(new AttackUnitsAction(attacker, state.getBoard().getElement(possibleLocations[i][0], possibleLocations[i][1]), attacker.GetFaction(), player));
                        }
                    }
                    if (actions.isEmpty())
                    {
                        actions.add(new SkipTurnAction(attacker, playerFaction, false, true));
                    }
                }
            }
        }

        //registerWinner(state, new Token("winner is: " + player));
    if (actions.isEmpty())
    {
        int i = 0;
        actions.add(new SkipTurnAction());
    }
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy()
    {
        return new BattleloreForwardModel();
    }


    /**
     * Checks if the game ended.
     * @param gameState - game state to check game end.
     */
    private boolean checkGameEnd(BattleloreGameState gameState, int playerId)
    {
        return gameState.GetPlayerScore(playerId) > 5;
    }

    @Override
    protected void endGame(AbstractGameState gameState)
    {
        if (VERBOSE)
        {
            System.out.println(Arrays.toString(gameState.getPlayerResults()));
        }
    }


    private void registerWinner(BattleloreGameState gameState, int winnerD)
    {
        gameState.setGameStatus(Utils.GameResult.GAME_END);
        //int winningPlayer = BattleloreConstants //.playerMapping.indexOf(winnerSymbol);
        gameState.setPlayerResult(Utils.GameResult.WIN, winnerD);
        gameState.setPlayerResult(Utils.GameResult.LOSE, winnerD == 0 ? 1 : 0);
    }
}
