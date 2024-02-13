package games.wonders7;

import core.AbstractGameState;
import core.AbstractParameters;
import core.actions.AbstractAction;
import core.components.Component;
import core.components.Deck;
import games.GameType;
import games.wonders7.cards.Wonder7Card;
import games.wonders7.cards.Wonder7Board;

import java.util.*;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;

public class Wonders7GameState extends AbstractGameState {

    int currentAge; // int from 1,2,3 of current age
    List<HashMap<Wonders7Constants.Resource, Integer>> playerResources; // Each player's full resource counts
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
            playerResources.add(new HashMap<>());
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

        for (HashMap<Wonders7Constants.Resource, Integer> map : playerResources) {
            copy.playerResources.add(new HashMap<>(map));
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
        copy.cardRnd = new Random(redeterminisationRnd.nextInt());

        if (getCoreGameParameters().partialObservable && playerId != -1) {
            // Player does not know the other players hands and discard pile (except for next players hand)
            // All the cards of other players and discard pile are shuffled
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    copy.ageDeck.add(copy.playerHands.get(i)); // Groups other players cards (except for next players hand) into the ageDeck (along with any cards that were not in the game at that age)
                }
            }
            copy.ageDeck.add(copy.discardPile); // Groups the discard pile into the ageDeck
            copy.ageDeck.shuffle(redeterminisationRnd); // Shuffle all the cards
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    Deck<Wonder7Card> hand = copy.playerHands.get(i);
                    int nCards = hand.getSize();
                    hand.clear();  // Empties the accurate player hands, except for the next players hand
                    for (int j = 0; j < nCards; j++) {
                        hand.add(copy.ageDeck.draw());  // Fills player hand from shuffled cards
                    }
                }
            }
            Deck<Wonder7Card> discPile = copy.discardPile;
            int nDisc = discPile.getSize();
            discPile.clear(); // Empties the accurate pile
            for (int i = 0; i < nDisc; i++) {
                discPile.add(copy.ageDeck.draw()); // Fills the pile with the remaining shuffled cards in the ageDeck
            }
            copy.turnActions = new AbstractAction[getNPlayers()];
            if (turnActions[playerId] != null)
                copy.turnActions[playerId] = turnActions[playerId].copy();
            // we know our action (if one has been chosen, but no one elses
        }
        return copy;
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

    @Override
    public double getGameScore(int playerId) {
        // return the players score for the current game state.
        // This may not apply for all games
        List<HashMap<Wonders7Constants.Resource, Integer>> playerResourcesCopy = new ArrayList<>();
        for (HashMap<Wonders7Constants.Resource, Integer> map : playerResources) {
            playerResourcesCopy.add(new HashMap<>(map));
        }
        // Evaluate military conflicts
        int nextplayer = (playerId + 1) % getNPlayers();
        if (playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Shield) > playerResourcesCopy.get(nextplayer).get(Wonders7Constants.Resource.Shield)) { // IF PLAYER i WINS
            playerResourcesCopy.get(playerId).put(Wonders7Constants.Resource.Victory, playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Victory) + (2 * currentAge - 1)); // 2N-1 POINTS FOR PLAYER i
            playerResourcesCopy.get(nextplayer).put(Wonders7Constants.Resource.Victory, playerResourcesCopy.get(nextplayer).get(Wonders7Constants.Resource.Victory) - 1); // -1 FOR THE PLAYER i+1
        } else if (playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Shield) < playerResourcesCopy.get(nextplayer).get(Wonders7Constants.Resource.Shield)) { // IF PLAYER i+1 WINS
            playerResourcesCopy.get(playerId).put(Wonders7Constants.Resource.Victory, playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Victory) - 1);// -1 POINT FOR THE PLAYER i
            playerResourcesCopy.get(nextplayer).put(Wonders7Constants.Resource.Victory, playerResourcesCopy.get(nextplayer).get(Wonders7Constants.Resource.Victory) + (2 * currentAge - 1));// 2N-1 POINTS FOR PLAYER i+1
        }

        int vp = playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Victory);
        // Treasury
        vp += playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Coin) / 3;
        // Scientific
        vp += (int) Math.pow(playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Cog), 2);
        vp += (int) Math.pow(playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Compass), 2);
        vp += (int) Math.pow(playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Tablet), 2);
        // Sets of different science symbols
        vp += 7 * Math.min(Math.min(playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Cog), playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Compass)), playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Tablet));

        playerResourcesCopy.get(playerId).put(Wonders7Constants.Resource.Victory, vp);
        return playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Victory);
    }


    public int cardsOfType(Wonder7Card.Type type) {
        Deck<Wonder7Card> allCards;
        allCards = new Deck<>("temp", VISIBLE_TO_ALL);
        for (int i = 0; i < getNPlayers(); i++) allCards.add(getPlayedCards(i));
        return (int) allCards.stream().filter(c -> c.getCardType() == type).count();
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

    public Deck<Wonder7Card> getPlayedCards(int index) {
        return playedCards.get(index);
    } // Get player 'played' cards

    public Deck<Wonder7Card> getDiscardPile() {
        return discardPile;
    }

    public AbstractAction getTurnAction(int index) {
        return turnActions[index];
    }

    public void setTurnAction(int index, AbstractAction action) {
        turnActions[index] = action;
    }

    public Wonder7Board getPlayerWonderBoard(int index) {
        return playerWonderBoard[index];
    }

    public void setPlayerWonderBoard(int index, Wonder7Board wonder) {
        playerWonderBoard[index] = wonder;
    }


    public List<HashMap<Wonders7Constants.Resource, Integer>> getAllPlayerResources() {
        return playerResources;
    } // Return all player's resources hashmap

    public HashMap<Wonders7Constants.Resource, Integer> getPlayerResources(int index) {
        return playerResources.get(index);
    } // Return players resource hashmap

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
        result = 31 * result + Arrays.hashCode(turnActions);
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