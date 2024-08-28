package games.toads;

import core.CoreConstants;
import core.actions.AbstractAction;
import games.toads.abilities.Saboteur;
import games.toads.abilities.SaboteurII;
import games.toads.actions.PlayFieldCard;
import games.toads.actions.PlayFlankCard;
import games.toads.components.ToadCard;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static games.toads.ToadConstants.ToadCardType.*;
import static org.junit.Assert.*;

public class GameFlow {

    ToadParameters params;
    ToadGameState state;
    ToadForwardModel fm;
    Random rnd;

    @Before
    public void setUp() {
        params = new ToadParameters();
        params.setRandomSeed(933);
        params.setParameterValue("useTactics", false);
        params.setParameterValue("discardOption", false);
        state = new ToadGameState(params, 2);
        fm = new ToadForwardModel();
        fm.setup(state);
        rnd = new Random(933);
    }

    @Test
    public void gameInitialisation() {
        assertEquals(5, state.playerDecks.get(0).getSize());
        assertEquals(5, state.playerDecks.get(1).getSize());
        assertEquals(4, state.playerHands.get(0).getSize());
        assertEquals(4, state.playerHands.get(1).getSize());
        assertEquals(0, state.playerDiscards.get(0).getSize());
        assertEquals(0, state.playerDiscards.get(1).getSize());
    }

    @Test
    public void playersPlayCardsInTurn() {
        assertEquals(0, state.getCurrentPlayer());
        // check we have 0 or 1 value 7 cards (as the only possible duplicate)
        assertEquals(0, state.getPlayerHand(0).stream().filter(c -> c.value == 7).count(), 1);
        assertEquals(4, fm.computeAvailableActions(state).size());
        for (ToadCard card : state.getPlayerHand(0)) {
            assertTrue(fm.computeAvailableActions(state).stream().anyMatch(a -> ((PlayFieldCard) a).card == card));
        }
        fm.computeAvailableActions(state).get(0).execute(state);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(3, fm.computeAvailableActions(state).size());
        for (ToadCard card : state.getPlayerHand(0)) {
            assertTrue(fm.computeAvailableActions(state).stream().anyMatch(a -> ((PlayFlankCard) a).card == card));
        }
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getCurrentPlayer());

