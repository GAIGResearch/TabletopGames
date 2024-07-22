package games.cluedo;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.*;
import games.GameType;
import games.cluedo.cards.CluedoCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CluedoGameState extends AbstractGameState {

    // Cards in each player's hand, index corresponds to player ID
    List<PartialObservableDeck<CluedoCard>> playerHandCards;
    // Cards in the case file
    PartialObservableDeck<CluedoCard> caseFile;

    GraphBoard gameBoard;

    // Room (Node) where each player is at, index corresponds to player ID
    List<BoardNode> playerLocations;

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers
     */
    public CluedoGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Cluedo;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            add(caseFile);
            add(gameBoard);
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        CluedoParameters cp = (CluedoParameters) getGameParameters();
        CluedoGameState copy = new CluedoGameState(gameParameters, playerId);

        copy.playerHandCards = new ArrayList<>();
        copy.caseFile = caseFile.copy();
        copy.gameBoard = gameBoard.copy();
        copy.playerLocations = new ArrayList<>();
        for (int i = 0; i < nPlayers; i++) {
            copy.playerHandCards.add(playerHandCards.get(i).copy());
            copy.playerLocations.add(playerLocations.get(i).copy());
        }

        if (playerId != -1) { // TODO
            // shuffle other players' cards + caseFile, then redistribute
        }

        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) { // TODO
        return 0;
    }

    @Override
    public double getGameScore(int playerId) { // TODO
        return 0;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CluedoGameState that)) return false;
        return Objects.equals(playerHandCards, that.playerHandCards)
                && Objects.equals(caseFile, that.caseFile)
                && Objects.equals(gameBoard, that.gameBoard)
                && Objects.equals(playerLocations, that.playerLocations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerHandCards, caseFile, gameBoard, playerLocations) + 31 * super.hashCode();
    }

}
