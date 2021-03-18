package games.terraformingmars;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import core.components.GridBoard;
import games.terraformingmars.actions.*;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.*;
import games.terraformingmars.rules.effects.Effect;
import games.terraformingmars.rules.requirements.TagOnCardRequirement;

import java.util.*;

import static games.terraformingmars.TMGameState.TMPhase.*;
import static games.terraformingmars.TMGameState.getEmptyTilesOfType;

public class TMForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        TMGameState gs = (TMGameState) firstState;
        TMGameParameters params = (TMGameParameters) firstState.getGameParameters();
        Random rnd = new Random(params.getRandomSeed());

        gs.playerResources = new HashMap[gs.getNPlayers()];
        gs.playerProduction = new HashMap[gs.getNPlayers()];
        gs.playerResourceMap = new HashSet[gs.getNPlayers()];
        gs.playerDiscountEffects = new HashMap[gs.getNPlayers()];
        gs.playerResourceIncreaseGen = new HashMap[gs.getNPlayers()];

        for (int i = 0; i < gs.getNPlayers(); i++) {
            gs.playerResources[i] = new HashMap<>();
            gs.playerProduction[i] = new HashMap<>();
            gs.playerResourceIncreaseGen[i] = new HashMap<>();
            for (TMTypes.Resource res: TMTypes.Resource.values()) {
                gs.playerResources[i].put(res, new Counter(params.startingResources.get(res), 0, params.maxPoints, res.toString() + "-" + i));
                gs.playerProduction[i].put(res, new Counter(params.startingProduction.get(res), params.minimumProduction.get(res), params.maxPoints, res.toString() + "-prod-" + i));
                gs.playerResourceIncreaseGen[i].put(res, false);
            }
            gs.playerResourceMap[i] = new HashSet<>();
            // By default, players can exchange steel for X MC and titanium for X MC. More may be added
            gs.playerResourceMap[i].add(new TMGameState.ResourceMapping(TMTypes.Resource.Steel, TMTypes.Resource.MegaCredit, params.nSteelMC, new TagOnCardRequirement(TMTypes.Tag.Building)));
            gs.playerResourceMap[i].add(new TMGameState.ResourceMapping(TMTypes.Resource.Titanium, TMTypes.Resource.MegaCredit, params.nTitaniumMC, new TagOnCardRequirement(TMTypes.Tag.Space)));

            // Set up player discount maps
            gs.playerDiscountEffects[i] = new HashMap<>();
        }

        gs.projectCards = new Deck<>("Projects", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        gs.corpCards = new Deck<>("Corporations", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        gs.discardCards = new Deck<>("Discard", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);

        // Load info from expansions (includes base)
        gs.board = new GridBoard<>(params.boardSize, params.boardSize);
        gs.extraTiles = new HashSet<>();
        gs.bonuses = new HashSet<>();
        gs.milestones = new HashSet<>();
        gs.awards = new HashSet<>();
        gs.globalParameters = new HashMap<>();
        for (TMTypes.Expansion e: params.expansions) {
            e.loadProjectCards(gs.projectCards);
            e.loadCorpCards(gs.corpCards);
            e.loadBoard(gs.board, gs.extraTiles, gs.bonuses, gs.milestones, gs.awards, gs.globalParameters);
        }

        // Shuffle dekcs
        gs.projectCards.shuffle(rnd);
        gs.corpCards.shuffle(rnd);

        HashMap<TMTypes.Tag, Counter>[] playerCardsPlayedTags;
        HashSet<AbstractAction>[] playerCardsPlayedEffects;
        HashSet<AbstractAction>[] playerCardsPlayedActions;
        HashMap<TMTypes.CardType, Counter>[] playerCardsPlayedTypes;
        HashMap<TMTypes.Tile, Counter>[] tilesPlaced;

        gs.playerCorporations = new TMCard[gs.getNPlayers()];
        gs.playerCardChoice = new Deck[gs.getNPlayers()];
        gs.playerHands = new Deck[gs.getNPlayers()];
        for (int i = 0; i < gs.getNPlayers(); i++) {
            gs.playerHands[i] = new Deck<>("Hand Player " + i, i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
            gs.playerCardChoice[i] = new Deck<>("Card Choice Player " + i, i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
        }

        gs.tilesPlaced = new HashMap[gs.getNPlayers()];
        gs.playerCardsPlayedTypes = new HashMap[gs.getNPlayers()];
        gs.playerCardsPlayedTags = new HashMap[gs.getNPlayers()];
        gs.playerCardsPlayedActions = new HashSet[gs.getNPlayers()];
        gs.playerCardsPlayedEffects = new HashSet[gs.getNPlayers()];
        gs.playerPersistingEffects = new HashSet[gs.getNPlayers()];
        for (int i = 0; i < gs.getNPlayers(); i++) {
            gs.tilesPlaced[i] = new HashMap<>();
            for (TMTypes.Tile t: TMTypes.Tile.values()) {
                gs.tilesPlaced[i].put(t, new Counter(0, 0, params.maxPoints, t.name() + " tiles placed player " + i));
            }
            gs.playerCardsPlayedTypes[i] = new HashMap<>();
            for (TMTypes.CardType t: TMTypes.CardType.values()) {
                gs.playerCardsPlayedTypes[i].put(t, new Counter(0,0,params.maxPoints,t.name() + " cards played player " + i));
            }
            gs.playerCardsPlayedTags[i] = new HashMap<>();
            for (TMTypes.Tag t: TMTypes.Tag.values()) {
                gs.playerCardsPlayedTags[i].put(t, new Counter(0, 0, params.maxPoints, t.name() + " cards played player " + i));
            }
            gs.playerCardsPlayedActions[i] = new HashSet<>();
            gs.playerCardsPlayedEffects[i] = new HashSet<>();
            gs.playerPersistingEffects[i] = new HashSet<>();
        }

        gs.nAwardsFunded = new Counter(0,0, params.nCostAwards.length,"Awards funded");
        gs.nMilestonesClaimed = new Counter(0, 0, params.nCostMilestone.length, "Milestones claimed");

        // First thing to do is select corporations
        gs.setGamePhase(CorporationSelect);
        for (int i = 0; i < gs.getNPlayers(); i++) {
            for (int j = 0; j < params.nCorpChoiceStart; j++) {
                gs.playerCardChoice[i].add(gs.corpCards.pick(0));
            }
        }

        gs.generation = 1;
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        TMGameState gs = (TMGameState)currentState;
        TMGameParameters params = (TMGameParameters) gs.getGameParameters();
        int player = gs.getCurrentPlayer();

        // Execute action
        action.execute(currentState);

        if (gs.getGamePhase() == CorporationSelect) {
            boolean allChosen = true;
            for (TMCard card: gs.getPlayerCorporations()) {
                if (card == null) {
                    allChosen = false;
                    break;
                }
            }
            if (allChosen) {
                gs.setGamePhase(Research);
                gs.getTurnOrder().endRound(gs);
                for (int i = 0; i < gs.getNPlayers(); i++) {
                    for (int j = 0; j < params.nProjectsStart; j++) {
                        gs.playerCardChoice[i].add(gs.projectCards.pick(0));
                    }
                }
            }
        } else if (gs.getGamePhase() == Research) {
            // Check if finished: no ore cards in card choice decks
            boolean allDone = true;
            for (Deck<TMCard> deck: gs.getPlayerCardChoice()) {
                if (deck.getSize() > 0) {
                    allDone = false;
                }
            }
            if (allDone) {
                gs.setGamePhase(Actions);
                gs.getTurnOrder().endRound(gs);
            }
        } else if (gs.getGamePhase() == Actions) {
            // Check if finished: all players passed
            if (((TMTurnOrder)gs.getTurnOrder()).nPassed == gs.getNPlayers()) {
                // Production
                for (int i = 0; i < gs.getNPlayers(); i++) {
                    // First, energy turns to heat
                    gs.getPlayerResources()[i].get(TMTypes.Resource.Heat).increment(gs.getPlayerResources()[i].get(TMTypes.Resource.Energy).getValue());
                    gs.getPlayerResources()[i].get(TMTypes.Resource.Energy).setValue(0);
                    // Then, all production values are added to resources
                    for (TMTypes.Resource res : TMTypes.Resource.values()) {
                        gs.getPlayerResources()[i].get(res).increment(gs.getPlayerProduction()[i].get(res).getValue());
                    }
                    // TR also adds to mega credits
                    gs.getPlayerResources()[i].get(TMTypes.Resource.MegaCredit).increment(gs.playerResources[i].get(TMTypes.Resource.TR).getValue());
                }

                // TODO check game end before next research phase
                // Move to research phase
                gs.getTurnOrder().endRound(gs);
                gs.setGamePhase(Research);
                for (int i = 0; i < gs.getNPlayers(); i++) {
                    for (int j = 0; j < params.nProjectsResearch; j++) {
                        gs.playerCardChoice[i].add(gs.projectCards.pick(0));
                    }
                    // Mark player actions unused
                    for (TMAction a : gs.playerCardsPlayedActions[i]) {
                        a.played = false;
                    }
                    // Reset resource increase
                    for (TMTypes.Resource res: TMTypes.Resource.values()) {
                        gs.playerResourceIncreaseGen[i].put(res, false);
                    }
                }

                // Next generation
                gs.generation++;
            }
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        // play a card (if valid), standard projects, claim milestone, fund award, card actions, 8 plants -> greenery, 8 heat -> temperature, pass
        // event cards are face-down after played, tags don't apply!
        ArrayList<AbstractAction> actions = new ArrayList<>();
        TMGameState gs = (TMGameState)gameState;
        TMGameParameters params = (TMGameParameters) gs.getGameParameters();
        int player = gs.getCurrentPlayer();

        if (gs.getGamePhase() == CorporationSelect) {
            // Decide one card at a time, first one player, then the other
            Deck<TMCard> cardChoice = gs.getPlayerCardChoice()[player];
            if (cardChoice.getSize() == 0) {
                actions.add(new TMAction(player));  // Pass
            } else {
                for (int i = 0; i < cardChoice.getSize(); i++) {
                    actions.add(new BuyCard(player, i, true));
                }
            }
        } else if (gs.getGamePhase() == Research) {
            // Decide one card at a time, first one player, then the other
            Deck<TMCard> cardChoice = gs.getPlayerCardChoice()[player];
            if (cardChoice.getSize() == 0) {
                actions.add(new TMAction(player));  // Pass
            } else {
                actions.add(new BuyCard(player, 0, true));
                actions.add(new DiscardCard(player, 0, true));
            }
        } else {
            if (gs.generation == 1) {
                // Check if any players have decided first action from corporations
                TMCard corpCard = gs.playerCorporations[player];
                if (corpCard.firstAction != null) {
                    actions.add(corpCard.firstAction);
                    corpCard.firstAction = null;
                    return actions;
                }
            }

            actions.add(new TMAction(player));  // Can always just pass
            // Play a card actions
            for (int i = 0; i < gs.playerHands[player].getSize(); i++) {
                TMCard card = gs.playerHands[player].get(i);
                boolean canPlayerPay = gs.canPlayerPay(player, card, null, TMTypes.Resource.MegaCredit, card.cost);
                if (canPlayerPay && (card.requirement == null || card.requirement.testCondition(gs))) {
                    actions.add(new PayForAction(player, new PlayCard(player, i, false), TMTypes.Resource.MegaCredit, card.cost, i));
                }
            }

            // Buy a standard project
            // - Discard cards for MC TODO
            // - Increase energy production 1 step for 11 MC
            Counter c = gs.playerProduction[player].get(TMTypes.Resource.Energy);
            if (gs.canPlayerPay(player, null, null, TMTypes.Resource.MegaCredit, params.nCostSPEnergy)) {
                actions.add(new PayForAction(player, new PlaceholderModifyCounter(player, 1, TMTypes.Resource.Energy, true, false),
                        TMTypes.Resource.MegaCredit, params.nCostSPEnergy, -1));
            }
            // - Increase temperature 1 step for 14 MC
            Counter temp = gs.globalParameters.get(TMTypes.GlobalParameter.Temperature);
            if (temp != null && !temp.isMaximum() && gs.canPlayerPay(player, null, null, TMTypes.Resource.MegaCredit, params.nCostSPTemp)) {
                actions.add(new PayForAction(player, new ModifyGlobalParameter(player, TMTypes.GlobalParameter.Temperature, 1, false),
                        TMTypes.Resource.MegaCredit, params.nCostSPTemp, -1));
            }
            // - Place ocean tile for 18 MC
            if (gs.canPlayerPay(player, null, null, TMTypes.Resource.MegaCredit, params.nCostSPOcean)) {
                actions.add(new PayForAction(player, new PlaceTile(player, TMTypes.Tile.Ocean, getEmptyTilesOfType(gs, TMTypes.MapTileType.Ocean), false),
                        TMTypes.Resource.MegaCredit, params.nCostSPOcean, -1));
            }
            // - Place greenery tile for 23 MC
            if (gs.canPlayerPay(player, null, null, TMTypes.Resource.MegaCredit, params.nCostSPGreenery)) {
                actions.add(new PayForAction(player, new PlaceTile(player, TMTypes.Tile.Greenery, null, false),
                        TMTypes.Resource.MegaCredit, params.nCostSPGreenery, -1));
            }
            // - Place city tile and increase MC prod by 1 for 25 MC TODO increase MC prod by 1
            if (gs.canPlayerPay(player, null, null, TMTypes.Resource.MegaCredit, params.nCostSPCity)) {
                actions.add(new PayForAction(player, new PlaceTile(player, TMTypes.Tile.City, null, false),
                        TMTypes.Resource.MegaCredit, params.nCostSPCity, -1));
            }

            // Claim a milestone
            if (!gs.getnMilestonesClaimed().isMaximum()) {
                int cost = params.nCostMilestone[gs.nMilestonesClaimed.getValue()];
                for (Milestone m : gs.milestones) {
                    if (m.canClaim(gs, player) && gs.canPlayerPay(player, null, null, TMTypes.Resource.MegaCredit, cost)) {
                        actions.add(new PayForAction(player, new ClaimAwardMilestone(player, m), TMTypes.Resource.MegaCredit, cost, -1));
                    }
                }
            }
            // Fund an award
            if (!gs.getnAwardsFunded().isMaximum()) {
                int cost = params.nCostAwards[gs.nAwardsFunded.getValue()];
                for (Award a : gs.awards) {
                    if (a.canClaim(gs, player) && gs.canPlayerPay(player, null, null, TMTypes.Resource.MegaCredit, cost)) {
                        actions.add(new PayForAction(player, new ClaimAwardMilestone(player, a), TMTypes.Resource.MegaCredit, cost, -1));
                    }
                }
            }

            // Use an active card action  - only 1, mark as used, then mark unused at the beginning of next generation
            for (TMAction a: gs.playerCardsPlayedActions[player]) {
                if (!a.played && (a.requirement == null || a.requirement.testCondition(gs))) {
                    if (a instanceof PayForAction) {
                        // Check if player can afford it
                        PayForAction aa = (PayForAction) a;
                        TMCard card = null;
                        if (aa.cardIdx != -1) {
                            card = gs.playerHands[player].get(aa.cardIdx);
                        }
                        if (gs.canPlayerPay(player, card, null, aa.resourceToPay, aa.costTotal)) {
                            actions.add(a);
                        }
                    }
                }
            }

            // 8 plants into greenery tile
            if (gs.canPlayerPay(player, null, null, TMTypes.Resource.Plant, params.nCostGreeneryPlant)) {
                actions.add(new PayForAction(player, new PlaceTile(player, TMTypes.Tile.Greenery, null, false),
                        TMTypes.Resource.Plant, params.nCostGreeneryPlant, -1));
            }
            // 8 heat into temperature increase
            if (temp != null && !temp.isMaximum() && gs.canPlayerPay(player, null, null, TMTypes.Resource.Heat, params.nCostTempHeat)) {
                actions.add(new PayForAction(player, new ModifyGlobalParameter(player, TMTypes.GlobalParameter.Temperature, 1, false),
                        TMTypes.Resource.Heat, params.nCostTempHeat, -1));
            }
        }

        return actions;
    }

    private boolean checkGameEnd(TMGameState gs) {
        boolean ended = true;
        for (TMTypes.GlobalParameter p: gs.globalParameters.keySet()) {
            if (p != null && p.countsForEndGame() && !gs.globalParameters.get(p).isMaximum()) ended = false;
        }
        return ended;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new TMForwardModel();
    }

}
