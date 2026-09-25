package games.toads;

import core.*;
import core.actions.AbstractAction;
import core.components.*;
import core.interfaces.IExtendedSequence;
import games.toads.abilities.BattleResult;
import games.toads.actions.*;
import games.toads.components.ToadCard;

import java.util.*;
import java.util.stream.Collectors;

import static games.toads.ToadConstants.ToadGamePhase.*;


public class ToadForwardModel extends StandardForwardModel {


    @Override
    protected void _setup(AbstractGameState firstState) {
        ToadGameState state = (ToadGameState) firstState;
        ToadParameters params = (ToadParameters) state.getGameParameters();

        state.discardOptions = 0;
        state.nextBattle = 0;
        state.battlesWon = new int[2][2];
        state.battlesTied = new int[2];
        state.shrineFlags = new int[2][2];
        state.roundWinners = new int[8][2];
        state.battlesWon[0][0] = params.firstRoundHandicap;
        state.battlesWon[1][0] = params.firstRoundHandicap;
        state.fieldCards = new ToadCard[state.getNPlayers()];
        state.hiddenFlankCards = new ToadCard[state.getNPlayers()];
        state.tieBreakers = new ToadCard[state.getNPlayers()];
        state.playerDiscards = new ArrayList<>();
        state.playerHands = new ArrayList<>();
        state.playerDecks = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++) {
            state.playerDecks.add(new PartialObservableDeck<>("Player " + i + " Deck", i, 2, CoreConstants.VisibilityMode.HIDDEN_TO_ALL));
            state.playerHands.add(new PartialObservableDeck<>("Player " + i + " Hand", i, 2, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            state.playerDiscards.add(new Deck<>("Player " + i + " Discard", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            List<ToadCard> cards = params.getCardDeck();
            state.cardTypesInPlay = cards.stream().map(c -> c.type).collect(Collectors.toUnmodifiableSet());
            state.playerDecks.get(i).add(cards);
            state.playerDecks.get(i).shuffle(state.getRnd());
        }
        drawHandsAndStartWar(state);
    }

    /**
     * Draws each player's hand for a War, and sets the phase in which it starts.
     */
    private void drawHandsAndStartWar(ToadGameState state) {
        ToadParameters params = (ToadParameters) state.getGameParameters();
        // with openingReturn each player draws one extra card, and then returns one to the bottom of their deck
        int cardsToDraw = params.openingReturn ? params.handSize + 1 : params.handSize;
        for (int player = 0; player < state.getNPlayers(); player++) {
            for (int i = 0; i < cardsToDraw; i++) {
                state.playerHands.get(player).add(state.playerDecks.get(player).draw());
            }
        }
        if (params.openingReturn)
            state.setGamePhase(OPENING_RETURN);
        else
            startBattlePhase(state);
    }

    private void startBattlePhase(ToadGameState state) {
        ToadParameters params = (ToadParameters) state.getGameParameters();
        state.setGamePhase(params.discardOption ? DISCARD : PLAY);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        ToadGameState state = (ToadGameState) gameState;
        if (state.getGamePhase().equals(OPENING_RETURN)) {
            return state.getPlayerHand(state.getCurrentPlayer()).stream()
                    .map(c -> (AbstractAction) new ReturnCardToDeck(c))
                    .distinct()
                    .toList();
        } else if (state.getGamePhase().equals(DISCARD)) {
            return computeDiscardActions(state);
        } else if (state.getGamePhase().equals(PLAY)) {
            return computePlayActions(state);
        }
        throw new AssertionError("Unknown game phase: " + state.getGamePhase());
    }

    private List<AbstractAction> computePlayActions(ToadGameState state) {
        List<AbstractAction> actions = new ArrayList<>();
        int player = state.getCurrentPlayer();
        if (state.fieldCards[player] == null) {
            for (ToadCard card : state.playerHands.get(player)) {
                actions.add(new PlayFieldCard(card));
            }
        } else if (state.hiddenFlankCards[player] == null) {
            for (ToadCard card : state.playerHands.get(player)) {
                actions.add(new PlayFlankCard(card));
            }
        } else {
            throw new AssertionError("Player already has Field and Flank cards in play");
        }

        return actions.stream().distinct().toList();
    }

    private List<AbstractAction> computeDiscardActions(ToadGameState state) {
        List<AbstractAction> actions = state.getPlayerHand(state.getCurrentPlayer()).stream()
                .map(RecycleCard::new)
                .distinct()
                .collect(Collectors.toList());
        actions.add(new RecycleCard(null));
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {

        if (gameState.isActionInProgress())
            return;
        // Player 0 takes two turns (field and flank) [well, turn-owner to be more precise]
        // then Player 1 does the same
        // then we reveal the hidden cards and resolve the two battles
        int currentPlayer = gameState.getCurrentPlayer();
        ToadGameState state = (ToadGameState) gameState;
        if (state.getGamePhase() == OPENING_RETURN) {
            // once both players have returned a card, the first Battle starts (the first to return attacks)
            state.discardOptions++;
            if (state.discardOptions == 2) {
                state.discardOptions = 0;
                startBattlePhase(state);
            }
            endPlayerTurn(state, 1 - currentPlayer);
            return;
        }
        if (state.getGamePhase() == DISCARD) {
            // in this case we check if both players have DISCARDED (or had the option to)
            if (action instanceof RecycleCard) {
                state.discardOptions++;
                if (state.discardOptions == 2) {
                    state.discardOptions = 0;
                    state.setGamePhase(PLAY);
                }
                endPlayerTurn(state, 1 - currentPlayer);
                return;

            } else {
                throw new AssertionError("Unknown action for DISCARD phase : " + action);
            }
        }
        if (state.getGamePhase() == POST_BATTLE) {
            afterBattle(state);
        }
        if (state.hiddenFlankCards[currentPlayer] == null) {
            // continue with the same player
        } else if (state.fieldCards[1 - currentPlayer] == null) {
            // next player
            endPlayerTurn(gameState, 1 - currentPlayer);
        } else {
            // we reveal cards
            state.revealFlankCards();

            // and then resolve battle
            // not the most elegant solution, but with 2 cards each no need to generalise yet
            int attacker = 1 - currentPlayer; // attacker always goes first; so the second person to play (the current player) is the defender
            BattleResult battle = new BattleResult(state, attacker, state.fieldCards[attacker], state.fieldCards[1 - attacker],
                    state.hiddenFlankCards[attacker], state.hiddenFlankCards[1 - attacker]);

            int[] scoreDiff = battle.calculate();
            int battlesTied = 2 - scoreDiff[0] - scoreDiff[1];
            state.battlesTied[state.getRoundCounter()] += battlesTied;

            // convert back to scores for each player
            int round = state.getRoundCounter();
            // each tied lane sends both cards to the Shrine, a Flag for each player
            int flags = battlesTied;
            // Overcommit rule (only counts as one victory if you win by 2 and are currently ahead - or override is set)
            // and this Calm double win sends the other Hostage stack to the Shrine, another Flag for each player
            if (scoreDiff[0] == 2 && !battle.getFrogOverride(0) && state.battlesWon[round][0] >= state.battlesWon[round][1]) {
                scoreDiff[0]--;
                flags++;
            } else if (scoreDiff[1] == 2 && !battle.getFrogOverride(1) && state.battlesWon[round][1] >= state.battlesWon[round][0]) {
                scoreDiff[1]--;
                flags++;
            }
            state.shrineFlags[round][0] += flags;
            state.shrineFlags[round][1] += flags;
            // and increment scores
            state.battlesWon[round][0] += scoreDiff[0];
            state.battlesWon[round][1] += scoreDiff[1];

            state.roundWinners[state.nextBattle][0] = scoreDiff[0];
            state.roundWinners[state.nextBattle][1] = scoreDiff[1];
            state.nextBattle++;

            // move cards to discard
            state.playerDiscards.get(0).add(state.fieldCards[0]);
            state.playerDiscards.get(0).add(state.hiddenFlankCards[0]);
            state.playerDiscards.get(1).add(state.fieldCards[1]);
            state.playerDiscards.get(1).add(state.hiddenFlankCards[1]);
            // reset field and flank cards
            state.fieldCards = new ToadCard[state.getNPlayers()];
            state.hiddenFlankCards = new ToadCard[state.getNPlayers()];

            // we then process any actions that need to be done after the battle
            // but first we draw up cards (as these are important for some of the post-battle actions)
            // Draw 2 cards for each player
            for (int player = 0; player < state.getNPlayers(); player++) {
                int cardsToDraw = Math.min(2, state.playerDecks.get(player).getSize());
                for (int i = 0; i < cardsToDraw; i++) {
                    state.playerHands.get(player).add(state.playerDecks.get(player).draw());
                }
            }

            if (battle.getPostBattleActions().isEmpty()) {
                afterBattle(state);
            } else {
                state.setGamePhase(POST_BATTLE);
                // they are listed in the order they resolved, and the first must be on top of the stack
                List<IExtendedSequence> postBattleActions = battle.getPostBattleActions();
                for (int i = postBattleActions.size() - 1; i >= 0; i--) {
                    state.setActionInProgress(postBattleActions.get(i));
                }
            }
        }
    }

    private void afterBattle(ToadGameState state) {
        // if all cards played, then we keep the same player as the attacker for the next round
        // Then check for end of round
        ToadParameters params = (ToadParameters) state.getGameParameters();
        startBattlePhase(state);

        if (state.playerHands.get(0).getSize() <= 1) {
            // one card left in hand each
            if (state.playerDecks.get(0).getSize() != 0 || state.playerDecks.get(1).getSize() != 0) {
                throw new AssertionError("Should have no cards left in either deck at this point");
            }
            // no more cards to draw so end of round
            if (state.getRoundCounter() == 1) {
                // end of game
                endGame(state);
            } else {
                // set tie breakers
                state.tieBreakers[0] = state.playerHands.get(0).draw();
                state.tieBreakers[1] = state.playerHands.get(1).draw();
                // the Casualties go beside the Shrine, so each player starts War 2 with a Flag
                state.shrineFlags[1][0] = 1;
                state.shrineFlags[1][1] = 1;
                // then discards become the other players decks
                state.playerDecks.get(0).add(state.playerDiscards.get(1));
                state.playerDecks.get(1).add(state.playerDiscards.get(0));
                for (Deck<ToadCard> discard : state.playerDiscards) {
                    discard.clear();
                }
                // shuffle
                state.playerDecks.get(0).shuffle(state.getRnd());
                state.playerDecks.get(1).shuffle(state.getRnd());
                drawHandsAndStartWar(state);
                int firstPlayerOfSecondRound = switch (params.secondRoundStart) {
                    case ONE -> 0;
                    case TWO -> 1;
                    case LOSER -> state.battlesWon[0][0] >= state.battlesWon[0][1] ? 1 : 0;
                    case WINNER -> state.battlesWon[0][0] > state.battlesWon[0][1] ? 0 : 1;
                };
                endRound(state, firstPlayerOfSecondRound);
            }
        } else {
            // the defender in this battle always starts the next one as Attacker
            endPlayerTurn(state, state.getCurrentPlayer());
        }
    }
}
