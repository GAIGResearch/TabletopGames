package games.seasaltpaper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.*;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.seasaltpaper.actions.*;
import games.seasaltpaper.cards.*;
import games.seasaltpaper.SeaSaltPaperGameState.TurnPhase;
import utilities.Pair;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static core.CoreConstants.*;


public class SeaSaltPaperForwardModel extends StandardForwardModel {

    /**
     * Creates the initial game-state of Sea Salt and Paper.
     *
     * @param firstState - state to be modified
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) firstState;
        sspgs.drawPile = new Deck<>("drawPile", VisibilityMode.HIDDEN_TO_ALL);

        // TODO make discard pile partial observable (only top card visible)
        sspgs.discardPile1 = new Deck<>("discardPile1", VisibilityMode.VISIBLE_TO_ALL);
        sspgs.discardPile2 = new Deck<>("discardPile2", VisibilityMode.VISIBLE_TO_ALL);

        sspgs.playerHands = new ArrayList<>(sspgs.getNPlayers());
        sspgs.playerDiscards = new ArrayList<>(sspgs.getNPlayers());
        sspgs.playerTotalScores = new int[sspgs.getNPlayers()];
        sspgs.protectedHands = new boolean[sspgs.getNPlayers()];
        //sspgs.playerCurrentDuoPoints = new int[sspgs.getNPlayers()];
        // Set up first round
//        SeaSaltPaperGameState.GameCount++;
        setupRound(sspgs);
    }

    protected void setupRound(SeaSaltPaperGameState sspgs)
    {
        sspgs.currentPhase = TurnPhase.DRAW;
        sspgs.lastChance = -1;
        sspgs.drawPile.clear();
        sspgs.discardPile1.clear();
        sspgs.discardPile2.clear();
        sspgs.playerPlayedDuoPoints = new int[sspgs.getNPlayers()];
        for (int i=0; i<sspgs.getNPlayers(); i++) {
            sspgs.protectedHands[i] = false;
        }

        setupDrawPile(sspgs);

        // Set up player hands and discards
        if (sspgs.playerHands.isEmpty()) {
            for (int i = 0; i < sspgs.getNPlayers(); i++) {
                boolean[] visible = new boolean[sspgs.getNPlayers()];
                visible[i] = true;
                PartialObservableDeck<SeaSaltPaperCard> playerHand = new PartialObservableDeck<SeaSaltPaperCard>("playerHand"+i, i, visible);
                sspgs.playerHands.add(playerHand);

                Deck<SeaSaltPaperCard> playerDiscard = new Deck<>("PlayerHandDiscard"+i, i, VisibilityMode.VISIBLE_TO_ALL);
                sspgs.playerDiscards.add(playerDiscard);
            }
        }

        // Set-up discard piles
        sspgs.discardPile1.add(sspgs.drawPile.draw());
        sspgs.discardPile2.add(sspgs.drawPile.draw());

        // Reset player status
        for (int i = 0; i < sspgs.getNPlayers(); i++) {
            sspgs.setPlayerResult(GameResult.GAME_ONGOING, i);
            sspgs.playerHands.get(i).clear();
            sspgs.playerDiscards.get(i).clear();
        }

        // Update components in the game state
        sspgs.updateComponents();
    }

    private void setupDrawPile(SeaSaltPaperGameState gs) {
        SeaSaltPaperParameters parameters = (SeaSaltPaperParameters) gs.getGameParameters();

        for (Pair<CardSuite, CardType> p: parameters.cardsInit.keySet()) {
            int count = parameters.cardsInit.get(p).a;
            for (int i = 0; i < count; i++) {
                CardColor c = parameters.cardsInit.get(p).b[i];
                gs.drawPile.add(new SeaSaltPaperCard(c, p.a, p.b));
            }
        }
        gs.drawPile.shuffle(gs.getRnd());
    }

    private void processEndRound(SeaSaltPaperGameState gs) {
        if (gs.lastChance != -1 )
        {
            processEndRoundLastChance(gs);
        }
        else {
            for (int i=0; i<gs.getNPlayers(); i++) {
                gs.playerTotalScores[i] += HandManager.calculatePoint(gs, i);
            }
        }

        int max = 0;
        for (int i=0; i<gs.getNPlayers(); i++) {
            if (max < gs.playerTotalScores[i]) {
                max = gs.playerTotalScores[i];
            }
        }
        SeaSaltPaperParameters param = (SeaSaltPaperParameters) gs.getGameParameters();
        // Victory condition reached, end the game
        if (max >= param.victoryCondition[gs.getNPlayers() - 2]) {
            endGame(gs); // TODO implement 4 Mermaid insta win
            if (gs.saveState)
            {
                gameStateToJson(gs);
            }
        }
        else {
            int nextPlayer = (gs.getCurrentPlayer() + 1) % gs.getNPlayers();
            endRound(gs, nextPlayer);
            if (gs.saveState)
            {
                gameStateToJson(gs);
            }
            setupRound(gs);
        }
    }

    private void processEndRoundLastChance(SeaSaltPaperGameState gs) {
        int[] playerScores = new int[gs.getNPlayers()];
        int max = 0;
        for (int i=0; i<gs.getNPlayers(); i++) {
            playerScores[i] = HandManager.calculatePoint(gs, i);
            if (max < playerScores[i]) {
                max = playerScores[i];
            }
        }
        // LastChance BET WON
        if (playerScores[gs.lastChance] >= max) {
            for (int i=0; i<gs.getNPlayers(); i++) {
                gs.playerTotalScores[i] += HandManager.calculateColorBonus(gs, i);
                if (i == gs.lastChance) {
                    gs.playerTotalScores[i] += playerScores[i];
                }
            }
        }
        // LastChance BET LOSS
        else {
            for (int i=0; i<gs.getNPlayers(); i++) {
                if (i == gs.lastChance) {
                    gs.playerTotalScores[i] += HandManager.calculateColorBonus(gs, i);
                }
                else {
                    gs.playerTotalScores[i] += playerScores[i];
                }
            }
        }
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     *
     * @return - List of AbstractAction objects.
     */
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        //Test Placeholder
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) gameState;
        SeaSaltPaperParameters params = (SeaSaltPaperParameters) sspgs.getGameParameters();
        List<AbstractAction> actions = new ArrayList<>();
        switch (sspgs.currentPhase) {
            case DRAW: //TODO check what happen if draw pile is empty
                int currentPlayerHandId = sspgs.playerHands.get(sspgs.getCurrentPlayer()).getComponentID();
                // Draw from discard pile
                if (sspgs.discardPile1.getSize() != 0) {
                    actions.add(new SSPDrawCard(sspgs.discardPile1.getComponentID(), currentPlayerHandId));
                }
                if (sspgs.discardPile2.getSize() != 0) {
                    actions.add(new SSPDrawCard(sspgs.discardPile2.getComponentID(), currentPlayerHandId));
                }
                // Draw 2 from draw pile, then discard 1 to one of the discard pile
                if (sspgs.getDrawPile().getSize() > 0) {
                    actions.add(new DrawAndDiscard(sspgs.getCurrentPlayer(), params.numberOfCardsDraw, params.numberOfCardsDiscard));
                }
                if (actions.isEmpty()) {
                    actions.add(new DoNothing());
                }
                break;
            case DUO:
//                System.out.println("this is duo/stop phase");
                actions.addAll(HandManager.generateDuoActions(sspgs, sspgs.getCurrentPlayer()));
                actions.add(new DoNothing());
                if (HandManager.calculatePoint(sspgs, sspgs.getCurrentPlayer()) >= params.roundStopCondition && sspgs.lastChance == -1) {
                    actions.add(new Stop(gameState.getCurrentPlayer()));
                    actions.add(new LastChance(gameState.getCurrentPlayer()));
                }
                break;
            default:
                actions.add(new DoNothing());
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        if (gameState.isActionInProgress()) return;

//        System.out.println("ACTIONS EXECUTED XD!!");
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) gameState;
        if (action instanceof Stop) {
            processEndRound(sspgs);
            return;
        }
        if (!(action instanceof PlayDuo)) {
            sspgs.currentPhase = sspgs.currentPhase.next();
        }
        if (sspgs.currentPhase == TurnPhase.FINISH) // Current player's turn is finished, end the turn
        {
            if (sspgs.drawPile.getSize() == 0) { // End the round if drawPile is empty
                processEndRound(sspgs);
                return;
            }
            if (sspgs.lastChance != -1) {
                //Process LastChance stuff
                sspgs.protectedHands[sspgs.getCurrentPlayer()] = true;
                sspgs.getPlayerHands().get(sspgs.getCurrentPlayer()).setVisibility(VisibilityMode.VISIBLE_TO_ALL);
                // if all players have gone after LastChance
                if (sspgs.allProtected()) {
                    processEndRound(sspgs);
                    return;
                }
            }
            int nextPlayer = (sspgs.getCurrentPlayer() + 1) % sspgs.getNPlayers();
            endPlayerTurn(sspgs, nextPlayer);
            sspgs.currentPhase = sspgs.currentPhase.next();
            if (sspgs.saveState && (sspgs.getTurnCounter() % sspgs.saveCycle == 0))
            {
                gameStateToJson(sspgs);
            }
        }
    }

    private void gameStateToJson(SeaSaltPaperGameState gs) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        Gson gson = new Gson();
        SeaSaltPaperParameters params = (SeaSaltPaperParameters) gs.getGameParameters();
        SSPGameStateContainer gsContainer = new SSPGameStateContainer(gs);
        String fileName = "ssp-" + gs.getGameID() + "-" + gs.getRoundCounter() + "-" + gs.getTurnCounter() + ".json";
        String fileDir = params.dataPath + "/gameStates/";
        try (FileWriter f = new FileWriter(fileDir + fileName)) {
            gson.toJson(gsContainer, f);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
