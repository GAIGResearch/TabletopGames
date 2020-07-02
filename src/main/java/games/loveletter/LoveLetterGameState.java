package games.loveletter;

import core.AbstractGameParameters;
import core.AbstractGameState;
import core.components.Component;
import core.interfaces.IGamePhase;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;

import static games.loveletter.LoveLetterGameState.LoveLetterGamePhase.Draw;

public class LoveLetterGameState extends AbstractGameState implements IPrintable {

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

    public LoveLetterGameState(AbstractGameParameters gameParameters, int nPlayers) {
        super(gameParameters, new LoveLetterTurnOrder(nPlayers));
        gamePhase = Draw;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.addAll(playerHandCards);
        components.addAll(playerDiscardCards);
        components.add(drawPile);
        components.add(reserveCards);
        return components;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        // TODO make sure PO
        LoveLetterGameState llgs = new LoveLetterGameState(gameParameters.copy(), getNPlayers());
        llgs.drawPile = drawPile.copy();
        llgs.reserveCards = reserveCards.copy();
        llgs.playerHandCards = new ArrayList<>();
        llgs.playerDiscardCards = new ArrayList<>();
        for (int i = 0; i < getNPlayers(); i++) {
            llgs.playerHandCards.add(playerHandCards.get(i).copy());
            llgs.playerDiscardCards.add(playerDiscardCards.get(i).copy());
        }
        llgs.effectProtection = effectProtection.clone();
        return llgs;
    }

    @Override
    protected double _getScore(int playerId) {
        return new LoveLetterHeuristic().evaluateState(this, playerId);
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        // TODO
        return null;
    }

    @Override
    protected void _reset() {
        gamePhase = Draw;
        playerHandCards = new ArrayList<>();
        playerDiscardCards = new ArrayList<>();
        drawPile = null;
        reserveCards = null;
        effectProtection = new boolean[getNPlayers()];
    }

    /**
     * Checks if the countess needs to be forced to play.
     * @param playerDeck - deck of player to check
     * @return - true if countess should be forced, false otherwise.
     */
    boolean needToForceCountess(Deck<LoveLetterCard> playerDeck){
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
        setPlayerResult(Utils.GameResult.LOSE, playerID);

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
     */
    public void printToConsole() {
        System.out.println("Love Letter Game-State");
        System.out.println("======================");

        for (int i = 0; i < playerHandCards.size(); i++){
            if (getCurrentPlayer() == i)
                System.out.print(">>> Player " + i + ":");
            else
                System.out.print("Player " + i + ": ");
            System.out.print(playerHandCards.get(i).toString(getCurrentPlayer()));
            System.out.print(";\t Discarded: ");
            System.out.print(playerDiscardCards.get(i));
        }

        System.out.print(";\t Protected: ");
        System.out.print(effectProtection[getCurrentPlayer()]);
        System.out.print(";\t Status: ");
        System.out.println(playerResults[getCurrentPlayer()]);

        System.out.print("DrawPile" + ":");
        System.out.print(drawPile);
        System.out.println();

        System.out.print("ReserveCards" + ":");
        System.out.println(reserveCards.toString(getCurrentPlayer()));
        System.out.println();

        System.out.println("Current GamePhase: " + gamePhase);
        System.out.println();
    }
}
