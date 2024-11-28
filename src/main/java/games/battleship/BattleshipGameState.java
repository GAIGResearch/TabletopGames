package games.battleship;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.components.Token;
import core.interfaces.IGridGameState;
import core.interfaces.IPrintable;
import games.GameType;

import java.util.ArrayList;
import java.util.List;

public class BattleshipGameState extends AbstractGameState implements IPrintable {

    GridBoard<Token> playerOneShipBoard;
    GridBoard<Token> playerOneHitBoard;
    GridBoard<Token> playerTwoShipBoard;
    GridBoard<Token> playerTwoHitBoard;
    public BattleshipGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        playerOneShipBoard = null;
        playerOneHitBoard = null;
        playerTwoShipBoard = null;
        playerTwoHitBoard = null;
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Battleship;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            add(playerOneShipBoard);
            add(playerOneHitBoard);
            add(playerTwoShipBoard);
            add(playerTwoHitBoard);
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        BattleshipGameState bgs =  new BattleshipGameState(gameParameters.copy(), getNPlayers());
        bgs.playerOneShipBoard = playerOneShipBoard.copy();
        bgs.playerOneHitBoard = playerOneHitBoard.copy();
        bgs.playerTwoShipBoard = playerTwoShipBoard.copy();
        bgs.playerTwoHitBoard = playerTwoHitBoard.copy();
        return bgs;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return 0;
    }

    @Override
    public double getGameScore(int playerId) {
        return playerResults[playerId].value;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BattleshipGameState)) return false;
        BattleshipGameState that = (BattleshipGameState) o;
        return playerOneShipBoard.equals(that.playerOneShipBoard) &&
                playerOneHitBoard.equals(that.playerOneHitBoard) &&
                playerTwoShipBoard.equals(that.playerTwoShipBoard) &&
                playerTwoHitBoard.equals(that.playerTwoHitBoard);
    }

    public GridBoard<Token> getPlayerOneShipBoard() {
        return playerOneShipBoard;
    }
    public GridBoard<Token> getPlayerOneHitBoard() {
        return playerOneHitBoard;
    }
    public GridBoard<Token> getPlayerTwoShipBoard() {
        return playerTwoShipBoard;
    }
    public GridBoard<Token> getPlayerTwoHitBoard() {
        return playerTwoHitBoard;
    }

    @Override
    public void printToConsole() {
        System.out.println("Player 1: \nHit Board: ");
        System.out.println(playerOneHitBoard.toString());
        System.out.println("Ship Board: ");
        System.out.println(playerOneShipBoard.toString());
        System.out.println("---\nPlayer 2: \n Hit Board: ");
        System.out.println(playerTwoHitBoard.toString());
        System.out.println("Ship Board: ");
    }
}
