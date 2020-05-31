package games.loveletter;

import core.AbstractGameState;
import core.interfaces.IGamePhase;
import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IObservation;
import games.loveletter.actions.*;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;

public class LoveLetterGameState extends AbstractGameState {

    // Love letter adds one game phase on top of default phases
    public enum LoveLetterGamePhase implements IGamePhase {
        Draw
    }

    // List of cards in player hands
    List<PartialObservableDeck<LoveLetterCard>> playerHandCards;

    // Discarded cards
    List<Deck<LoveLetterCard>> playerDiscardCards;

    // Cards in draw pile
    PartialObservableDeck<LoveLetterCard> drawPile;

    // Cards in the reserve
    PartialObservableDeck<LoveLetterCard> reserveCards;

    // if true: player cannot be effected by any card effects
    boolean[] effectProtection;

    /**
     * Must add all components used in the game to the allComponents area, mapping to their assigned component ID
     * and NOT another game specific key. Use one of these functions for this functionality only:
     *          - Area.putComponent(Component component)
     *          - Area.putComponents(List<Component> components)
     *          - Area.putComponents(Area area)
     * Method is called after initialising the game state.
     */
    @Override
    public void addAllComponents() {
        allComponents.putComponents(playerHandCards);
        allComponents.putComponents(playerDiscardCards);
        allComponents.putComponent(drawPile);
        allComponents.putComponent(reserveCards);
    }

    public LoveLetterGameState(LoveLetterParameters gameParameters, AbstractForwardModel model, int nPlayers) {
        super(gameParameters, model, new LoveLetterTurnOrder(nPlayers));
        gamePhase = LoveLetterGamePhase.Draw;
    }

    @Override
    public IObservation getObservation(int player) {
        return new LoveLetterObservation(playerHandCards, playerDiscardCards, drawPile, reserveCards,
                effectProtection, player, gamePhase, playerResults);
    }

