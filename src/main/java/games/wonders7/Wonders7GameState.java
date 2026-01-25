package games.wonders7;

import core.AbstractGameState;
import core.AbstractParameters;
import core.actions.AbstractAction;
import core.components.Component;
import core.components.Deck;
import games.GameType;
import games.wonders7.actions.BuildFromDiscard;
import games.wonders7.cards.Wonder7Card;
import games.wonders7.cards.Wonder7Board;

import java.util.*;
import java.util.stream.Collectors;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static games.wonders7.Wonders7Constants.Resource.*;
import static games.wonders7.cards.Wonder7Card.CardType.*;

public class Wonders7GameState extends AbstractGameState {

    int currentAge; // int from 1,2,3 of current age
    List<EnumMap<Wonders7Constants.Resource, Integer>> playerResources; // Each player's full resource counts
    List<Deck<Wonder7Card>> playerHands; // Player Hands
    List<Deck<Wonder7Card>> playedCards; // Player used cards
    Deck<Wonder7Card> ageDeck; // The 'draw deck' for the Age
    Deck<Wonder7Card> discardPile; // Discarded cards
    Deck<Wonder7Board> wonderBoardDeck; // The deck of wonder board that decide a players wonder
    Wonder7Board[] playerWonderBoard; // Every player's assigned Wonder Board
    AbstractAction[] turnActions; // The round's actions chosen by each player

    public int direction;

    protected Random cardRnd;

