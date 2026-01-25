package games.explodingkittens;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IGameEvent;
import games.explodingkittens.actions.*;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.*;

import static games.explodingkittens.cards.ExplodingKittensCard.CardType.*;

public class ExplodingKittensForwardModel extends StandardForwardModel {


    @Override
    protected void _setup(AbstractGameState firstState) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) firstState;
        ExplodingKittensParameters ekp = (ExplodingKittensParameters) firstState.getGameParameters();
        ekgs.playerHandCards = new ArrayList<>();
        // Set up draw pile deck
        PartialObservableDeck<ExplodingKittensCard> drawPile = new PartialObservableDeck<>("Draw Pile", -1, firstState.getNPlayers(), CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        ekgs.drawPile = drawPile;
        ekgs.inPlay = new Deck<>("In Play", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        // Add all cards but defuse and exploding kittens
        for (HashMap.Entry<ExplodingKittensCard.CardType, Integer> entry : ekp.cardCounts.entrySet()) {
            if (entry.getKey() == DEFUSE || entry.getKey() == EXPLODING_KITTEN)
                continue;
            for (int i = 0; i < entry.getValue(); i++) {
                ExplodingKittensCard card = new ExplodingKittensCard(entry.getKey());
                drawPile.add(card);
            }
        }
        ekgs.drawPile.shuffle(ekgs.getRnd());

        // Set up player hands
        List<PartialObservableDeck<ExplodingKittensCard>> playerHandCards = new ArrayList<>(firstState.getNPlayers());
        for (int i = 0; i < firstState.getNPlayers(); i++) {
            boolean[] visible = new boolean[firstState.getNPlayers()];
            visible[i] = true;
            PartialObservableDeck<ExplodingKittensCard> playerCards = new PartialObservableDeck<>("Player Cards", i, visible);
            playerHandCards.add(playerCards);

            // Add defuse card
            ExplodingKittensCard defuse = new ExplodingKittensCard(DEFUSE);
            defuse.setOwnerId(i);
            playerCards.add(defuse);

            // Add N random cards from the deck
            for (int j = 0; j < ekp.nCardsPerPlayer; j++) {
                ExplodingKittensCard c = ekgs.drawPile.draw();
                c.setOwnerId(i);
                playerCards.add(c);
            }
        }
        ekgs.playerHandCards = playerHandCards;
        ekgs.discardPile = new Deck<>("Discard Pile", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        // Add remaining defuse cards and exploding kitten cards to the deck and shuffle again
        for (int i = ekgs.getNPlayers(); i < ekp.cardCounts.get(DEFUSE); i++) {
            ExplodingKittensCard defuse = new ExplodingKittensCard(DEFUSE);
            drawPile.add(defuse);
        }
        for (int i = 0; i < ekgs.getNPlayers() + ekp.cardCounts.get(EXPLODING_KITTEN); i++) {
            ExplodingKittensCard explodingKitten = new ExplodingKittensCard(EXPLODING_KITTEN);
            drawPile.add(explodingKitten);
        }
        drawPile.shuffle(ekgs.getRnd());

        ekgs.orderOfPlayerDeath = new int[ekgs.getNPlayers()];
        ekgs.currentPlayerTurnsLeft = 1;
        ekgs.setGamePhase(CoreConstants.DefaultGamePhase.Main);
    }

    @Override
    protected void _afterAction(AbstractGameState state, AbstractAction action) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) state;
        if (ekgs.isActionInProgress())
            return;

        // We may have some special statuses to be aware of:
        // - If a player has skipped, they skip the draw card

        // - If a player has been attacked, they will have to take two turns
        // - This is stored in the currentAttackLevel, which has to decrement down to zero before play passes on
        // - the nextAttackLevel indicates the attack level that will then be applied to the next player

        if (ekgs.isNotTerminal()) {
            if (ekgs.skip) {
                ekgs.setSkip(false);
            } else {
                // Draw a card for the current player
                int currentPlayer = ekgs.getCurrentPlayer();
                ExplodingKittensCard card = ekgs.drawPile.draw();
                if (card == null) {
                    throw new AssertionError("No cards left in the draw pile");
                }
                if (card.cardType == EXPLODING_KITTEN) {
                    if (ekgs.playerHandCards.get(currentPlayer).stream().anyMatch(c -> c.cardType == DEFUSE)) {
                        // Exploding kitten drawn, player has defuse
                        ExplodingKittensCard defuseCard = ekgs.playerHandCards.get(currentPlayer).stream().filter(c -> c.cardType == DEFUSE).findFirst().get();
                        ekgs.playerHandCards.get(currentPlayer).remove(defuseCard);
                        ekgs.playerHandCards.get(currentPlayer).add(card);  // add Exploding Kitten (to be removed)
                        ekgs.discardPile.add(defuseCard);
                        ekgs.setActionInProgress(new DefuseKitten(currentPlayer));
                        return;
                    } else {
                        // Exploding kitten drawn, player is dead
                        if (ekgs.getPlayerResults()[currentPlayer] == CoreConstants.GameResult.LOSE_GAME) {
                            throw new AssertionError("Player " + currentPlayer + " is already dead");
                        }
                        ekgs.discardPile.add(card);
                        killPlayer(ekgs, currentPlayer);
                        ekgs.currentPlayerTurnsLeft = 1;  // force end of player turn later
                    }
                } else {
                    card.setOwnerId(currentPlayer);
                    ekgs.playerHandCards.get(currentPlayer).add(card);
                }
            }
            ekgs.currentPlayerTurnsLeft--;
            if (ekgs.currentPlayerTurnsLeft == 0 && ekgs.isNotTerminal()) {
                endPlayerTurn(ekgs);
                ekgs.currentPlayerTurnsLeft = Math.max(1, ekgs.nextAttackLevel);
                ekgs.nextAttackLevel = 0;
            }
        }
    }


    public void killPlayer(ExplodingKittensGameState ekgs, int playerID) {
        ekgs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, playerID);
        ekgs.logEvent(new IGameEvent() {
            @Override
            public String name() {
                return "Player " + playerID + " has been killed";
            }

            @Override
            public Set<IGameEvent> getValues() {
                return Set.of();
            }
        });
        int players = ekgs.getNPlayers();
        int nPlayersActive = 0;
        for (int i = 0; i < players; i++) {
            if (ekgs.getPlayerResults()[i] == CoreConstants.GameResult.GAME_ONGOING) nPlayersActive++;
        }
        ekgs.orderOfPlayerDeath[playerID] = players - nPlayersActive;
        if (nPlayersActive == 1) {
            endGame(ekgs);
        }
    }


    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     *
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {

        // This is called when it is a player's main turn; not during the Nope interrupts
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gameState;
        int playerID = ekgs.getCurrentPlayer();
        List<AbstractAction> actions = new ArrayList<>();
        Deck<ExplodingKittensCard> playerDeck = ekgs.playerHandCards.get(playerID);

        // We can pass, or play any card in our hand
        actions.add(new Pass());
        // get all unique playable types in hand
        Set<ExplodingKittensCard.CardType> playableTypes = new HashSet<>();
        for (int i = 0; i < playerDeck.getSize(); i++) {
            playableTypes.add(playerDeck.get(i).cardType);
        }
        // remove defuse and exploding kittens from playable types
        playableTypes.remove(DEFUSE);
        playableTypes.remove(EXPLODING_KITTEN);
        playableTypes.remove(NOPE);

        for (ExplodingKittensCard.CardType type : playableTypes) {
            switch (type) {
                case FAVOR:
                    for (int i = 0; i < ekgs.getNPlayers(); i++) {
                        if (i != playerID) {
                            if (ekgs.isNotTerminalForPlayer(i) && ekgs.getPlayerHand(i).getSize() > 0)
                                actions.add(new PlayEKCard(FAVOR, i));
                        }
                    }
                    break;
                case SHUFFLE, SKIP, ATTACK, SEETHEFUTURE:
                    actions.add(new PlayEKCard(type));
                    break;
                default:
                    // for Cat Cards we need a pair
                    for (int i = 0; i < ekgs.getNPlayers(); i++) {
                        if (i != playerID) {
                            if (ekgs.isNotTerminalForPlayer(i) && ekgs.getPlayerHand(i).getSize() > 0)
                                if (ekgs.playerHandCards.get(playerID).stream().filter(c -> c.cardType == type).count() > 1)
                                    actions.add(new PlayEKCard(type, i));
                        }
                    }
            }
        }
        return actions;
    }


}