    /**
     * Sets the game-state to be terminal and determines the result of each player.
     */
    @Override
    public void endGame() {
        this.gameStatus = Utils.GameResult.GAME_END;

        // determine which player has the card with the highest value
        List<Integer> bestPlayers = new ArrayList<>();
        int bestValue = 0;
        int points;
        for (int i = 0; i < getNPlayers(); i++) {
            if (playerResults[i] != Utils.GameResult.GAME_LOSE)
                points = playerHandCards.get(i).peek().cardType.getValue();
            else
                points = 0;

            if (points > bestValue){
                bestValue = points;
                bestPlayers.clear();
                bestPlayers.add(i);
            } else if (points == bestValue) {
                bestPlayers.add(i);
            }
        }

        // if just a single player is alive, the player immediately wins the game
        if (bestPlayers.size() == 1){
            for (int i = 0; i < getNPlayers(); i++) {
                playerResults[i] = Utils.GameResult.GAME_LOSE;
            }
            playerResults[bestPlayers.get(0)] = Utils.GameResult.GAME_WIN;
            return;
        }

        // else, the player with the higher sum of card values in its discard pile wins
        // in case two or more players have the same value, they all win
        bestValue = 0;
        for (int i = 0; i < getNPlayers(); i++) {
            points = 0;
            if (playerResults[i] == Utils.GameResult.GAME_WIN)
                for (LoveLetterCard card : playerDiscardCards.get(i).getComponents())
                    points += card.cardType.getValue();
            if (points > bestValue){
                bestValue = points;
                bestPlayers.clear();
                bestPlayers.add(i);
            } else if (points == bestValue) {
                bestPlayers.add(i);
            }
        }

        for (int i = 0; i < getNPlayers(); i++) {
            playerResults[i] = Utils.GameResult.GAME_LOSE;
        }
        for (Integer playerID : bestPlayers)
            playerResults[playerID] = Utils.GameResult.GAME_WIN;
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    public List<AbstractAction> computeAvailableActions() {
        ArrayList<AbstractAction> actions;
        int player = getTurnOrder().getCurrentPlayer(this);
        if (gamePhase.equals(DefaultGamePhase.Main)) {
            actions = playerActions(player);
        } else if (gamePhase.equals(LoveLetterGamePhase.Draw)) {
            actions = drawAction(player);
        } else {
            throw new IllegalArgumentException(gamePhase.toString() + " is unknown to LoveLetterGameState");
        }

        return actions;
    }

    /**
     * Computes actions available for the given player.
     * @param playerID - ID of player to calculate actions for.
     * @return - ArrayList of AbstractAction objects.
     */
    private ArrayList<AbstractAction> playerActions(int playerID) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        Deck<LoveLetterCard> playerDeck = playerHandCards.get(playerID);
        Deck<LoveLetterCard> playerDiscardPile = playerDiscardCards.get(playerID);

        // in case a player holds the countess and either the king or the prince, the countess needs to be played
        if (needToForceCountess(playerDeck)){
            for (int c = 0; c < playerDeck.getSize(); c++) {
                if (playerDeck.getComponents().get(c).cardType == LoveLetterCard.CardType.Countess)
                    actions.add(new CountessAction(playerDeck.getComponentID(), playerDiscardPile.getComponentID(), c));
            }
        }
        // else: we create the respective actions for each card on the player's hand
        else {
            for (int card = 0; card < playerDeck.getSize(); card++) {
                switch (playerDeck.getComponents().get(card).cardType) {
                    case Priest:
                        for (int targetPlayer = 0; targetPlayer < getNPlayers(); targetPlayer++) {
                            if (targetPlayer == playerID || playerResults[targetPlayer] == Utils.GameResult.GAME_LOSE)
                                continue;
                            actions.add(new PriestAction(playerDeck.getComponentID(),
                                    playerDiscardPile.getComponentID(), card, targetPlayer));
                        }
                        break;

                    case Guard:
                        for (int targetPlayer = 0; targetPlayer < getNPlayers(); targetPlayer++) {
                            if (targetPlayer == playerID || playerResults[targetPlayer] == Utils.GameResult.GAME_LOSE)
                                continue;
                            for (LoveLetterCard.CardType type : LoveLetterCard.CardType.values())
                                actions.add(new GuardAction(playerDeck.getComponentID(),
                                        playerDiscardPile.getComponentID(), card, targetPlayer, type));
                        }
                        break;

                    case Baron:
                        for (int targetPlayer = 0; targetPlayer < getNPlayers(); targetPlayer++) {
                            if (targetPlayer == playerID || playerResults[targetPlayer] == Utils.GameResult.GAME_LOSE)
                                continue;
                            actions.add(new BaronAction(playerDeck.getComponentID(),
                                    playerDiscardPile.getComponentID(), card, targetPlayer));
                        }
                        break;

                    case Handmaid:
                        actions.add(new HandmaidAction(playerDeck.getComponentID(),
                                playerDiscardPile.getComponentID(), card));
                        break;

                    case Prince:
                        for (int targetPlayer = 0; targetPlayer < getNPlayers(); targetPlayer++) {
                            if (targetPlayer == playerID || playerResults[targetPlayer] == Utils.GameResult.GAME_LOSE)
                                continue;
                            actions.add(new PrinceAction(playerDeck.getComponentID(),
                                    playerDiscardPile.getComponentID(), card, targetPlayer));
                        }
                        break;

                    case King:
                        for (int targetPlayer = 0; targetPlayer < getNPlayers(); targetPlayer++) {
                            if (targetPlayer == playerID || playerResults[targetPlayer] == Utils.GameResult.GAME_LOSE)
                                continue;
                            actions.add(new KingAction(playerDeck.getComponentID(),
                                    playerDiscardPile.getComponentID(), card, targetPlayer));
                        }
                        break;

                    case Countess:
                        actions.add(new CountessAction(playerDeck.getComponentID(),
                                playerDiscardPile.getComponentID(), card));
                        break;

                    case Princess:
                        actions.add(new PrincessAction(playerDeck.getComponentID(),
                                playerDiscardPile.getComponentID(), card));
                        break;

                    default:
                        throw new IllegalArgumentException("No core actions known for cardtype: " +
                                playerDeck.getComponents().get(card).cardType.toString());
                }
            }
        }

        // add end turn by drawing a card
        return actions;
    }