    public Wonders7GameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);

        // Each player starts off with no resources
        playerResources = new ArrayList<>(); // New arraylist , containing different hashmaps for each player
        for (int i = 0; i < getNPlayers(); i++) {
            playerResources.add(new EnumMap<>(Wonders7Constants.Resource.class)); // Adds a new hashmap for each player
        }
    }

    @Override
    protected void reset() {
        super.reset();
        // if either wonder or card distribution seeds are set to something other than -1,
        // then this seed is fixed. The game random seed will be used in all cases where these are -1 (the default)
        Wonders7GameParameters params = (Wonders7GameParameters) gameParameters;
        cardRnd = params.cardShuffleSeed == -1 ? rnd : new Random(params.cardShuffleSeed);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Wonders7;
    }

    public List<Component> _getAllComponents() {
        // return a list of all (parents, and those nested as well) components in your game state. The method is called after game setup, so you may assume all components are already created. Decks and Areas have all of their nested components automatically added.
        return new ArrayList<Component>() {{
            addAll(playerHands);
            addAll(playedCards);
            add(discardPile);
        }};
        //return new ArrayList<>();
    }

    public AbstractGameState _copy(int playerId) {
        // define a reduced, player-specific, copy of your game state
        // Including components that player with the given ID will see.
        // For example, some decks may be face down and unobservable to the player
        // All the components in the observation should be copies of those in the game state
        Wonders7GameState copy = new Wonders7GameState(gameParameters.copy(), getNPlayers());
        //Wonders7TurnOrder turnOrder = new Wonders7TurnOrder(getNPlayers());
        copy.playerResources = new ArrayList<>();
        copy.playerHands = new ArrayList<>();
        copy.playedCards = new ArrayList<>();
        copy.playerWonderBoard = new Wonder7Board[getNPlayers()];
        copy.turnActions = new AbstractAction[getNPlayers()];
        for (int i = 0; i < getNPlayers(); i++) {
            if (turnActions[i] != null)
                copy.turnActions[i] = turnActions[i].copy();
        }

        for (Map<Wonders7Constants.Resource, Integer> map : playerResources) {
            copy.playerResources.add(new EnumMap<>(map));
        }
        for (Deck<Wonder7Card> deck : playerHands) {
            copy.playerHands.add(deck.copy());
        }
        for (Deck<Wonder7Card> deck : playedCards) {
            copy.playedCards.add(deck.copy());
        }
        for (int i = 0; i < getNPlayers(); i++) {
            copy.playerWonderBoard[i] = playerWonderBoard[i].copy();  // Every player's wonder boards are known
        }
        copy.ageDeck = ageDeck.copy();
        copy.discardPile = discardPile.copy();
        copy.currentAge = currentAge;
        copy.direction = direction;
        copy.wonderBoardDeck = wonderBoardDeck.copy();
        copy.cardRnd = new Random(redeterminisationRnd.nextLong());

        if (getCoreGameParameters().partialObservable && playerId != -1) {
            // Seven Wonders does not use PartialObservableDecks
            // However a player knows the cards in the hands of players now holding hands that the player used to have
            // (there is one exception to this, in that any card used to build a wonder from the hand of cards is not known)
            // (we ignore this exception for now; as this is a small effect compared to know cards other players mostly have)


            for (int i = 0; i < getNPlayers(); i++) {
                if (!isHandKnownTo(playerId, i)) {
                    copy.ageDeck.add(copy.playerHands.get(i));
                    // Groups other players cards (except for next players hand) into the ageDeck
                    // (along with any cards that were not in the game at that age)
                }
            }
            // we extract the contents of the discard pile from previous ages
            Map<Boolean, List<Wonder7Card>> previousAgeDiscards = copy.discardPile.stream()
                    .collect(Collectors.partitioningBy(c -> c.cardType.age == copy.currentAge));

            // if the perspective player is building from the discard pile, then they know the contents of the discard pile
            // TODO: At some point convert SevenWonders to use PartialObservableDecks; and then remove this special case
            // TODO: Also, formally the cards played facedown by other players to build Wonders in given ages should be included in this reshuffle
            if (currentActionInProgress() instanceof BuildFromDiscard && getCurrentPlayer() == playerId) {
                previousAgeDiscards.get(false).addAll(previousAgeDiscards.get(true));
                previousAgeDiscards.get(true).clear();
                // this will ensure that the discard pile is not touched
            }

            copy.ageDeck.add(previousAgeDiscards.get(true)); // Groups the same age cards from the discard pile into the ageDeck
            copy.ageDeck.shuffle(redeterminisationRnd); // Shuffle all the cards
            for (int i = 0; i < getNPlayers(); i++) {
                if (!isHandKnownTo(playerId, i)) {
                    Deck<Wonder7Card> hand = copy.playerHands.get(i);
                    int nCards = hand.getSize();
                    hand.clear();  // Empties the accurate player hands, except for the next players hand
                    for (int j = 0; j < nCards; j++) {
                        hand.add(copy.ageDeck.draw());  // Fills player hand from shuffled cards
                    }
                }
            }
            copy.discardPile.clear(); // Empties the accurate pile
            copy.discardPile.add(previousAgeDiscards.get(false)); // Adds the different age cards back to the pile
            while(copy.discardPile.getSize() < this.discardPile.getSize()) {
                copy.discardPile.add(copy.ageDeck.draw()); // Fills the pile with the remaining shuffled cards in the ageDeck
            }
            copy.turnActions = new AbstractAction[getNPlayers()];
            if (turnActions[playerId] != null)
                copy.turnActions[playerId] = turnActions[playerId].copy();
            // we know our action (if one has been chosen, but no one elses)
        }
        return copy;
    }

    /**
     * we do know the contents of the hands of players up to T to our left or right, where T
     * is the number of cards we have played.
     *
     * @param playerId   - id of player whose vision we're checking
     * @param opponentId - id of opponent owning the hand of cards we're checking vision of
     * @return - true if player has seen the opponent's hand of cards, false otherwise
     */
    public boolean isHandKnownTo(int playerId, int opponentId) {
        if (playerId == opponentId) return true;  // always know your own hand
        Wonders7GameParameters params = (Wonders7GameParameters) gameParameters;
        // a player knows a number of other hands equal to the number of cards they have played
        int handsKnown = params.nWonderCardsPerPlayer - playerHands.get(playerId).getSize();

        // 'Left' means we pass to lower numbered players, 'Right' means we pass to higher numbered players
        int opponentSpacesToLeft = (playerId - opponentId + getNPlayers()) % getNPlayers();
        int opponentSpacesToRight = (opponentId - playerId + getNPlayers()) % getNPlayers();

        if (direction == 1) {  // this passes to lower numbered players
            return handsKnown >= opponentSpacesToLeft;
        } else  // this passes to higher numbered players
            return handsKnown >= opponentSpacesToRight;
    }

    @Override
    public double _getHeuristicScore(int playerId) {
        // Implement a rough-and-ready heuristic (or a very sophisticated one)
        // that gives an estimate of how well a player is doing in the range [-1, +1], 
        // where -1 is immediate loss, and +1 is immediate win
        // This is used by a number of agents as a default, including MCTS, to value the current state. If the game has a direct score, then the simplest approach here is just to scale this in line with some plausible maximum
        // see DominionGameState._getHeuristicScore() for an example of this; and contrast to DominionHeuristic for a more sophisticated approach
        return new Wonders7Heuristic().evaluateState(this, playerId);
    }

    protected void updateEndOfAgeMilitaryVPs() {
        for (int player = 0; player < nPlayers; player++) {
            int nextplayer = (player + 1) % getNPlayers();
            if (playerResources.get(player).get(Shield) > playerResources.get(nextplayer).get(Shield)) {
                // IF PLAYER WINS
                playerResources.get(player).put(Victory, playerResources.get(player).get(Victory) + (2 * currentAge - 1)); // 2N-1 POINTS FOR PLAYER i
                playerResources.get(nextplayer).put(Victory, playerResources.get(nextplayer).get(Victory) - 1); // -1 FOR THE PLAYER i+1
            } else if (playerResources.get(player).get(Shield) < playerResources.get(nextplayer).get(Shield)) { // IF PLAYER i+1 WINS
                playerResources.get(player).put(Victory, playerResources.get(player).get(Victory) - 1);// -1 POINT FOR THE PLAYER i
                playerResources.get(nextplayer).put(Victory, playerResources.get(nextplayer).get(Victory) + (2 * currentAge - 1));// 2N-1 POINTS FOR PLAYER i+1
            }
        }
    }

    @Override
    public double getGameScore(int playerId) {
        // return the players score for the current game state.
        // this just looks at current VP (including science based on evaluation now)
        int vp = playerResources.get(playerId).get(Victory);
        // Treasury
        vp += playerResources.get(playerId).get(Wonders7Constants.Resource.Coin) / 3;
        // Scientific
        vp += getSciencePoints(playerId);

        // then consider the endgame effects of cards
        for (Wonder7Card card : playedCards.get(playerId)) {
            vp += card.endGameVP(this, playerId);
        }
        return vp;
    }

    public int getSciencePoints(int player) {
        int wild = playerResources.get(player).get(ScienceWild);
        int cog = playerResources.get(player).get(Cog);
        int compass = playerResources.get(player).get(Compass);
        int tablet = playerResources.get(player).get(Tablet);

        return sciencePoints(cog, compass, tablet, wild);
    }

    private int sciencePoints(int cog, int compass, int tablet, int wild) {
        if (wild == 0)
            return cog * cog + compass * compass + tablet * tablet + 7 * Math.min(Math.min(cog, compass), tablet);

        // otherwise we recursively consider all possible allocations of the wild
        int maxPoints = 0;
        for (int i = 0; i <= 3; i++) {
            int points = sciencePoints(
                    cog + (i == 0 ? 1 : 0),
                    compass + (i == 1 ? 1 : 0),
                    tablet + (i == 2 ? 1 : 0),
                    wild - 1);
            if (points > maxPoints)
                maxPoints = points;
        }
        return maxPoints;
    }

    public int cardsOfType(Wonder7Card.Type type, int player) {
        return (int) playedCards.get(player).getComponents().stream().filter(c -> c.type == type).count();
    }

    public Wonders7GameParameters getParams() {
        return (Wonders7GameParameters) gameParameters;
    }

    public int getCurrentAge() {
        return currentAge;
    }

    public Deck<Wonder7Card> getAgeDeck() {
        return ageDeck;
    }

    public Deck<Wonder7Card> getPlayerHand(int index) {
        return playerHands.get(index);
    } // Get player hand

    public List<Deck<Wonder7Card>> getPlayerHands() {
        return playerHands;
    }

    // Cards played to the specified player's tableau
    public Deck<Wonder7Card> getPlayedCards(int playerId) {
        return playedCards.get(playerId);
    }

    public Deck<Wonder7Card> getDiscardPile() {
        return discardPile;
    }

    AbstractAction getTurnAction(int index) {
        return turnActions[index];
    }

    public void setTurnAction(int index, AbstractAction action) {
        turnActions[index] = action;
    }

    public Wonder7Board getPlayerWonderBoard(int playerId) {
        return playerWonderBoard[playerId];
    }

    public void setPlayerWonderBoard(int playerId, Wonder7Board wonder) {
        playerWonderBoard[playerId] = wonder;
    }

    public Wonder7Card findCardIn(Deck<Wonder7Card> deck, Wonder7Card.CardType cardType) {
        Wonder7Card card = null;
        for (Wonder7Card cardSearch : deck.getComponents()) { // Goes through each card in the deck
            if (cardType == cardSearch.cardType) { // If cardName is the one searching for (being played)
                card = cardSearch;
                break;
            }
        }

        if (card == null) {
            throw new AssertionError("Card not found in deck");
        }
        return card;
    }

    public Wonder7Card findCardInHand(int player, Wonder7Card.CardType cardType) {
        return findCardIn(playerHands.get(player), cardType);
    }
    public Wonder7Card findCardInDiscard(Wonder7Card.CardType cardType) {
        return findCardIn(discardPile, cardType);
    }


    // A summary Map of all the resources a player has from their played cards and Wonder Board
    public Map<Wonders7Constants.Resource, Integer> getPlayerResources(int playerId) {
        return playerResources.get(playerId);
    } // Return players resource map

    /**
     * This returns the cost for buyer to acquire resource from seller
     * This does not check to see if the resource is available, only the cost
     */
    public int costOfResource(Wonders7Constants.Resource resource, int buyer, int seller) {
        // For the moment we'll hardcode the Marketplace and TradingPost functionality here (it is not used elsewhere)
        // In the future this can be replaced with a more general mechanism
        if (resource.isRare()) {
            if (playedCards.get(buyer).stream().anyMatch(c -> c.cardType == Marketplace)) {
                return getParams().nCostDiscountedResource;
            }
        } else if (resource.isBasic()) {
            if (seller == (buyer - 1 + nPlayers) % nPlayers && playedCards.get(buyer).stream().anyMatch(c -> c.cardType == EastTradingPost)) {
                return getParams().nCostDiscountedResource;
            }
            if (seller == (buyer + 1 + nPlayers) % nPlayers && playedCards.get(buyer).stream().anyMatch(c -> c.cardType == WestTradingPost)) {
                return getParams().nCostDiscountedResource;
            }
        }
        return getParams().nCostNeighbourResource;
    }

    // The number of Resources of the specified type a player has
    public int getResource(int player, Wonders7Constants.Resource resource) {
        return playerResources.get(player).get(resource);
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wonders7GameState)) return false;
        if (!super.equals(o)) return false;
        Wonders7GameState that = (Wonders7GameState) o;
        return currentAge == that.currentAge && direction == that.direction && Objects.equals(playerResources, that.playerResources) &&
                Objects.equals(playerHands, that.playerHands) && Objects.equals(playedCards, that.playedCards) &&
                Objects.equals(ageDeck, that.ageDeck) &&
                Objects.equals(discardPile, that.discardPile) && Objects.equals(wonderBoardDeck, that.wonderBoardDeck) &&
                Arrays.equals(playerWonderBoard, that.playerWonderBoard) && Arrays.equals(turnActions, that.turnActions);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), currentAge, playerResources, playerHands, playedCards, ageDeck, discardPile, wonderBoardDeck, direction);
        result = 31 * result + Arrays.hashCode(playerWonderBoard);
        result = 31 * 31 * result + Arrays.hashCode(turnActions);
        return result;
    }

    public int getDirection() {
        return direction;
    }

    public void reverse() {
        direction = -direction;
    }

    @Override
    public String toString() {
        return gameParameters.hashCode() + "|" +
                gameStatus.hashCode() + "|" +
                gamePhase.hashCode() + "|" +
                Arrays.hashCode(playerResults) + "|*|" +
                playerResources.hashCode() + "|" +
                playerHands.hashCode() + "|" +
                playedCards.hashCode() + "|" +
                ageDeck.hashCode() + "|" +
                discardPile.hashCode() + "|" +
                wonderBoardDeck.hashCode() + "|" +
                Arrays.hashCode(playerWonderBoard) + "|" +
                Arrays.hashCode(turnActions) + "|" +
                currentAge + "|" +
                direction + "|";
    }
}