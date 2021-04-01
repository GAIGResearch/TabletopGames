package games.terraformingmars;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import core.components.GridBoard;
import games.terraformingmars.actions.*;
import games.terraformingmars.components.Award;
import games.terraformingmars.components.Milestone;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.requirements.TagOnCardRequirement;
import utilities.Utils;

import java.util.*;

import static games.terraformingmars.TMGameState.TMPhase.*;
import static games.terraformingmars.TMTypes.StandardProject.*;
import static games.terraformingmars.TMTypes.ActionType.*;

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
                if (params.startingProduction.containsKey(res)) {
                    gs.playerProduction[i].put(res, new Counter(params.startingProduction.get(res), params.minimumProduction.get(res), params.maxPoints, res.toString() + "-prod-" + i));
                }
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
        gs.playerComplicatedPointCards = new Deck[gs.getNPlayers()];
        gs.playedCards = new Deck[gs.getNPlayers()];
        gs.playerCardPoints = new Counter[gs.getNPlayers()];
        for (int i = 0; i < gs.getNPlayers(); i++) {
            gs.playerHands[i] = new Deck<>("Hand of p" + i, i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
            gs.playerCardChoice[i] = new Deck<>("Card Choice for p" + i, i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
            gs.playerComplicatedPointCards[i] = new Deck<>("Resource or Points Cards Played by p" + i, i, CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
            gs.playedCards[i] = new Deck<>("Other Cards Played by p" + i, i, CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
            gs.playerCardPoints[i] = new Counter(0, 0, params.maxPoints, "Points of p" + i);
        }

        gs.playerTilesPlaced = new HashMap[gs.getNPlayers()];
        gs.playerCardsPlayedTypes = new HashMap[gs.getNPlayers()];
        gs.playerCardsPlayedTags = new HashMap[gs.getNPlayers()];
        gs.playerExtraActions = new HashSet[gs.getNPlayers()];
        gs.playerPersistingEffects = new HashSet[gs.getNPlayers()];
        for (int i = 0; i < gs.getNPlayers(); i++) {
            gs.playerTilesPlaced[i] = new HashMap<>();
            for (TMTypes.Tile t: TMTypes.Tile.values()) {
                gs.playerTilesPlaced[i].put(t, new Counter(0, 0, params.maxPoints, t.name() + " tiles placed player " + i));
            }
            gs.playerCardsPlayedTypes[i] = new HashMap<>();
            for (TMTypes.CardType t: TMTypes.CardType.values()) {
                gs.playerCardsPlayedTypes[i].put(t, new Counter(0,0,params.maxPoints,t.name() + " cards played player " + i));
            }
            gs.playerCardsPlayedTags[i] = new HashMap<>();
            for (TMTypes.Tag t: TMTypes.Tag.values()) {
                gs.playerCardsPlayedTags[i].put(t, new Counter(0, 0, params.maxPoints, t.name() + " cards played player " + i));
            }
            gs.playerExtraActions[i] = new HashSet<>();
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
                        if (res.isPlayerBoardRes()) {
                            gs.getPlayerResources()[i].get(res).increment(gs.getPlayerProduction()[i].get(res).getValue());
                        }
                    }
                    // TR also adds to mega credits
                    gs.getPlayerResources()[i].get(TMTypes.Resource.MegaCredit).increment(gs.playerResources[i].get(TMTypes.Resource.TR).getValue());
                }

                // Check game end before next research phase
                if (checkGameEnd(gs)) {
                    gs.setGameStatus(Utils.GameResult.GAME_END);
                    ArrayList<Integer> best = new ArrayList<>();
                    int bestPoints = 0;
                    for (int i = 0; i < gs.getNPlayers(); i++) {
                        int points = gs.countPoints(i);
                        if (points > bestPoints) {
                            bestPoints = points;
                        }
                    }
                    for (int i = 0; i < gs.getNPlayers(); i++) {
                        int points = gs.countPoints(i);
                        if (points == bestPoints) {
                            best.add(i);
                        }
                    }
                    // TODO tiebreaker
                    for (int i = 0; i < gs.getNPlayers(); i++) {
                        if (best.contains(i)) {
                            gs.setPlayerResult(Utils.GameResult.WIN, i);
                        } else {
                            gs.setPlayerResult(Utils.GameResult.LOSE, i);
                        }
                    }
                    return;
                }

                // Move to research phase
                gs.getTurnOrder().endRound(gs);
                gs.setGamePhase(Research);
                for (int i = 0; i < gs.getNPlayers(); i++) {
                    for (int j = 0; j < params.nProjectsResearch; j++) {
                        gs.playerCardChoice[i].add(gs.projectCards.pick(0));
                    }
                    // Mark player actions unused
                    for (TMAction a : gs.playerExtraActions[i]) {
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
                    actions.add(new BuyCard(player, cardChoice.get(i).getComponentID(), 0));
                }
            }
        } else if (gs.getGamePhase() == Research) {
            // Decide one card at a time, first one player, then the other
            Deck<TMCard> cardChoice = gs.getPlayerCardChoice()[player];
            if (cardChoice.getSize() == 0) {
                actions.add(new TMAction(player));  // Pass
            } else {
                BuyCard a = new BuyCard(player, cardChoice.get(0).getComponentID(), params.getProjectPurchaseCost());
                if (a.canBePlayed(gs)) {
                    actions.add(a);
                }
                actions.add(new DiscardCard(player, cardChoice.get(0).getComponentID()));
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

            ArrayList<TMAction> possibleActions = getAllActions(gs);

            // Wrap actions that can actually be played and must be paid for
            for (TMAction a: possibleActions) {
                if (a.canBePlayed(gs)) {
                    int cost = a.getCost();
                    int cardID = a.getCardID();  // -1 if no card
                    if (cost > 0) {
                        actions.add(new PayForAction(a.actionType, player, a, a.getCostResource(), cost, cardID));
                    } else {
                        actions.add(a);
                    }
                }
            }
        }

        return actions;
    }

    public ArrayList<TMAction> getAllActions(TMGameState gs) {
        TMGameParameters params = (TMGameParameters) gs.getGameParameters();
        int player = gs.getCurrentPlayer();
        ArrayList<TMAction> possibleActions = new ArrayList<>();

        possibleActions.add(new TMAction(player));  // Can always just pass

        // Play a card actions
        for (int i = 0; i < gs.playerHands[player].getSize(); i++) {
            possibleActions.add(new PlayCard(player, gs.playerHands[player].get(i), false));
        }

        // Buy a standard project
        // - Discard cards for MC TODO
        // - Increase energy production 1 step for 11 MC
        possibleActions.add(new ModifyPlayerResource(PowerPlant, params.getnCostSPEnergy(), player, 1, TMTypes.Resource.Energy));

        // - Increase temperature 1 step for 14 MC
        possibleActions.add(new ModifyGlobalParameter(StandardProject, TMTypes.Resource.MegaCredit, params.getnCostSPTemp(), TMTypes.GlobalParameter.Temperature, 1, false));

        // - Place ocean tile for 18 MC
        possibleActions.add(new PlaceTile(Aquifer, params.getnCostSPOcean(), player, TMTypes.Tile.Ocean, TMTypes.MapTileType.Ocean));

        // - Place greenery tile for 23 MC
        possibleActions.add(new PlaceTile(Greenery, params.getnCostSPGreenery(), player, TMTypes.Tile.Greenery, TMTypes.MapTileType.Ground));

        // - Place city tile and increase MC prod by 1 for 25 MC
        TMAction a1 = new PlaceTile(player, TMTypes.Tile.City, TMTypes.MapTileType.Ground, true);
        TMAction a2 = new ModifyPlayerResource(player, params.nSPCityMCGain, TMTypes.Resource.MegaCredit, true);
        possibleActions.add(new CompoundAction(StandardProject, player, new TMAction[]{a1, a2}, params.nCostSPCity));

        // Claim a milestone
        int milestoneCost = params.getnCostMilestone()[gs.getnMilestonesClaimed().getValue()];
        for (Milestone m : gs.milestones) {
            possibleActions.add(new ClaimAwardMilestone(player, m, milestoneCost));
        }
        // Fund an award
        int awardCost = params.getnCostAwards()[gs.getnAwardsFunded().getValue()];
        for (Award a : gs.awards) {
            possibleActions.add(new ClaimAwardMilestone(player, a, awardCost));
        }

        // Use an active card action  - only 1, mark as used, then mark unused at the beginning of next generation
        possibleActions.addAll(gs.playerExtraActions[player]);

        // 8 plants into greenery tile
        possibleActions.add(new PlaceTile(TMTypes.BasicResourceAction.PlantToGreenery, params.getnCostGreeneryPlant(), player, TMTypes.Tile.Greenery, TMTypes.MapTileType.Ground));
        // 8 heat into temperature increase
        possibleActions.add(new ModifyGlobalParameter(BasicResourceAction, TMTypes.Resource.Heat, params.getnCostTempHeat(), TMTypes.GlobalParameter.Temperature, 1, false));

        return possibleActions;
    }

    @Override
    protected void illegalActionPlayed(AbstractGameState gameState, AbstractAction action) {
        next(gameState, new TMAction(gameState.getCurrentPlayer()));
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