    /**
     * In draw phase, the players can only draw cards. This returns draw actions.
     * @param player - ID of player who should be drawing a card.
     * @return - ArrayList of DrawCard actions.
     */
    private ArrayList<AbstractAction> drawAction(int player){
        ArrayList<AbstractAction> actions = new ArrayList<>();
        actions.add(new DrawCard(drawPile.getComponentID(), playerHandCards.get(player).getComponentID(), 0));
        return actions;
    }

    /**
     * Checks if the countess needs to be forced to play.
     * @param playerDeck - deck of player to check
     * @return - true if countess should be forced, false otherwise.
     */
    private boolean needToForceCountess(Deck<LoveLetterCard> playerDeck){
        boolean ownsCountess = false;
        for (LoveLetterCard card : playerDeck.getComponents()) {
            if (card.cardType == LoveLetterCard.CardType.Countess){
                ownsCountess = true;
                break;
            }
        }

        boolean forceCountess = false;
        if (ownsCountess)
        {
            for (LoveLetterCard card: playerDeck.getComponents()) {
                if (card.cardType == LoveLetterCard.CardType.Prince || card.cardType == LoveLetterCard.CardType.King){
                    forceCountess = true;
                    break;
                }
            }
        }
        return forceCountess;
    }

    /**
     * Sets this player as dead and updates game and player status
     * @param playerID - ID of player dead
     */
    public void killPlayer(int playerID){
        setPlayerResult(Utils.GameResult.GAME_LOSE, playerID);

        // a losing player needs to discard all cards
        while (playerHandCards.get(playerID).getSize() > 0)
            playerDiscardCards.get(playerID).add(playerHandCards.get(playerID).draw());

        int nPlayersActive = 0;
        for (int i = 0; i < getNPlayers(); i++) {
            if (playerResults[i] == Utils.GameResult.GAME_ONGOING) nPlayersActive++;
        }
        if (nPlayersActive == 1) {
            this.gameStatus = Utils.GameResult.GAME_END;
        }
    }

    // Getters, Setters
    public LoveLetterCard getReserveCard(){return reserveCards.draw();}
    public boolean isNotProtected(int playerID){
        return !effectProtection[playerID];
    }
    public void setProtection(int playerID, boolean protection){
        effectProtection[playerID] = protection;
    }
    public int getRemainingCards(){return drawPile.getComponents().size();}
    public List<PartialObservableDeck<LoveLetterCard>> getPlayerHandCards() {
        return playerHandCards;
    }
    public List<Deck<LoveLetterCard>> getPlayerDiscardCards() {
        return playerDiscardCards;
    }
    public PartialObservableDeck<LoveLetterCard> getDrawPile() {
        return drawPile;
    }

    /**
     * Prints the game state.
     * @param turnOrder - turn order for this game.
     */
    public void print(LoveLetterTurnOrder turnOrder) {
        System.out.println("Love Letter Game-State");
        System.out.println("======================");

        int currentPlayer = turnOrder.getCurrentPlayer(this);

        for (int i = 0; i < getNPlayers(); i++){
            if (currentPlayer == i)
                System.out.print(">>> Player " + i + ":");
            else
                System.out.print("Player " + i + ": ");
            System.out.print(playerHandCards.get(i).toString(currentPlayer));
            System.out.print("; Discarded: ");
            System.out.print(playerDiscardCards.get(i));
            System.out.print("; Protected: ");
            System.out.println(effectProtection[i]);
        }

        System.out.print("DrawPile" + ":");
        System.out.print(drawPile.toString(currentPlayer));
        System.out.println();

        System.out.print("ReserveCards" + ":");
        System.out.println(reserveCards.toString(currentPlayer));
        System.out.println();

        System.out.println("Current GamePhase: " + gamePhase);
    }
}
