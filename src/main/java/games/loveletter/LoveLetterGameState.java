package games.loveletter;

import core.AbstractGameState;
import core.gamephase.GamePhase;
import core.gamephase.DefaultGamePhase;
import core.ForwardModel;
import core.actions.IAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.observations.IObservation;
import games.loveletter.actions.*;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;

public class LoveLetterGameState extends AbstractGameState {

    // Love letter adds one game phase on top of default phases
    public enum LoveLetterGamePhase implements GamePhase {
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
    //
    boolean[] effectProtection;

    public LoveLetterGameState(LoveLetterParameters gameParameters, ForwardModel model, int nPlayers) {
        super(gameParameters, model, new LoveLetterTurnOrder(nPlayers));
        gamePhase = LoveLetterGamePhase.Draw;
    }

    @Override
    public IObservation getObservation(int player) {
        return new LoveLetterObservation(playerHandCards, playerDiscardCards, drawPile, reserveCards, effectProtection, player, gamePhase, playerResults);
    }

    @Override
    public void endGame() {
        this.gameStatus = Utils.GameResult.GAME_END;

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

        if (bestPlayers.size() == 1){
            for (int i = 0; i < getNPlayers(); i++) {
                playerResults[i] = Utils.GameResult.GAME_LOSE;
            }
            playerResults[bestPlayers.get(0)] = Utils.GameResult.GAME_WIN;
            return;
        }

        bestValue = 0;
        for (int i = 0; i < getNPlayers(); i++) {
            points = 0;
            if (playerResults[i] == Utils.GameResult.GAME_WIN)
                for (LoveLetterCard card : playerDiscardCards.get(i).getCards())
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

    @Override
    public List<IAction> computeAvailableActions() {
        ArrayList<IAction> actions;
        int player = getTurnOrder().getCurrentPlayer(this);
        if (gamePhase.equals(DefaultGamePhase.Main)) {
            actions = playerActions(player);
        } else if (gamePhase.equals(LoveLetterGamePhase.Draw)) {
            actions = drawAction(player);
        } else {
            actions = new ArrayList<>();
        }

        return actions;
    }

    /**
     * Computes actions available for the given player.
     * @param playerID - ID of player to calculate actions for.
     * @return - ArrayList of IAction objects.
     */
    private ArrayList<IAction> playerActions(int playerID) {
        ArrayList<IAction> actions = new ArrayList<>();
        Deck<LoveLetterCard> playerDeck = playerHandCards.get(playerID);
        Deck<LoveLetterCard> playerDiscardPile = playerDiscardCards.get(playerID);

        if (needToForceCountess(playerDeck)){
            for (LoveLetterCard card : playerDeck.getCards()) {
                if (card.cardType == LoveLetterCard.CardType.Countess)
                    actions.add(new CountessAction(card, playerDeck, playerDiscardPile));
            }
        }
        else {
            for (LoveLetterCard card : playerDeck.getCards()) {
                switch (card.cardType) {
                    case Priest:
                        for (int targetPlayer = 0; targetPlayer < getNPlayers(); targetPlayer++) {
                            if (targetPlayer == playerID || playerResults[targetPlayer] == Utils.GameResult.GAME_LOSE)
                                continue;
                            actions.add(new PriestAction(card, playerDeck, playerDiscardPile,
                                    playerHandCards.get(targetPlayer), targetPlayer, playerID));
                        }
                        break;
                    case Guard:
                        for (int targetPlayer = 0; targetPlayer < getNPlayers(); targetPlayer++) {
                            if (targetPlayer == playerID || playerResults[targetPlayer] == Utils.GameResult.GAME_LOSE)
                                continue;
                            for (LoveLetterCard.CardType type : LoveLetterCard.CardType.values())
                                actions.add(new GuardAction(card, playerDeck, playerDiscardPile,
                                        playerHandCards.get(targetPlayer), targetPlayer, type));
                        }
                        break;
                    case Baron:
                        for (int targetPlayer = 0; targetPlayer < getNPlayers(); targetPlayer++) {
                            if (targetPlayer == playerID || playerResults[targetPlayer] == Utils.GameResult.GAME_LOSE)
                                continue;
                            actions.add(new BaronAction(card, playerDeck, playerDiscardPile,
                                    playerHandCards.get(targetPlayer), targetPlayer, playerID));
                        }
                        break;
                    case Handmaid:
                        actions.add(new HandmaidAction(card, playerDeck, playerDiscardPile, playerID));
                        break;
                    case Prince:
                        for (int targetPlayer = 0; targetPlayer < getNPlayers(); targetPlayer++) {
                            if (targetPlayer == playerID || playerResults[targetPlayer] == Utils.GameResult.GAME_LOSE)
                                continue;
                            actions.add(new PrinceAction(card, playerDeck, playerDiscardPile,
                                    playerHandCards.get(targetPlayer), targetPlayer, drawPile,
                                    playerDiscardCards.get(targetPlayer)));
                        }
                        break;
                    case King:
                        for (int targetPlayer = 0; targetPlayer < getNPlayers(); targetPlayer++) {
                            if (targetPlayer == playerID || playerResults[targetPlayer] == Utils.GameResult.GAME_LOSE)
                                continue;
                            actions.add(new KingAction(card, playerDeck, playerDiscardPile,
                                    playerHandCards.get(targetPlayer), targetPlayer));
                        }
                        break;
                    case Countess:
                        actions.add(new CountessAction(card, playerDeck, playerDiscardPile));
                        break;
                    case Princess:
                        actions.add(new PrincessAction(card, playerDeck, playerDiscardPile, playerID));
                        break;
                    default:
                        System.out.println("No core actions known for cardtype: " + card.cardType.toString());
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
    private ArrayList<IAction> drawAction(int player){
        ArrayList<IAction> actions = new ArrayList<>();
        actions.add(new DrawCard(drawPile, playerHandCards.get(player), player));
        return actions;
    }

    /**
     * Checks if the countess needs to be forced to play.
     * @param playerDeck - deck of player to check
     * @return - true if countess should be forced, false otherwise.
     */
    private boolean needToForceCountess(Deck<LoveLetterCard> playerDeck){
        boolean ownsCountess = false;
        for (LoveLetterCard card : playerDeck.getCards()) {
            if (card.cardType == LoveLetterCard.CardType.Countess){
                ownsCountess = true;
                break;
            }
        }

        boolean forceCountess = false;
        if (ownsCountess)
        {
            for (LoveLetterCard card: playerDeck.getCards()) {
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
        while (playerHandCards.get(playerID).getCards().size() > 0)
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
    public int getRemainingCards(){return drawPile.getCards().size();}

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
