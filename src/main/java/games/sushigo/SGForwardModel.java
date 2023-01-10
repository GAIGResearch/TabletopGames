package games.sushigo;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import games.sushigo.actions.ChopSticksAction;
import games.sushigo.actions.NigiriWasabiAction;
import games.sushigo.actions.PlayCardAction;
import games.sushigo.cards.SGCard;
import utilities.Utils;

import java.util.*;
import java.util.stream.Collectors;

import static games.sushigo.cards.SGCard.SGCardType.*;

public class SGForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        SGGameState gs = (SGGameState) firstState;
        SGParameters parameters = (SGParameters) gs.getGameParameters();
        gs.nCardsInHand = 0;
        gs.deckRotations = 0;
        gs.playerScore = new Counter[firstState.getNPlayers()];
        gs.playerCardPicks = new int[firstState.getNPlayers()];
        gs.playerExtraCardPicks = new int[firstState.getNPlayers()];
        gs.playedCards = new HashMap[firstState.getNPlayers()];
        gs.playerWasabiAvailable = new Counter[firstState.getNPlayers()];
        gs.playerChopsticksActivated = new boolean[firstState.getNPlayers()];
        gs.playerExtraTurns = new int[firstState.getNPlayers()];
        Arrays.fill(gs.getPlayerCardPicks(), -1);

        //Setup draw & discard piles
        setupDrawpile(gs);
        gs.discardPile = new Deck<>("Discard pile", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        //Setup player hands and fields
        gs.playerHands = new ArrayList<>();
        gs.nCardsInHand = parameters.nCards - firstState.getNPlayers() + 2;
        for (int i = 0; i < gs.getNPlayers(); i++) {
            gs.playerHands.add(new Deck<>("Player" + i + " hand", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            gs.playedCards[i] = new HashMap<>();
            for (SGCard.SGCardType type: SGCard.SGCardType.values()) {
                gs.playedCards[i].put(type, new Counter(0, 0, -1, type.name()));
            }
        }
        drawNewHands(gs);

        gs.getTurnOrder().setStartingPlayer(0);
    }

    public void drawNewHands(SGGameState gs) {
        for (int i = 0; i < gs.getNPlayers(); i++) {
            for (int j = 0; j < gs.nCardsInHand; j++) {
                gs.playerHands.get(i).add(gs.drawPile.draw());
            }
        }
    }

    private void setupDrawpile(SGGameState gs) {
        SGParameters parameters = (SGParameters) gs.getGameParameters();
        gs.drawPile = new Deck<>("Draw pile", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        for (int i = 0; i < parameters.nMaki_3Cards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Maki, 3));
        }
        for (int i = 0; i < parameters.nMaki_2Cards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Maki, 2));
        }
        for (int i = 0; i < parameters.nMaki_1Cards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Maki, 1));
        }
        for (int i = 0; i < parameters.nChopstickCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Chopsticks));
        }
        for (int i = 0; i < parameters.nTempuraCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Tempura));
        }
        for (int i = 0; i < parameters.nSashimiCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Sashimi));
        }
        for (int i = 0; i < parameters.nDumplingCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Dumpling));
        }
        for (int i = 0; i < parameters.nSquidNigiriCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.SquidNigiri));
        }
        for (int i = 0; i < parameters.nSalmonNigiriCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.SalmonNigiri));
        }
        for (int i = 0; i < parameters.nEggNigiriCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.EggNigiri));
        }
        for (int i = 0; i < parameters.nWasabiCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Wasabi));
        }
        for (int i = 0; i < parameters.nPuddingCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Pudding));
        }
        gs.drawPile.shuffle(new Random());
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        //Rotate deck and reveal cards
        SGGameState gs = (SGGameState) currentState;
        int turn = gs.getTurnOrder().getTurnCounter();
        if ((turn + 1) % gs.getNPlayers() == 0 && gs.getPlayerExtraTurns(gs.getCurrentPlayer()) <= 0) {
            revealCards(gs);
            removeUsedChopsticks(gs);
            rotateDecks(gs);

            //clear picks
            Arrays.fill(gs.getPlayerCardPicks(), -1);
        }


        //Check if game/round over
        if (isRoundOver(gs) && gs.getPlayerExtraTurns(gs.getCurrentPlayer()) <= 0) {
            giveMakiPoints(gs);
            if (gs.getTurnOrder().getRoundCounter() >= 2) {
                givePuddingPoints(gs);
                gs.endGame();
                return;
            }
            gs.getTurnOrder().endRound(currentState);
            return;
        }

        //End turn
        if (currentState.getGameStatus() == Utils.GameResult.GAME_ONGOING) {
            if (gs.getPlayerChopSticksActivated(gs.getCurrentPlayer()) && gs.getPlayerExtraTurns(gs.getCurrentPlayer()) > 0) {
                gs.setPlayerExtraTurns(gs.getCurrentPlayer(), gs.getPlayerExtraTurns(gs.getCurrentPlayer()) - 1);
                return;
                // if we use chopsticks it is still our go, so don't end player turn
            }

            //reset chopstick activation and end turn
            currentState.getTurnOrder().endPlayerTurn(currentState);
        }
    }

    private void removeUsedChopsticks(SGGameState gs) {
        for (int i = 0; i < gs.getNPlayers(); i++) {
            if (gs.getPlayerChopSticksActivated(i)) {
                gs.playedCards[i].get(SGCard.SGCardType.Chopsticks).decrement(1);
                gs.getPlayerHands().get(i).add(new SGCard(SGCard.SGCardType.Chopsticks));
                gs.setPlayerChopsticksActivated(i, false);
            }
        }
    }

    private void giveMakiPoints(SGGameState gs) {
        //Calculate maki points for each player
        int[] makiPlayerPoints = new int[gs.getNPlayers()];
        for (int i = 0; i < gs.getNPlayers(); i++) {
            makiPlayerPoints[i] = gs.playedCards[i].get(Maki).getValue();
        }

        //Calculate who has the most points and who has the second most points
        int currentBest = 0;
        int secondBest = 0;
        List<Integer> mostPlayers = new ArrayList<>();
        List<Integer> secondPlayers = new ArrayList<>();
        for (int i = 0; i < makiPlayerPoints.length; i++) {
            if (makiPlayerPoints[i] > currentBest) {
                secondBest = currentBest;
                secondPlayers.clear();
                secondPlayers.addAll(mostPlayers);

                currentBest = makiPlayerPoints[i];
                mostPlayers.clear();
                mostPlayers.add(i);
            } else if (makiPlayerPoints[i] == currentBest) mostPlayers.add(i);
            else if (makiPlayerPoints[i] > secondBest) {
                secondBest = makiPlayerPoints[i];
                secondPlayers.clear();
                secondPlayers.add(i);
            } else if (makiPlayerPoints[i] == secondBest) secondPlayers.add(i);
        }

        //Calculate the score each player gets
        SGParameters parameters = (SGParameters) gs.getGameParameters();
        int mostScore = parameters.valueMakiMost;
        int secondScore = parameters.valueMakiSecond;
        if (!mostPlayers.isEmpty()) mostScore /= mostPlayers.size();
        if (!secondPlayers.isEmpty()) secondScore /= secondPlayers.size();

        //Add score to players
        if (currentBest != 0) {
            for (Integer mostPlayer : mostPlayers) {
                gs.getPlayerScore()[mostPlayer].increment(mostScore);
            }
        }
        if (secondBest != 0) {
            for (Integer secondPlayer : secondPlayers) {
                gs.getPlayerScore()[secondPlayer].increment(secondScore);
            }
        }
    }

    private void givePuddingPoints(SGGameState gs) {
        SGParameters parameters = (SGParameters) gs.getGameParameters();

        //Calculate maki points for each player
        int[] puddingPlayerPoints = new int[gs.getNPlayers()];
        for (int i = 0; i < gs.getNPlayers(); i++) {
            puddingPlayerPoints[i] = gs.playedCards[i].get(Pudding).getValue() * parameters.puddingValue;
        }

        //Calculate who has the most points and who has the second most points
        int currentBest = 0;
        int currentWorst = parameters.nPuddingCards + 1;
        List<Integer> mostPlayers = new ArrayList<>();
        List<Integer> leastPlayers = new ArrayList<>();
        for (int i = 0; i < puddingPlayerPoints.length; i++) {
            if (puddingPlayerPoints[i] > currentBest) {
                currentBest = puddingPlayerPoints[i];
                mostPlayers.clear();
                mostPlayers.add(i);
            } else if (puddingPlayerPoints[i] == currentBest) mostPlayers.add(i);
            else currentWorst = puddingPlayerPoints[i];
        }
        if (currentBest > currentWorst) {
            for (int i = 0; i < puddingPlayerPoints.length; i++) {
                if (puddingPlayerPoints[i] < currentWorst) {
                    currentWorst = puddingPlayerPoints[i];
                    leastPlayers.clear();
                    leastPlayers.add(i);
                } else if (puddingPlayerPoints[i] == currentWorst) leastPlayers.add(i);
            }
        }

        //Calculate the score each player gets
        int mostScore = parameters.valuePuddingMost;
        int leastScore = parameters.valuePuddingLeast;
        if (!mostPlayers.isEmpty()) mostScore /= mostPlayers.size();
        if (!leastPlayers.isEmpty()) leastScore /= leastPlayers.size();

        //Add score to players
        if (currentBest != 0) {
            for (Integer mostPlayer : mostPlayers) {
                gs.getPlayerScore()[mostPlayer].increment(mostScore);
            }
        }
        for (Integer leastPlayer : leastPlayers) {
            gs.getPlayerScore()[leastPlayer].increment(leastScore);
        }
    }

    boolean isRoundOver(SGGameState sggs) {
        for (int i = 0; i < sggs.getPlayerHands().size(); i++) {
            if (sggs.getPlayerHands().get(i).getSize() > 0) return false;
        }
        return true;
    }

    void revealCards(SGGameState gs) {
        for (int i = 0; i < gs.getNPlayers(); i++) {
            //Moves the card from the players hand to field
            if (gs.getPlayerCardPicks()[i] < 0) gs.setPlayerCardPick(0, i);
            if (gs.getPlayerHands().get(i).getSize() <= gs.getPlayerCardPicks()[i]) continue;
            SGCard cardToReveal = gs.getPlayerHands().get(i).get(gs.getPlayerCardPicks()[i]);
            gs.getPlayerHands().get(i).remove(cardToReveal);
            gs.playedCards[i].get(cardToReveal.type).increment(cardToReveal.count);

            //Add points to player
            gs.getPlayerScore()[i].increment(cardToReveal.type.getCardScore(gs, i));

            if (gs.getPlayerChopSticksActivated(i)) {
                if (gs.getPlayerCardPicks()[i] < gs.getPlayerExtraCardPicks()[i])
                    gs.setPlayerExtraCardPick(gs.getPlayerExtraCardPicks()[i] - 1, i);
                if (gs.getPlayerHands().get(i).getSize() > gs.getPlayerExtraCardPicks()[i]) {
                    SGCard extraCardToReveal = gs.getPlayerHands().get(i).get(gs.getPlayerExtraCardPicks()[i]);
                    gs.getPlayerHands().get(i).remove(extraCardToReveal);
                    gs.playedCards[i].get(extraCardToReveal.type).increment(extraCardToReveal.count);

                    //Add points to player
                    gs.getPlayerScore()[i].increment(extraCardToReveal.type.getCardScore(gs, i));
                }
            }
        }
    }


    void rotateDecks(SGGameState SGGS) {
        SGGS.deckRotations++;
        Deck<SGCard> tempDeck;
        tempDeck = SGGS.getPlayerHands().get(0).copy();
        for (int i = 1; i < SGGS.getNPlayers(); i++) {
            SGGS.getPlayerHands().set(i - 1, SGGS.getPlayerHands().get(i).copy());
        }
        SGGS.getPlayerHands().set(SGGS.getNPlayers() - 1, tempDeck.copy());
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        SGGameState sggs = (SGGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();

        int currentPlayer = sggs.getCurrentPlayer();
        Deck<SGCard> currentPlayerHand = sggs.getPlayerHands().get(currentPlayer);
        for (int i = 0; i < currentPlayerHand.getSize(); i++) {
            SGCard.SGCardType cardType = currentPlayerHand.get(i).type;
            if (sggs.getPlayerCardPicks()[currentPlayer] == i) continue;
            switch (cardType) {
                case Maki:
                case Tempura:
                case Sashimi:
                case Dumpling:
                case Wasabi:
                case Chopsticks:
                case Pudding:
                    actions.add(new PlayCardAction(currentPlayer, cardType));
                    break;
                case SquidNigiri:
                case SalmonNigiri:
                case EggNigiri:
                    actions.add(new PlayCardAction(currentPlayer, cardType));
                    if (sggs.getPlayerWasabiAvailable(currentPlayer).getValue() > 0)
                        actions.add(new NigiriWasabiAction(currentPlayer, cardType));
                    break;
            }
        }
        // remove duplicate actions
        actions = actions.stream().distinct().collect(Collectors.toList());
        if (sggs.playedCards[currentPlayer].get(Chopsticks).getValue() > 0 &&
                !sggs.getPlayerChopSticksActivated(currentPlayer) &&
                sggs.getPlayerHands().get(currentPlayer).getSize() > 1) {
            actions.add(new ChopSticksAction(currentPlayer));
        }
        return actions;
    }
}