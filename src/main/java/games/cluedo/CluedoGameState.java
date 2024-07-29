package games.cluedo;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.*;
import core.interfaces.IGamePhase;
import games.GameType;
import games.cluedo.cards.CluedoCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class CluedoGameState extends AbstractGameState {

    GraphBoard gameBoard;

    // Map of playerId to character index
    // Character index is defined by 0:Scarlett, 1:Mustard, 2:Orchid etc. in turn order
    public HashMap<Integer, Integer> characterToPlayerMap = new HashMap<>();
    // Room names where each character is at, index corresponds to character index as defined above
    List<String> characterLocations;

    // All cards in the game
    // (useful for implementing suggestions; only have to search through one deck to find the card you're suggesting)
    PartialObservableDeck<CluedoCard> allCards;
    // Cards in each player's hand, index corresponds to playerId
    List<PartialObservableDeck<CluedoCard>> playerHandCards;
    // Cards in the case file
    PartialObservableDeck<CluedoCard> caseFile;
    // Suggestion being made by current player
    public PartialObservableDeck<CluedoCard> currentGuess;

    CluedoTurnOrder turnOrder;
    // ID of player whose turn it is (ie the player who made the suggestion)
    int currentTurnPlayerId; // TODO this should be turnOwner; turnOwner should be currentPlayer

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers - number of players
     */
    public CluedoGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        turnOrder = _createTurnOrder(nPlayers);
    }

    protected CluedoTurnOrder _createTurnOrder(int nPlayers){
        return new CluedoTurnOrder(nPlayers);
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

        copy.gameBoard = gameBoard.copy();
        copy.characterToPlayerMap = new HashMap<>();
        copy.characterLocations = new ArrayList<>();
        copy.allCards = allCards.copy();
        copy.playerHandCards = new ArrayList<>();
        copy.caseFile = caseFile.copy();
        copy.currentGuess = currentGuess.copy();
        copy.turnOrder = (CluedoTurnOrder) turnOrder.copy();
        copy.currentTurnPlayerId = currentTurnPlayerId;

        for (int i = 0; i < 6; i++) {
            if (characterToPlayerMap.containsKey(i)) {
                copy.characterToPlayerMap.put(i, characterToPlayerMap.get(i));
            }
            copy.characterLocations.add(characterLocations.get(i));
            if (i < nPlayers) {
                copy.playerHandCards.add(playerHandCards.get(i).copy());
            }
        }

        if (playerId != -1) {
            // Add all cards that are not visible to the current player into a deck
            Deck<CluedoCard> unknownCards = new Deck<>("Unknown Cards", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    PartialObservableDeck<CluedoCard> playerHand = playerHandCards.get(i);
                    for (int j=0; j<playerHand.getSize(); j++) {
                        if (!playerHand.getVisibilityForPlayer(j, playerId)) {
                            unknownCards.add((CluedoCard) playerHand.get(j).copy());
                        }
                    }
                }
            }
            unknownCards.add(caseFile);

            // Shuffle deck
            unknownCards.shuffle(redeterminisationRnd);

            // Redistribute shuffled cards
            copy.caseFile.clear();
            copy.caseFile.add(unknownCards.draw());
            copy.caseFile.add(unknownCards.draw());
            copy.caseFile.add(unknownCards.draw());

            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    PartialObservableDeck<CluedoCard> playerHand = playerHandCards.get(i);
                    for (int j=0; j<playerHand.getSize(); j++) {
                        if (!playerHand.getVisibilityForPlayer(j, playerId)) {
                            playerHand.setComponent(j, unknownCards.draw());
                        }
                    }
                }
            }
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
        return Objects.equals(gameBoard, that.gameBoard)
                && Objects.equals(characterToPlayerMap, that.characterToPlayerMap)
                && Objects.equals(characterLocations, that.characterLocations)
                && Objects.equals(allCards, that.allCards)
                && Objects.equals(playerHandCards, that.playerHandCards)
                && Objects.equals(caseFile, that.caseFile)
                && Objects.equals(currentGuess, that.currentGuess)
                && Objects.equals(turnOrder, that.turnOrder)
                && Objects.equals(currentTurnPlayerId, that.currentTurnPlayerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameBoard, characterToPlayerMap, characterLocations, allCards, playerHandCards,
                caseFile, currentGuess, turnOrder, currentTurnPlayerId) + 31 * super.hashCode();
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

}
