package games.virus;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import games.virus.actions.*;
import games.virus.cards.VirusCard;
import games.virus.cards.VirusTreatmentCard;
import games.virus.components.VirusBody;

import java.util.*;

import static core.CoreConstants.VisibilityMode;
import static games.virus.cards.VirusCard.OrganType.Treatment;
import static games.virus.cards.VirusCard.OrganType.Wild;

// Official Rules
// https://tranjisgames.com/wp-content/uploads/2017/02/VIRUS-RULES-eng.pdf
public class VirusForwardModel extends StandardForwardModel {

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
        for (int i = 0; i < vgs.getNPlayers(); i++) {
            vgs.playerBodies.add(new VirusBody());
        }

        // Create the draw deck with all the cards
        vgs.drawDeck = new Deck<>("DrawDeck", -1, VisibilityMode.HIDDEN_TO_ALL);
        createCards(vgs);

        vgs.drawDeck.shuffle(vgs.getRnd());

        // Create the discard deck, at the beginning it is empty
        vgs.discardDeck = new Deck<>("DiscardDeck", -1, VisibilityMode.VISIBLE_TO_ALL);

        // Draw initial cards to each player
        vgs.playerDecks = new ArrayList<>(vgs.getNPlayers());
        drawCardsToPlayers(vgs);
    }

    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        VirusGameState vgs = (VirusGameState) gameState;
        checkGameEnd(vgs);
        if (gameState.isNotTerminal()) {
            if (vgs.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING)
                endPlayerTurn(gameState);
            if (gameState.getCurrentPlayer() == 0)
                endRound(gameState);
        }
    }

    /**
     * Create all the cards and include them into the drawPile
     *
     * @param vgs - Virus game state
     */
    private void createCards(VirusGameState vgs) {
        VirusGameParameters vgp = (VirusGameParameters) vgs.getGameParameters();

        // Organs, Virus and Medicine cards
        for (VirusCard.OrganType organ : VirusCard.OrganType.values()) {
            if (organ != Wild && organ != Treatment) {
                for (int i = 0; i < vgp.nCardsPerOrgan; i++)
                    vgs.drawDeck.add(new VirusCard(organ, VirusCard.VirusCardType.Organ));

                for (int i = 0; i < vgp.nCardsPerVirus; i++)
                    vgs.drawDeck.add(new VirusCard(organ, VirusCard.VirusCardType.Virus));

                for (int i = 0; i < vgp.nCardsPerMedicine; i++)
                    vgs.drawDeck.add(new VirusCard(organ, VirusCard.VirusCardType.Medicine));
            }
        }

        //  Wilds cards
        for (int i = 0; i < vgp.nCardsPerWildOrgan; i++)
            vgs.drawDeck.add(new VirusCard(Wild, VirusCard.VirusCardType.Organ));

        for (int i = 0; i < vgp.nCardsPerWildVirus; i++)
            vgs.drawDeck.add(new VirusCard(Wild, VirusCard.VirusCardType.Virus));

        for (int i = 0; i < vgp.nCardsPerWildMedicine; i++)
            vgs.drawDeck.add(new VirusCard(Wild, VirusCard.VirusCardType.Medicine));

        // Treatment cards
        for (int i = 0; i < vgp.nCardsPerTreatmentOrganThief; i++)
            vgs.drawDeck.add(new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.OrganThief));

        for (int i = 0; i < vgp.nCardsPerTreatmentSpreading; i++)
            vgs.drawDeck.add(new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.Spreading));

        for (int i = 0; i < vgp.nCardsPerTreatmentTransplant; i++)
            vgs.drawDeck.add(new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.Transplant));

        for (int i = 0; i < vgp.nCardsPerTreatmentLatexGlove; i++)
            vgs.drawDeck.add(new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.LatexGlove));

        for (int i = 0; i < vgp.nCardsPerTreatmentMedicalError; i++)
            vgs.drawDeck.add(new VirusTreatmentCard(VirusTreatmentCard.TreatmentType.MedicalError));
    }

    /**
     * Draw cards to players
     *
     * @param vgs - Virus game state
     */
    private void drawCardsToPlayers(VirusGameState vgs) {
        int nCards = ((VirusGameParameters) vgs.getGameParameters()).nCardsPlayerHand;
        for (int i = 0; i < vgs.getNPlayers(); i++) {
            String playerDeckName = "Player" + i + "Deck";
            vgs.playerDecks.add(new Deck<>(playerDeckName, i, VisibilityMode.VISIBLE_TO_OWNER));
            for (int j = 0; j < nCards; j++) {
                vgs.playerDecks.get(i).add(vgs.drawDeck.draw());
            }
        }
    }

    /**
     * Check if the game is end. A player wins when have, at least, 4 organs not infected
     *
     * @param vgs - Virus game state
     */
    public void checkGameEnd(VirusGameState vgs) {
        for (int i = 0; i < vgs.getNPlayers(); i++) {
            if (vgs.playerBodies.get(i).getNOrganHealthy() >= VirusCard.OrganType.values().length - 2) {
                endGame(vgs);
                break;
            }
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        VirusGameState vgs = (VirusGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        VirusGameParameters vgp = (VirusGameParameters) vgs.getGameParameters();
        int player = vgs.getCurrentPlayer();
        Deck<VirusCard> playerHand = vgs.playerDecks.get(player);

        // If the playerHand has no cards (it has suffered a LatexGlove) then play DrawNewPlayerHand
        if (playerHand.getSize() == 0) {
            actions.add(new DrawNewPlayerHand(playerHand.getComponentID()));
            return actions;
        }

        // Playable cards actions
        Set<VirusCard> uniqueCards = new LinkedHashSet<>(playerHand.getComponents());
        for (VirusCard card : uniqueCards)
            addActionsForCard(vgs, card, actions, playerHand);

        // Create DiscardCard actions. The player can always discard 1, 2 or 3 cards TODO: variable player hand card size
        // Discard one card
        for (int i = 0; i < vgp.maxCardsDiscard; i++)
            actions.add(new ReplaceOneCard(playerHand.getComponentID(), vgs.discardDeck.getComponentID(), i, vgs.drawDeck.getComponentID()));

        // Discard 2 cards
        ArrayList<Integer> cardsToBeDiscarded = new ArrayList<>();
        cardsToBeDiscarded.add(0);
        cardsToBeDiscarded.add(1);
        actions.add(new ReplaceCards(playerHand.getComponentID(), vgs.discardDeck.getComponentID(), cardsToBeDiscarded, vgs.drawDeck.getComponentID()));

        cardsToBeDiscarded.clear();
        cardsToBeDiscarded.add(0);
        cardsToBeDiscarded.add(2);
        actions.add(new ReplaceCards(playerHand.getComponentID(), vgs.discardDeck.getComponentID(), cardsToBeDiscarded, vgs.drawDeck.getComponentID()));

        cardsToBeDiscarded.clear();
        cardsToBeDiscarded.add(1);
        cardsToBeDiscarded.add(2);
        actions.add(new ReplaceCards(playerHand.getComponentID(), vgs.discardDeck.getComponentID(), cardsToBeDiscarded, vgs.drawDeck.getComponentID()));

        // Discard all cards
        actions.add(new ReplaceAllCards(playerHand.getComponentID(), vgs.discardDeck.getComponentID(), vgs.drawDeck.getComponentID(), playerHand.getSize()));

        return actions;
    }

    /**
     * Compute possible actions given a treatment card and add to actions
     *
     * @param gameState  - Virus game state
     * @param card       - card that can be played
     * @param actions    - list of actions to be filled
     * @param playerHand - Player hand
     */
    private void addActionForTreatmentCard(VirusGameState gameState, VirusTreatmentCard card, List<AbstractAction> actions, Deck<VirusCard> playerHand) {
        switch (card.treatmentType) {
            case OrganThief:
                addActionForOrganThief(gameState, card, actions, playerHand);
                break;
            case LatexGlove:
                addActionForLatexGlove(gameState, card, actions, playerHand);
                break;
            case Spreading:
                addActionForSpreading(gameState, card, actions, playerHand);
                break;
            case Transplant:
                addActionForTransplant(gameState, card, actions, playerHand);
                break;
            case MedicalError:
                addActionForMedicalError(gameState, card, actions, playerHand);
                break;
        }
    }

    /**
     * Compute possible actions given an OrganThief treatment card
     *
     * @param gameState  - Virus game state
     * @param card       - card that can be played
     * @param actions    - list of actions to be filled
     * @param playerHand - Player hand
     */
    private void addActionForOrganThief(VirusGameState gameState, VirusCard card, List<AbstractAction> actions, Deck<VirusCard> playerHand) {
        int playerId = gameState.getCurrentPlayer();
        int cardIdx = playerHand.getComponents().indexOf(card);
        VirusBody myBody = gameState.playerBodies.get(playerId);

        for (int otherPlayer = 0; otherPlayer < gameState.getNPlayers(); otherPlayer++) {
            if (otherPlayer != playerId) {
                VirusBody itsBody = gameState.playerBodies.get(otherPlayer);
                for (VirusCard.OrganType organ : VirusCard.OrganType.values()) {
                    if (itsBody.hasOrgan(organ) && itsBody.organNotYetImmunised(organ) && !myBody.hasOrgan(organ)) {
                        actions.add(new PlayOrganThief(playerHand.getComponentID(),
                                gameState.discardDeck.getComponentID(),
                                cardIdx,
                                myBody.getComponentID(),
                                itsBody.getComponentID(),
                                otherPlayer,
                                organ));
                    }
                }
            }
        }
    }

    private void addActionForLatexGlove(VirusGameState gameState, VirusCard card, List<AbstractAction> actions, Deck<VirusCard> playerHand) {
        int playerId = gameState.getCurrentPlayer();
        int cardIdx = playerHand.getComponents().indexOf(card);
        VirusBody myBody = gameState.playerBodies.get(playerId);
        for (int otherPlayer = 0; otherPlayer < gameState.getNPlayers(); otherPlayer++) {
            if (otherPlayer != playerId) {
                Deck<VirusCard> otherPlayerHand = gameState.playerDecks.get(otherPlayer);

                actions.add(new PlayLatexGlove(playerHand.getComponentID(),
                        gameState.discardDeck.getComponentID(),
                        cardIdx,
                        myBody.getComponentID(),
                        otherPlayer, otherPlayerHand.getComponentID()));
            }
        }
    }

    /**
     * Compute possible actions given a Spreading treatment card
     * NOTE: Only one virus can be spreading to another player (This is a simplification of the original game)
     *
     * @param gameState  - Virus game state
     * @param card       - card that can be played
     * @param actions    - list of actions to be filled
     * @param playerHand - Player hand
     */
    private void addActionForSpreading(VirusGameState gameState, VirusCard card, List<AbstractAction> actions, Deck<VirusCard> playerHand) {
        int playerId = gameState.getCurrentPlayer();
        int cardIdx = playerHand.getComponents().indexOf(card);
        VirusBody myBody = gameState.playerBodies.get(playerId);

        for (VirusCard.OrganType myOrganType : VirusCard.OrganType.values()) {
            if (myBody.hasOrgan(myOrganType) && myBody.hasOrganInfected(myOrganType)) {
                boolean isWild = myBody.hasOrganInfectedWild(myOrganType);
                for (int otherPlayerId = 0; otherPlayerId < gameState.getNPlayers(); otherPlayerId++) {
                    if (otherPlayerId != playerId) {
                        VirusBody otherBody = gameState.playerBodies.get(otherPlayerId);

                        for (VirusCard.OrganType otherOrganType : VirusCard.OrganType.values()) {
                            if (isWild) {
                                if (otherBody.hasOrganNeutral(otherOrganType)) {
                                    actions.add(new PlaySpreading(playerHand.getComponentID(),
                                            gameState.discardDeck.getComponentID(),
                                            cardIdx,
                                            myBody.getComponentID(),
                                            otherPlayerId,
                                            otherBody.getComponentID(),
                                            myOrganType,
                                            otherOrganType));
                                }
                            } else {
                                if (otherBody.hasOrganNeutral(otherOrganType) && (myOrganType == otherOrganType || otherOrganType == Wild)) {
                                    actions.add(new PlaySpreading(playerHand.getComponentID(),
                                            gameState.discardDeck.getComponentID(),
                                            cardIdx,
                                            myBody.getComponentID(),
                                            otherPlayerId,
                                            otherBody.getComponentID(),
                                            myOrganType,
                                            otherOrganType));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void addActionForTransplant(VirusGameState gameState, VirusCard card, List<AbstractAction> actions, Deck<VirusCard> playerHand) {
        int playerId = gameState.getCurrentPlayer();
        int cardIdx = playerHand.getComponents().indexOf(card);
        VirusBody myBody = gameState.playerBodies.get(playerId);

        for (VirusCard.OrganType myOrganType : VirusCard.OrganType.values()) {
            for (int otherPlayer = 0; otherPlayer < gameState.getNPlayers(); otherPlayer++) {
                if (otherPlayer != playerId) {
                    VirusBody otherBody = gameState.playerBodies.get(otherPlayer);
                    for (VirusCard.OrganType otherOrganType : VirusCard.OrganType.values()) {
                        if (myBody.hasOrgan(myOrganType) &&
                                myBody.organNotYetImmunised(myOrganType) &&
                                otherBody.hasOrgan(otherOrganType) &&
                                otherBody.organNotYetImmunised(otherOrganType) &&
                                !myBody.hasOrgan(otherOrganType) &&
                                !otherBody.hasOrgan(myOrganType)) {
                            actions.add(new PlayTransplant(playerHand.getComponentID(),
                                    gameState.discardDeck.getComponentID(),
                                    cardIdx,
                                    myBody.getComponentID(),
                                    otherBody.getComponentID(),
                                    playerId,
                                    otherPlayer,
                                    myOrganType,
                                    otherOrganType
                            ));
                        }
                    }
                }
            }
        }
    }

    private void addActionForMedicalError(VirusGameState gameState, VirusCard card, List<AbstractAction> actions, Deck<VirusCard> playerHand) {
        int playerId = gameState.getCurrentPlayer();
        int cardIdx = playerHand.getComponents().indexOf(card);
        VirusBody myBody = gameState.playerBodies.get(playerId);

        for (int otherPlayerId = 0; otherPlayerId < gameState.getNPlayers(); otherPlayerId++) {
            if (otherPlayerId != playerId) {
                VirusBody otherBody = gameState.playerBodies.get(otherPlayerId);
                actions.add(new PlayMedicalError(playerHand.getComponentID(),
                        gameState.discardDeck.getComponentID(),
                        cardIdx,
                        myBody.getComponentID(),
                        otherBody.getComponentID(),
                        playerId,
                        otherPlayerId));
            }
        }
    }


    /**
     * Compute possible actions given a card from player hand and add to actions
     * Organ:    Add if it does not exit yet in the player's body
     * Medicine: Apply only in own body if organ exists and it is not immunised yet.
     * Virus:    It can be applied in other players' organ, if exist and they are not immunised yet.
     *
     * @param gameState  - Virus game state
     * @param card       - card that can be played
     * @param actions    - list of actions to be filled
     * @param playerHand - Player hand
     */
    private void addActionsForCard(VirusGameState gameState, VirusCard card, List<AbstractAction> actions, Deck<VirusCard> playerHand) {

        if (card.type == VirusCard.VirusCardType.Treatment) {
            addActionForTreatmentCard(gameState, (VirusTreatmentCard) card, actions, playerHand);
            return;
        }

        // If the card is not a treatment
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
                // A medicine (not wild) card can be played if in the player's body, there is:
                // - An organ of the same type and it is has not been yet immunised
                // - An organ of any type has been infected or vaccinated with a wild card and is has not been yet immunised
                // - the wild organ and it has not been yet immunised
                // A medicine Wild can be played in any not yet immunised organ
                VirusBody myBody = gameState.playerBodies.get(playerID);
                for (VirusCard.OrganType organ : VirusCard.OrganType.values()) {
                    if (myBody.hasOrgan(organ) && myBody.organNotYetImmunised(organ)) {  // Organ exists and it not immunised
                        if (card.organ == VirusCard.OrganType.Wild ||                  // It is a medicine wild
                                organ == VirusCard.OrganType.Wild ||                   // It is the organ wild
                                organ == card.organ ||                                 // Same organ type
                                myBody.hasOrganVaccinatedWild(organ) ||                // The organ is vaccinated Wild
                                myBody.hasOrganInfectedWild(organ))                    // The organ is infected wild
                            actions.add(new ApplyMedicine(playerHand.getComponentID(), gameState.discardDeck.getComponentID(), cardIdx, myBody.getComponentID(), organ));
                    }
                }
                break;
            }
            case Virus:
                // A virus (not wild) card can be played if in the other player's body, there is:
                // - An organ of the same type and it is has not been yet immunised
                // - An organ of any type has been infected or vaccinated with a wild card and is has not been yet immunised
                // - the wild organ and it has not been yet immunised
                // A virus Wild can be played in any not yet immunised organ

                for (int i = 0; i < gameState.getNPlayers(); i++) {
                    if (i != playerID) {
                        VirusBody itsBody = gameState.playerBodies.get(i);
                        for (VirusCard.OrganType organ : VirusCard.OrganType.values()) {
                            if (itsBody.hasOrgan(organ) && itsBody.organNotYetImmunised(organ)) {  // Organ exists and it not immunised
                                if (card.organ == VirusCard.OrganType.Wild ||                  // It is a medicine wild
                                        organ == VirusCard.OrganType.Wild ||                   // It is the organ wild
                                        organ == card.organ ||                                 // Same organ type
                                        itsBody.hasOrganVaccinatedWild(organ) ||                // The organ is vaccinated Wild
                                        itsBody.hasOrganInfectedWild(organ))                    // The organ is infected wild
                                    actions.add(new ApplyVirus(playerHand.getComponentID(), gameState.discardDeck.getComponentID(), cardIdx, itsBody.getComponentID(), organ, i));
                            }
                        }
                    }
                }
                break;
        }
    }

}