        // at this point no cards have been drawn
        assertEquals(3, state.playerHands.get(0).getSize());
        assertEquals(4, state.playerHands.get(1).getSize());
        assertEquals(5, state.playerDecks.get(0).getSize());
        assertEquals(5, state.playerDecks.get(1).getSize());
        // then they play two cards
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, state.getRoundCounter());
        assertEquals(1, state.getTurnCounter());
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, state.getRoundCounter());
        assertEquals(2, state.getTurnCounter());
        assertEquals(4, state.playerHands.get(0).getSize());
        assertEquals(4, state.playerHands.get(1).getSize());
        assertEquals(3, state.playerDecks.get(0).getSize());
        assertEquals(3, state.playerDecks.get(1).getSize());
    }

    private void playCards(ToadCard... cardsInOrder) {
        for (int i = 0; i < cardsInOrder.length; i++) {
            state.getPlayerHand(state.getCurrentPlayer()).add(cardsInOrder[i]);
            AbstractAction action = i % 2 == 0 ? new PlayFieldCard(cardsInOrder[i]) : new PlayFlankCard(cardsInOrder[i]);
            fm.next(state, action);
        }
    }

    @Test
    public void playersAlternateAsAttackerStartingWithPlayerZero() {
        params.secondRoundStart = ToadParameters.SecondRoundStart.ONE;
        for (int i = 0; i < 32; i++) {
            // Each player effectively gets four consecutive actions, as after they have defended, they are the attacker in the next battle
            int expectedPlayer = ((i + 2) / 4) % 2;
            System.out.println("Round " + state.getRoundCounter() + " Turn " + state.getTurnCounter() + " Player " + state.getCurrentPlayer() + " Expected " + expectedPlayer);
            assertEquals(expectedPlayer, state.getCurrentPlayer());
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
    }


    @Test
    public void playersAlternateAsAttackerStartingWithSecondPlayerOnSecondRound() {
        params.secondRoundStart = ToadParameters.SecondRoundStart.TWO;
        for (int i = 0; i < 32; i++) {
            // Each player effectively gets four consecutive actions, as after they have defended, they are the attacker in the next battle
            int expectedPlayer = ((i + 2) / 4) % 2;
            if (i >= 16)
                expectedPlayer = 1 - expectedPlayer;
            System.out.println("Round " + state.getRoundCounter() + " Turn " + state.getTurnCounter() + " Player " + state.getCurrentPlayer() + " Expected " + expectedPlayer);
            assertEquals(expectedPlayer, state.getCurrentPlayer());
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
    }

    @Test
    public void loserStartsSecondRoundIfSoConfigured() {
        params.secondRoundStart = ToadParameters.SecondRoundStart.LOSER;
        for (int game = 0; game < 10; game++) {
            fm.setup(state);
            for (int i = 0; i < 32; i++) {
                // Each player effectively gets four consecutive actions, as after they have defended, they are the attacker in the next battle
                int expectedPlayer = ((i + 2) / 4) % 2;
                if (i >= 16) {
                    int startPlayer = state.battlesWon[0][0] < state.battlesWon[0][1] ? 0 : 1;
                    expectedPlayer = (expectedPlayer + startPlayer) % 2;
                }
                System.out.println("Round " + state.getRoundCounter() + " Turn " + state.getTurnCounter() + " Player " + state.getCurrentPlayer() + " Expected " + expectedPlayer);
                assertEquals(expectedPlayer, state.getCurrentPlayer());
                fm.next(state, fm.computeAvailableActions(state).get(0));
            }
            assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        }
    }

    @Test
    public void winnerStartsSecondRoundIfSoConfigured() {
        params.secondRoundStart = ToadParameters.SecondRoundStart.WINNER;
        for (int game = 0; game < 10; game++) {
            fm.setup(state);
            for (int i = 0; i < 32; i++) {
                // Each player effectively gets four consecutive actions, as after they have defended, they are the attacker in the next battle
                int expectedPlayer = ((i + 2) / 4) % 2;
                if (i >= 16) {
                    int startPlayer = state.battlesWon[0][0] > state.battlesWon[0][1] ? 0 : 1;
                    expectedPlayer = (expectedPlayer + startPlayer) % 2;
                }
                System.out.println("Round " + state.getRoundCounter() + " Turn " + state.getTurnCounter() + " Player " + state.getCurrentPlayer() + " Expected " + expectedPlayer);
                assertEquals(expectedPlayer, state.getCurrentPlayer());
                fm.next(state, fm.computeAvailableActions(state).get(0));
            }
            assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        }
    }


    @Test
    public void winInRoundTwoIsCorrectlyAllocated() {
        params.secondRoundStart = ToadParameters.SecondRoundStart.ONE;
        for (int i = 0; i < 16; i++) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(1, state.getRoundCounter());
        assertEquals(0, state.getCurrentPlayer());
        // attacker is player zero
        assertEquals(0, state.battlesWon[1][0]);
        assertEquals(0, state.battlesWon[1][1]);
        playCards(
                new ToadCard("Five", 5),  // Field
                new ToadCard("Six", 6),  // Flank
                new ToadCard("Three", 3), // field
                new ToadCard("Six", 6) // flank
        );
        assertEquals(1, state.battlesWon[1][0]);
        assertEquals(0, state.battlesWon[1][1]);

        assertEquals(1, state.getCurrentPlayer());
        // attacker is player one
        playCards(
                new ToadCard("Three", 3), // field
                new ToadCard("Six", 6),
                new ToadCard("Five", 5),  // Field
                new ToadCard("Six", 6)  // Flank
        );
        assertEquals(2, state.battlesWon[1][0]);
        assertEquals(0, state.battlesWon[1][1]);
    }

    @Test
    public void gameEndsAfterTwoRounds() {
        for (int i = 0; i < 16; i++) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(1, state.getRoundCounter());
        assertEquals(0, state.getTurnCounter());
        assertEquals(CoreConstants.GameResult.GAME_ONGOING, state.getGameStatus());
        for (int i = 0; i < 16; i++) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
    }

    @Test
    public void scoreUpdatesForVanillaCards() {
        playCards(
                new ToadCard("Five", 5),  // Field
                new ToadCard("Six", 6),  // Flank
                new ToadCard("Three", 3), // field
                new ToadCard("Six", 6) // flank
        );
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }

    @Test
    public void tiedBattlesAreUpdated() {
        playCards(
                new ToadCard("Five", 5),  // Field
                new ToadCard("Six", 6),  // Flank
                new ToadCard("Five", 5), // field
                new ToadCard("Six", 6) // flank
        );
        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
        assertEquals(2, state.battlesTied[0]);
    }

    @Test
    public void assassinAgainstSeven() {
        playCards(
                new ToadCard("Three", 3), // field
                new ToadCard("Assassin", 0, ASSASSIN),  // Flank
                new ToadCard("Five", 5),  // Field
                new ToadCard("Seven", 7) // flank
        );
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void assassinAgainstSix() {
        playCards(
                new ToadCard("Five", 5), // field
                new ToadCard("Assassin", 0, ASSASSIN),  // Flank
                new ToadCard("Five", 5),  // Field
                new ToadCard("Six", 6) // flank
        );
        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void bombAgainstFourAttack() {
        playCards(
                new ToadCard("Five", 5), // field
                new ToadCard("Saboteur", 4, SABOTEUR, new Saboteur()),
                new ToadCard("Five", 5),  // Field
                new ToadCard("Bomb", 0, BOMB)
        );
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }

    @Test
    public void bombAgainstFourDefense() {
        playCards(
                new ToadCard("Five", 5), // field
                new ToadCard("Bomb", 0, BOMB),  // Flank
                new ToadCard("Five", 5),  // Field
                new ToadCard("Four", 4, SABOTEUR, new SaboteurII())  // flank
        );

        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }


    @Test
    public void assaultCannonAgainstSixAttack() {
        playCards(
                new ToadCard("Five", 5),  // Field
                new ToadCard("Six", 6), // flank
                new ToadCard("Five", 5), // field
                new ToadCard("Bomb", 0, ASSAULT_CANNON) // Flank
        );

        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }

    @Test
    public void assaultCannonAgainstSixDefense() {
        playCards(
                new ToadCard("Five", 5), // field
                new ToadCard("AC", 0, ASSAULT_CANNON), // Flank
                new ToadCard("Five", 5),  // Field
                new ToadCard("Six", 6) // flank
        );

        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }


    @Test
    public void assaultCannonInRound2() {
        playCards(
                new ToadCard("Assassin", 0, ASSASSIN), // field
                new ToadCard("G2", 7, GENERAL_TWO), // Flank
                new ToadCard("Berserker", 5, BERSERKER),  // Field
                new ToadCard("G1", 7, GENERAL_ONE) // flank
        );
        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);

        assertEquals(1, state.getCurrentPlayer());
        playCards(
                new ToadCard("G2", 7, GENERAL_TWO), // field
                new ToadCard("Saboteur", 4, SABOTEUR, new Saboteur()),
                new ToadCard("AC", 0, ASSAULT_CANNON),  // Field
                new ToadCard("IconBearer", 6, ICON_BEARER) // flank
        );
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(2, state.battlesWon[0][1]);
    }

    @Test
    public void saboteurBeatsAssaultCannon() {
        playCards(
                new ToadCard("Five", 5), // field
                new ToadCard("AC", 0, ASSAULT_CANNON),  // Flank
                new ToadCard("Five", 5),  // Field
                new ToadCard("Saboteur", 4, SABOTEUR, new Saboteur())
        );

        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void saboteurIIBeatsAssaultCannon() {
        playCards(
                new ToadCard("Five", 5), // field
                new ToadCard("AC", 0, ASSAULT_CANNON),  // Flank
                new ToadCard("Five", 5),  // Field
                new ToadCard("Saboteur", 4, SABOTEUR, new SaboteurII())  // flank
        );

        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }


    @Test
    public void overcommit() {
        playCards(
                new ToadCard("Five", 5),  // Field
                new ToadCard("Seven", 7), // flank
                new ToadCard("Three", 3), // field
                new ToadCard("Six", 6)  // Flank
        );

        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(0, state.battlesWon[0][1]);
    }

    @Test
    public void pushback() {
        state.battlesWon[0][1] = 1;
        playCards(
                new ToadCard("Five", 5),  // Field
                new ToadCard("Seven", 7), // flank
                new ToadCard("Three", 3), // field
                new ToadCard("Six", 6)  // Flank
        );

        assertEquals(2, state.battlesWon[0][0]);
        assertEquals(1, state.battlesWon[0][1]);
    }

    @Test
    public void redeterminisationShufflesFlankButNotFieldCards() {
        playCards(
                new ToadCard("Five", 5),  // Field
                new ToadCard("Seven", 7), // flank
                new ToadCard("Three", 3) // field
        );
        state.hiddenFlankCards[1] = new ToadCard("Six", 6);
        ToadGameState copy = (ToadGameState) state.copy(0);
        assertEquals(state.fieldCards[0], copy.fieldCards[0]);
        assertEquals(state.fieldCards[1], copy.fieldCards[1]);
        assertEquals(state.hiddenFlankCards[0], copy.hiddenFlankCards[0]);
        assertNotEquals(state.hiddenFlankCards[1], copy.hiddenFlankCards[1]);
    }

    @Test
    public void redeterminisationShuffleOwnDeck() {
        ToadGameState copy = (ToadGameState) state.copy(1);
        assertEquals(state.getPlayerHand(1), copy.getPlayerHand(1));
        assertNotEquals(state.getPlayerDeck(1), copy.getPlayerDeck(1));
    }

    @Test
    public void redeterminisationShuffleOtherDeck() {
        ToadGameState copy = (ToadGameState) state.copy(1);
        assertNotEquals(state.getPlayerHand(0), copy.getPlayerHand(0));
        assertNotEquals(state.getPlayerDeck(0), copy.getPlayerDeck(0));
    }

    @Test
    public void winFirstLoseSecondLosesGame() {
        state.battlesWon[0][0] = 10;
        state.battlesWon[0][1] = 0;
        assertEquals(10, state.getGameScore(0), 0.001);
        assertEquals(0, state.getGameScore(1), 0.001);
        fm.endRound(state, 1);
        assertEquals(0, state.getGameScore(0), 0.001);
        assertEquals(0, state.getGameScore(1), 0.001);
        state.playerDecks.get(0).clear();
        state.playerHands.get(0).clear();
        state.playerDecks.get(1).clear();
        state.playerHands.get(1).clear();
        state.tieBreakers[0] = new ToadCard("Six", 6);
        state.tieBreakers[1] = new ToadCard("Five", 5);

        playCards(
                new ToadCard("Five", 5),  // Field
                new ToadCard("Seven", 7), // flank
                new ToadCard("Three", 3), // field
                new ToadCard("Six", 6)  // Flank
        );
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertEquals(0.0, state.getGameScore(0), 0.001);
        assertEquals(10.0, state.getGameScore(1), 0.001);
        assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void winningFirstAndTieOnSecondWins() {
        state.battlesWon[0][0] = 0;
        state.battlesWon[0][1] = 10;
        assertEquals(0, state.getGameScore(0), 0.001);
        assertEquals(10, state.getGameScore(1), 0.001);
        fm.endRound(state, 1);
        assertEquals(0, state.getGameScore(0), 0.001);
        assertEquals(0, state.getGameScore(1), 0.001);
        state.playerDecks.get(0).clear();
        state.playerHands.get(0).clear();
        state.playerDecks.get(1).clear();
        state.playerHands.get(1).clear();
        state.tieBreakers[1] = new ToadCard("Six", 6);
        state.tieBreakers[0] = new ToadCard("Five", 5);

        state.fieldCards[0] = new ToadCard("Five", 5);
        state.fieldCards[1] = new ToadCard("Three", 3);

        ToadCard flank0 = new ToadCard("Six", 6);
        ToadCard flank1 = new ToadCard("Seven", 7);
        state.hiddenFlankCards[1] = flank1;
        state.hiddenFlankCards[0] = flank0;
        state.playerHands.get(0).add(flank0);
        state.playerHands.get(1).add(flank1);

        fm._afterAction(state, null);
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertEquals(0.0, state.getGameScore(0), 0.001);
        assertEquals(10.0, state.getGameScore(1), 0.001);
        assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void tieBreaker() {
        state.battlesWon[0][0] = 3;
        state.battlesWon[0][1] = 3;
        assertEquals(3, state.getGameScore(0), 0.001);
        assertEquals(3, state.getGameScore(1), 0.001);
        fm.endRound(state, 1);
        assertEquals(0, state.getGameScore(0), 0.001);
        assertEquals(0, state.getGameScore(1), 0.001);
        state.playerDecks.get(0).clear();
        state.playerHands.get(0).clear();
        state.playerDecks.get(1).clear();
        state.playerHands.get(1).clear();
        state.tieBreakers[1] = new ToadCard("Six", 6);
        state.tieBreakers[0] = new ToadCard("Five", 5);

        state.fieldCards[0] = new ToadCard("Five", 5);
        state.fieldCards[1] = new ToadCard("Three", 3);

        ToadCard flank0 = new ToadCard("Six", 6);
        ToadCard flank1 = new ToadCard("Seven", 7);
        state.hiddenFlankCards[1] = flank1;
        state.hiddenFlankCards[0] = flank0;
        state.playerHands.get(0).add(flank0);
        state.playerHands.get(1).add(flank1);
        fm._afterAction(state, null);
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertEquals(5.0, state.getGameScore(0), 0.001);
        assertEquals(5.0, state.getGameScore(1), 0.001);
        assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[1]);
    }


    @Test
    public void absoluteTie() {
        state.battlesWon[0][0] = 3;
        state.battlesWon[0][1] = 3;
        assertEquals(3, state.getGameScore(0), 0.001);
        assertEquals(3, state.getGameScore(1), 0.001);
        fm.endRound(state, 1);
        assertEquals(0, state.getGameScore(0), 0.001);
        assertEquals(0, state.getGameScore(1), 0.001);
        state.playerDecks.get(0).clear();
        state.playerHands.get(0).clear();
        state.playerDecks.get(1).clear();
        state.playerHands.get(1).clear();
        state.tieBreakers[1] = new ToadCard("Five", 5);
        state.tieBreakers[0] = new ToadCard("Five", 5);

        state.fieldCards[0] = new ToadCard("Five", 5);
        state.fieldCards[1] = new ToadCard("Three", 3);

        ToadCard flank0 = new ToadCard("Six", 6);
        ToadCard flank1 = new ToadCard("Seven", 7);
        state.hiddenFlankCards[1] = flank1;
        state.hiddenFlankCards[0] = flank0;
        state.playerHands.get(0).add(flank0);
        state.playerHands.get(1).add(flank1);

        fm._afterAction(state, null);
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertEquals(5.0, state.getGameScore(0), 0.001);
        assertEquals(5.0, state.getGameScore(1), 0.001);
        assertEquals(CoreConstants.GameResult.DRAW_GAME, state.getPlayerResults()[0]);
        assertEquals(CoreConstants.GameResult.DRAW_GAME, state.getPlayerResults()[1]);
    }
}
