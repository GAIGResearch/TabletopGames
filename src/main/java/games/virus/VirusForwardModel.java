package games.virus;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.virus.actions.*;
import games.virus.cards.*;
import games.virus.components.VirusBody;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.VERBOSE;
import static games.virus.cards.VirusCard.OrganType.Wild;
import static games.virus.cards.VirusCard.OrganType.None;

public class VirusForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        // 1. Each player has a body
        // 2. Each player has a deck with 3 cards
        // 3. There is a draw deck with 68 cards at the beginning.
        //    21 organs, 17 viruses, 20 medicines, 10 treatments
        // 4. There is a discard card. Empty at the beginning.

        VirusGameState vgs = (VirusGameState) firstState;

        vgs.playerBodies = new ArrayList<>(vgs.getNPlayers());

        // Create an empty body for each player
        for (int i=0; i<vgs.getNPlayers(); i++) {
            vgs.playerBodies.add(new VirusBody());
        }

        // Create the draw deck with all the cards
        vgs.drawDeck = new Deck<>("DrawDeck", -1);
        createCards(vgs);

        vgs.drawDeck.shuffle(new Random(vgs.getGameParameters().getRandomSeed()));

        // Create the discard deck, at the beginning it is empty
        vgs.discardDeck = new Deck<>("DiscardDeck", -1);

        // Draw initial cards to each player
        vgs.playerDecks = new ArrayList<>(vgs.getNPlayers());
        drawCardsToPlayers(vgs);
    }

    @Override
    protected void _next(AbstractGameState gameState, AbstractAction action) {
        action.execute(gameState);
        checkGameEnd((VirusGameState)gameState);
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING)
            gameState.getTurnOrder().endPlayerTurn(gameState);
    }

    /**
     * Create all the cards and include them into the drawPile
     * @param vgs - Virus game state
     */
    private void createCards(VirusGameState vgs) {
        VirusGameParameters vgp = (VirusGameParameters) vgs.getGameParameters();

        // Organs, Virus and Medicine cards
        for (VirusCard.OrganType organ: VirusCard.OrganType.values()) {
            if (organ != None && organ != Wild) {
                for (int i=0; i<vgp.nCardsPerOrgan; i++)
                    vgs.drawDeck.add(new VirusCard(organ, VirusCard.VirusCardType.Organ));

                for (int i=0; i<vgp.nCardsPerVirus; i++)
                    vgs.drawDeck.add(new VirusCard(organ, VirusCard.VirusCardType.Virus));

                for (int i=0; i<vgp.nCardsPerMedicine; i++)
                    vgs.drawDeck.add(new VirusCard(organ, VirusCard.VirusCardType.Medicine));
            }
        }

        //  Wilds cards
        for (int i=0; i<vgp.nCardsPerWildOrgan; i++)
            vgs.drawDeck.add(new VirusCard(Wild, VirusCard.VirusCardType.Organ));

        for (int i=0; i<vgp.nCardsPerWildVirus; i++)
            vgs.drawDeck.add(new VirusCard(Wild, VirusCard.VirusCardType.Virus));

        for (int i=0; i<vgp.nCardsPerWildMedicine; i++)
            vgs.drawDeck.add(new VirusCard(Wild, VirusCard.VirusCardType.Medicine));

        // Treatment cards
        for (int i=0; i<vgp.nCardsPerTreatmentSpreading; i++)
            vgs.drawDeck.add(new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.Spreading));

        for (int i=0; i<vgp.nCardsPerTreatmentTransplant; i++)
            vgs.drawDeck.add(new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.Transplant));

        for (int i=0; i<vgp.nCardsPerTreatmentOrganThief; i++)
            vgs.drawDeck.add(new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.OrganThief));

        for (int i=0; i<vgp.nCardsPerTreatmentLatexGlove; i++)
            vgs.drawDeck.add(new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.LatexGlove));

        for (int i=0; i<vgp.nCardsPerTreatmentMedicalError; i++)
            vgs.drawDeck.add(new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.MedicalError));
    }

    /**
     * Draw cards to players
     * @param vgs - Virus game state
     */
    private void drawCardsToPlayers(VirusGameState vgs) {
        for (int i = 0; i < vgs.getNPlayers(); i++) {
            String playerDeckName = "Player" + i + "Deck";
            vgs.playerDecks.add(new Deck<>(playerDeckName, i));
            for (int j = 0; j < 3; j++) {
                vgs.playerDecks.get(i).add(vgs.drawDeck.draw());
            }
        }
    }

    /**
     * Check if the game is end. A player wins when have, at least, 4 organs not infected
     * @param vgs - Virus game state
     * @return - Nothing
     */
    // TODO: add wild organ
    public void checkGameEnd(VirusGameState vgs) {
        for (int i = 0; i < vgs.getNPlayers(); i++) {
            if (vgs.playerBodies.get(i).areAllOrganHealthy()) {
                for (int j = 0; j < vgs.getNPlayers(); j++) {
                    if (i == j)
                        vgs.setPlayerResult(Utils.GameResult.WIN, i);
                    else
                        vgs.setPlayerResult(Utils.GameResult.LOSE, j);
                }
                vgs.setGameStatus(Utils.GameResult.GAME_END);
                break;
            }
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        VirusGameState vgs = (VirusGameState) gameState;
        ArrayList<AbstractAction> actions    = new ArrayList<>();
        VirusGameParameters vgp = (VirusGameParameters) vgs.getGameParameters();
        int                player     = vgs.getCurrentPlayer();
        Deck<VirusCard>    playerHand = vgs.playerDecks.get(player);

        // Playable cards actions:
        for (int i = 0; i < playerHand.getSize(); i++)
            addActionsForCard(vgs, playerHand.peek(i), actions, playerHand);

        // Create DiscardCard actions. The player can always discard 1, 2 or 3 cards
        // Discard one card
        for (int i = 0; i < vgp.maxCardsDiscard; i++)
            actions.add(new ReplaceOneCard(playerHand.getComponentID(), vgs.discardDeck.getComponentID(), i, vgs.drawDeck.getComponentID()));

        // Discard two cards:
        // TODO: add discard two cards actions
        // Discard all cards
        actions.add(new ReplaceCards(playerHand.getComponentID(), vgs.discardDeck.getComponentID(), playerHand.getSize(), vgs.drawDeck.getComponentID()));

        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new VirusForwardModel();
    }

    /**
     * Compute possible actions given a card from player hand and add to actions
     * Organ:    Add if it does not exit yet in the player's body
     * Medicine: Apply only in own body if organ exists and it is not immunised yet.
     * Virus:    It can be applied in other players' organ, if exist and they are not immunised yet.
     * @param gameState - Virus game state
     * @param card - card that can be played
     * @param actions - list of actions
     * @param playerHand - Player hand
     * @return - Nothing
     */
    // TODO: WILD and treatments cards
    private void addActionsForCard(VirusGameState gameState, VirusCard card, ArrayList<AbstractAction> actions, Deck<VirusCard> playerHand) {
        int playerID = gameState.getCurrentPlayer();
        int cardIdx = playerHand.getComponents().indexOf(card);
        switch (card.type) {
            case Organ: {
                VirusBody myBody = gameState.playerBodies.get(playerID);
                if (!myBody.hasOrgan(card.organ))
                    actions.add(new AddOrgan(playerHand.getComponentID(), gameState.discardDeck.getComponentID(), cardIdx, myBody.getComponentID()));
                break;
            }
            case Medicine: {
                VirusBody myBody = gameState.playerBodies.get(playerID);
                if (myBody.hasOrgan(card.organ) && !myBody.hasOrganImmunised(card.organ))
                    actions.add(new ApplyMedicine(playerHand.getComponentID(), gameState.discardDeck.getComponentID(), cardIdx, myBody.getComponentID()));
                break;
            }
            case Virus:
                for (int i = 0; i < gameState.getNPlayers(); i++) {
                    if (i != playerID) {
                        VirusBody itsBody = gameState.playerBodies.get(i);
                        if (itsBody.hasOrgan(card.organ) && !itsBody.hasOrganImmunised(card.organ))
                            actions.add(new ApplyVirus(playerHand.getComponentID(), gameState.discardDeck.getComponentID(), cardIdx, itsBody.getComponentID()));
                    }
                }
                break;
        }
    }

    @Override
    protected void endGame(AbstractGameState gameState) {
        if (VERBOSE) {
            System.out.println("Game Results:");
            for (int playerID = 0; playerID < gameState.getNPlayers(); playerID++) {
                if (gameState.getPlayerResults()[playerID] == Utils.GameResult.WIN) {
                    System.out.println("The winner is the player : " + playerID);
                    break;
                }
            }
        }
    }
}
