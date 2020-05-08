package games.loveletter;

import core.AbstractGameState;
import core.ForwardModel;
import core.actions.IAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.observations.IObservation;
import games.loveletter.actions.*;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class LoveLetterGameState extends AbstractGameState {

    public enum GamePhase {
        DrawPhase,
        PlayerMove
    }

    private List<PartialObservableDeck<LoveLetterCard>> playerHandCards;
    private List<Deck<LoveLetterCard>> playerDiscardCards;
    private PartialObservableDeck<LoveLetterCard> drawPile;
    private PartialObservableDeck<LoveLetterCard> reserveCards;
    private boolean[] effectProtection;
    private GamePhase gamePhase = GamePhase.DrawPhase;

    public static boolean PARTIAL_OBSERVABLE = false;

    public GamePhase getGamePhase() {
        return gamePhase;
    }

    public void setGamePhase(GamePhase gamePhase) {
        this.gamePhase = gamePhase;
    }

    public LoveLetterGameState(LoveLetterParameters gameParameters, ForwardModel model, int nPlayers) {
        super(gameParameters, model, nPlayers, new LoveLetterTurnOrder(nPlayers));
        setComponents(gameParameters);
    }

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

    public LoveLetterCard getReserveCard(){return reserveCards.draw();}

    public boolean getProtection(int playerID){
        return effectProtection[playerID];
    }

    public void setProtection(int playerID, boolean protection){
        effectProtection[playerID] = protection;
    }

    public int getRemainingCards(){return drawPile.getCards().size();}

    public void setComponents(LoveLetterParameters gameParameters) {
        drawPile = new PartialObservableDeck<>("drawPile", getNPlayers());
        effectProtection = new boolean[getNPlayers()];

        // add all cards and distribute 7 random cards to each player
        for (HashMap.Entry<LoveLetterCard.CardType, Integer> entry : gameParameters.cardCounts.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                LoveLetterCard card = new LoveLetterCard(entry.getKey());
                drawPile.add(card);
            }
        }

        reserveCards = new PartialObservableDeck<>("reserveCards", getNPlayers());
        drawPile.shuffle();
        reserveCards.add(drawPile.draw());

        // give each player a single card
        playerHandCards = new ArrayList<>(getNPlayers());
        playerDiscardCards = new ArrayList<>(getNPlayers());
        for (int i = 0; i < getNPlayers(); i++) {
            boolean[] visibility = new boolean[getNPlayers()];
            Arrays.fill(visibility, !PARTIAL_OBSERVABLE);
            visibility[i] = true;

            PartialObservableDeck<LoveLetterCard> playerCards = new PartialObservableDeck<>("playerHand"+i, visibility);
            playerCards.add(drawPile.draw());
            playerHandCards.add(playerCards);

            Arrays.fill(visibility, true);
            Deck<LoveLetterCard> discardCards = new Deck<>("discardPlayer"+i);
            playerDiscardCards.add(discardCards);
        }
    }

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

    private ArrayList<IAction> drawAction(int player){
        ArrayList<IAction> actions = new ArrayList<>();
        actions.add(new DrawCard(drawPile, playerHandCards.get(player), player));
        return actions;
    }

    @Override
    public List<IAction> computeAvailableActions() {

        ArrayList<IAction> actions;
        int player = getTurnOrder().getCurrentPlayer(this);
        switch (gamePhase){
            case PlayerMove:
                actions = playerActions(player);
                break;
            case DrawPhase:
                actions = drawAction(player);
                break;
            default:
                actions = new ArrayList<>();
                break;
        }

        return actions;
    }

    @Override
    public void setComponents() {

    }


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
