package games.cluedo;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.*;
import core.interfaces.IGamePhase;
import core.turnorders.ReactiveTurnOrder;
import games.GameType;
import games.cluedo.cards.CluedoCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class CluedoGameState extends AbstractGameState {

    // Map of playerId to character index
    // Character index is defined by 0:Scarlett, 1:Mustard, 2:Orchid etc. in turn order
    public HashMap<Integer, Integer> characterToPlayerMap = new HashMap<>();
    // Room (Node) where each character is at, index corresponds to character index as defined above
    List<BoardNode> characterLocations;

    // All cards in the game
    // (useful for implementing suggestions; only have to search through one deck to find the card you're suggesting)
    PartialObservableDeck<CluedoCard> allCards;
    // Cards in each player's hand, index corresponds to playerId
    List<PartialObservableDeck<CluedoCard>> playerHandCards;
    // Cards in the case file
    PartialObservableDeck<CluedoCard> caseFile;
    // Suggestion being made by current player
    public PartialObservableDeck<CluedoCard> currentGuess;

    GraphBoard gameBoard;

    int currentTurnPlayerId;

    ReactiveTurnOrder turnOrder;

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers - number of players
     */
    public CluedoGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        turnOrder = _createTurnOrder(nPlayers);
    }

    protected ReactiveTurnOrder _createTurnOrder(int nPlayers){
        return new ReactiveTurnOrder(nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Cluedo;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>() {{
            add(caseFile);
            add(gameBoard);
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        CluedoGameState copy = new CluedoGameState(gameParameters, playerId);

        copy.allCards = allCards.copy();
        copy.characterToPlayerMap = new HashMap<>();
        copy.playerHandCards = new ArrayList<>();
        copy.caseFile = caseFile.copy();
        copy.currentGuess = currentGuess.copy();
        copy.gameBoard = gameBoard.copy();
        copy.characterLocations = new ArrayList<>();
        copy.currentTurnPlayerId = currentTurnPlayerId;
        copy.turnOrder = (ReactiveTurnOrder) turnOrder.copy();

        for (int i = 0; i < nPlayers; i++) {
            copy.characterToPlayerMap.put(i, characterToPlayerMap.get(i));
            copy.playerHandCards.add(playerHandCards.get(i).copy());
            copy.characterLocations.add(characterLocations.get(i).copy());
        }

        if (playerId != -1) { // TODO shuffle other players' cards + caseFile, then redistribute

        }

        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) { // TODO Heuristic Score
        return 0;
    }

    @Override
    public double getGameScore(int playerId) { // TODO Game Score
        return 0;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CluedoGameState that)) return false;
        return Objects.equals(characterToPlayerMap, that.characterToPlayerMap)
                && Objects.equals(allCards, that.allCards)
                && Objects.equals(playerHandCards, that.playerHandCards)
                && Objects.equals(caseFile, that.caseFile)
                && Objects.equals(currentGuess, that.currentGuess)
                && Objects.equals(gameBoard, that.gameBoard)
                && Objects.equals(characterLocations, that.characterLocations)
                && Objects.equals(currentTurnPlayerId, that.currentTurnPlayerId)
                && Objects.equals(turnOrder, that.turnOrder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(characterToPlayerMap,allCards, playerHandCards, caseFile, currentGuess,
                            gameBoard, characterLocations, currentTurnPlayerId, turnOrder) + 31 * super.hashCode();
    }

    public enum CluedoGamePhase implements IGamePhase {
        chooseCharacter,
        makeSuggestion,
        revealCards,
        makeAccusation
    }

    public List<PartialObservableDeck<CluedoCard>> getPlayerHandCards() {
        return playerHandCards;
    }

    public PartialObservableDeck<CluedoCard> getAllCards() {
        return allCards;
    }

    public ReactiveTurnOrder getTurnOrder() {
        return turnOrder;
    }

}
