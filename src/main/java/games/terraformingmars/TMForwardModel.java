package games.terraformingmars;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModelWithTurnOrder;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import core.components.GridBoard;
import games.terraformingmars.actions.*;
import games.terraformingmars.components.Award;
import games.terraformingmars.components.Milestone;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.components.TMMapTile;
import games.terraformingmars.rules.requirements.TagOnCardRequirement;
import utilities.Vector2D;

import java.util.*;

import static games.terraformingmars.TMGameState.TMPhase.*;
import static games.terraformingmars.TMTypes.Resource.MegaCredit;
import static games.terraformingmars.TMTypes.Resource.TR;
import static games.terraformingmars.TMTypes.StandardProject.*;
import static games.terraformingmars.TMTypes.ActionType.*;

public class TMForwardModel extends StandardForwardModelWithTurnOrder {

    @Override
    protected void _setup(AbstractGameState firstState) {
        TMGameState gs = (TMGameState) firstState;
        TMGameParameters params = (TMGameParameters) firstState.getGameParameters();

        gs.playerResources = new HashMap[gs.getNPlayers()];
        gs.playerProduction = new HashMap[gs.getNPlayers()];
        gs.playerResourceMap = new HashSet[gs.getNPlayers()];
        gs.playerDiscountEffects = new HashMap[gs.getNPlayers()];
        gs.playerResourceIncreaseGen = new HashMap[gs.getNPlayers()];

        for (int i = 0; i < gs.getNPlayers(); i++) {
            gs.playerResources[i] = new HashMap<>();
            gs.playerProduction[i] = new HashMap<>();
            gs.playerResourceIncreaseGen[i] = new HashMap<>();
            for (TMTypes.Resource res : TMTypes.Resource.values()) {
                int startingRes = params.startingResources.get(res);
                if (res == TR && gs.getNPlayers() == 1) {
                    startingRes = params.soloTR;
                }
                gs.playerResources[i].put(res, new Counter(startingRes, 0, params.maxPoints, res.toString() + "-" + i));
                if (params.startingProduction.containsKey(res)) {
                    int startingProduction = params.startingProduction.get(res);
                    if (params.expansions.contains(TMTypes.Expansion.CorporateEra))
                        startingProduction = 0;  // No production in corporate era
                    gs.playerProduction[i].put(res, new Counter(startingProduction, params.minimumProduction.get(res), params.maxPoints, res + "-prod-" + i));
                }
                gs.playerResourceIncreaseGen[i].put(res, false);
            }
            gs.playerResourceMap[i] = new HashSet<>();
            // By default, players can exchange steel for X MC and titanium for X MC. More may be added
            gs.playerResourceMap[i].add(new TMGameState.ResourceMapping(TMTypes.Resource.Steel, TMTypes.Resource.MegaCredit, params.nSteelMC, new TagOnCardRequirement(new TMTypes.Tag[]{TMTypes.Tag.Building})));
            gs.playerResourceMap[i].add(new TMGameState.ResourceMapping(TMTypes.Resource.Titanium, TMTypes.Resource.MegaCredit, params.nTitaniumMC, new TagOnCardRequirement(new TMTypes.Tag[]{TMTypes.Tag.Space})));

            // Set up player discount maps
            gs.playerDiscountEffects[i] = new HashMap<>();
        }

        gs.projectCards = new Deck<>("Projects", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        gs.corpCards = new Deck<>("Corporations", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        gs.discardCards = new Deck<>("Discard", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);

        // Load info from expansions (includes base)
        gs.board = new GridBoard(params.boardSize, params.boardSize);
        gs.extraTiles = new HashSet<>();
        gs.bonuses = new HashSet<>();
        gs.milestones = new HashSet<>();
        gs.awards = new HashSet<>();
        gs.globalParameters = new HashMap<>();

        // Load base
        TMTypes.Expansion.Base.loadProjectCards(gs.projectCards);
        TMTypes.Expansion.Base.loadCorpCards(gs.corpCards);
        TMTypes.Expansion.Base.loadBoard(gs.board, gs.extraTiles, gs.bonuses, gs.milestones, gs.awards, gs.globalParameters);

        if (params.expansions.contains(TMTypes.Expansion.Hellas) || params.expansions.contains(TMTypes.Expansion.Elysium)) {
            // Clear milestones and awards, they'll be replaced by these expansions
            gs.milestones.clear();
            gs.awards.clear();
        }

        for (TMTypes.Expansion e : params.expansions) {
            if (e != TMTypes.Expansion.Hellas && e != TMTypes.Expansion.Elysium) {
                // Hellas and Elysium don't have project or corporation cards
                e.loadProjectCards(gs.projectCards);
                e.loadCorpCards(gs.corpCards);
            }
            e.loadBoard(gs.board, gs.extraTiles, gs.bonuses, gs.milestones, gs.awards, gs.globalParameters);
        }

//        TMCard cccc = null;
//        try {
//            GsonBuilder gsonBuilder = new GsonBuilder()
//                    .registerTypeAdapter(Requirement.class, new SimpleSerializer<Requirement>())
//                    .registerTypeAdapter(Effect.class, new SimpleSerializer<Effect>())
//                    .registerTypeAdapter(Discount.class, new Discount())
//                    .registerTypeAdapter(TMAction.class, new SimpleSerializer<TMAction>())
//                    ;
//
////            GsonBuilder gsonBuilder = new GsonBuilder().registerTypeAdapter(Requirement.class, new RequirementJSONSerializer());
//            Gson gson = gsonBuilder.setPrettyPrinting().create();
//
//            FileWriter fw = new FileWriter("data/terraformingmars/projectCards/jsonCardsCORP.json");
//            fw.write("[");
//            for (TMCard c: gs.projectCards.getComponents()) {
//                cccc = c;
//                TMCard cCopy = c.copySerializable();
//                String jsonString = gson.toJson(cCopy);
//                fw.write(jsonString + ",");
//                System.out.println(jsonString);
//                fw.flush();
//            }
//            fw.write("]");
//            fw.flush();
//            fw.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println(cccc.toString());
//        }

        if (gs.getNPlayers() == 1) {
            // Disable milestones and awards for solo play
            gs.milestones = new HashSet<>();
            gs.awards = new HashSet<>();
        }

        // Shuffle dekcs
        gs.projectCards.shuffle(gs.getRnd());
        gs.corpCards.shuffle(gs.getRnd());

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
            for (TMTypes.Tile t : TMTypes.Tile.values()) {
                gs.playerTilesPlaced[i].put(t, new Counter(0, 0, params.maxPoints, t.name() + " tiles placed player " + i));
            }
            gs.playerCardsPlayedTypes[i] = new HashMap<>();
            for (TMTypes.CardType t : TMTypes.CardType.values()) {
                gs.playerCardsPlayedTypes[i].put(t, new Counter(0, 0, params.maxPoints, t.name() + " cards played player " + i));
            }
            gs.playerCardsPlayedTags[i] = new HashMap<>();
            for (TMTypes.Tag t : TMTypes.Tag.values()) {
                gs.playerCardsPlayedTags[i].put(t, new Counter(0, 0, params.maxPoints, t.name() + " cards played player " + i));
            }
            gs.playerExtraActions[i] = new HashSet<>();
            gs.playerPersistingEffects[i] = new HashSet<>();
        }

        gs.nAwardsFunded = new Counter(0, 0, params.nCostAwards.length, "Awards funded");
        gs.nMilestonesClaimed = new Counter(0, 0, params.nCostMilestone.length, "Milestones claimed");

        // First thing to do is select corporations
        gs.setGamePhase(CorporationSelect);
        for (int i = 0; i < gs.getNPlayers(); i++) {
            // TODO: remove, used for testing corps
//            for (TMCard c: gs.corpCards.getComponents()) {
//                if (c.getComponentName().equals("UNITED NATIONS MARS INITIATIVE")) {
//                    gs.playerCardChoice[i].add(c);
//                }
//            }
            for (int j = 0; j < params.nCorpChoiceStart; j++) {
                gs.playerCardChoice[i].add(gs.corpCards.pick(0));
            }
        }

        // Solo setup: place X cities randomly, with 1 greenery adjacent each (no oxygen increase)
        if (gs.getNPlayers() == 1) {
            int boardH = gs.board.getHeight();
            int boardW = gs.board.getWidth();
            gs.getTurnOrder().setTurnOwner(1);
            for (int i = 0; i < params.soloCities; i++) {
                // Place city + greenery adjacent
                PlaceTile pt = new PlaceTile(1, TMTypes.Tile.City, TMTypes.MapTileType.Ground, true);
                List<AbstractAction> actions = pt._computeAvailableActions(gs);
                PlaceTile action = (PlaceTile) actions.get(gs.getRnd().nextInt(actions.size()));
                action.execute(gs);
                TMMapTile mt = (TMMapTile) gs.getComponentById(action.mapTileID);
                List<Vector2D> neighbours = PlaceTile.getNeighbours(new Vector2D(mt.getX(), mt.getY()));
                boolean placed = false;
                while (!placed) {
                    Vector2D v = neighbours.get(gs.getRnd().nextInt(neighbours.size()));
                    TMMapTile mtn = (TMMapTile) gs.board.getElement(v.getX(), v.getY());
                    if (mtn != null && mtn.getOwnerId() == -1 && mtn.getTileType() == TMTypes.MapTileType.Ground) {
                        mtn.setTilePlaced(TMTypes.Tile.Greenery, gs);
                        placed = true;
                    }
                }
            }
            gs.getTurnOrder().setTurnOwner(0);
            gs.globalParameters.get(TMTypes.GlobalParameter.Oxygen).setValue(0);
        }

        gs.generation = 1;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        TMGameState gs = (TMGameState) currentState;
        TMGameParameters params = (TMGameParameters) gs.getGameParameters();

        if (gs.getGamePhase() == CorporationSelect) {
            boolean allChosen = true;
            for (TMCard card : gs.getPlayerCorporations()) {
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
                        gs.playerCardChoice[i].add(gs.drawCard());
                    }
                    // TODO: remove, used for testing specific cards
//                    for (TMCard c: gs.projectCards.getComponents()) {
//                        if (c.getComponentName().equalsIgnoreCase("search for life")) {
//                            gs.playerCardChoice[i].add(c);
//                        }
//                    }
                }
            }
        } else if (gs.getGamePhase() == Research) {
            // Check if finished: no ore cards in card choice decks
            boolean allDone = true;
            for (Deck<TMCard> deck : gs.getPlayerCardChoice()) {
                if (deck.getSize() > 0) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) {
                gs.setGamePhase(Actions);
                gs.getTurnOrder().endRound(gs);
            }
        } else if (gs.getGamePhase() == Actions) {
            // Check if finished: all players passed
            if (((TMTurnOrder) gs.getTurnOrder()).nPassed == gs.getNPlayers()) {
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
                    gs.getPlayerResources()[i].get(TMTypes.Resource.MegaCredit).increment(gs.playerResources[i].get(TR).getValue());
                }

                // Check game end before next research phase
                if (checkGameEnd(gs)) {

                    if (gs.getNPlayers() == 1) {
                        // If solo, game goes for 14 generations regardless of global parameters
                        CoreConstants.GameResult won = CoreConstants.GameResult.WIN_GAME;
                        for (TMTypes.GlobalParameter p : gs.globalParameters.keySet()) {
                            if (p != null && p.countsForEndGame() && !gs.globalParameters.get(p).isMaximum())
                                won = CoreConstants.GameResult.LOSE_GAME;
                        }
                        gs.setGameStatus(CoreConstants.GameResult.GAME_END);
                        gs.setPlayerResult(won, 0);
                    } else {
                        endGame(gs);
                    }

                    return;
                }

                // Move to research phase
                gs.getTurnOrder().endRound(gs);
                gs.setGamePhase(Research);
                for (int j = 0; j < params.nProjectsResearch; j++) {
                    for (int i = 0; i < gs.getNPlayers(); i++) {
                        TMCard c = gs.drawCard();
                        if (c != null) {
                            gs.playerCardChoice[i].add(c);
                        } else {
                            break;
                        }
                    }
                }
                for (int i = 0; i < gs.getNPlayers(); i++) {
                    // Mark player actions unused
                    for (TMCard c : gs.playerComplicatedPointCards[i].getComponents()) {
                        c.actionPlayed = false;
                    }
                    // Reset resource increase
                    for (TMTypes.Resource res : TMTypes.Resource.values()) {
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
        TMGameState gs = (TMGameState) gameState;
        TMGameParameters params = (TMGameParameters) gs.getGameParameters();
        int player = gs.getCurrentPlayer();

        List<AbstractAction> possibleActions = getAllActions(gs);

        // Wrap actions that can actually be played and must be paid for
        for (AbstractAction aa : possibleActions) {
            TMAction a = (TMAction) aa;
            if (a != null && a.canBePlayed(gs)) {
                if (a.getCost() != 0) {
                    actions.add(new PayForAction(player, a));
                } else {
                    actions.add(a);
                }
            }
        }

        return actions;
    }

    /**
     * Bypass regular computeActions function call to list all actions possible in the current state, some of which
     * might not be playable at the moment. Requirements list on the action informs of why an action is not playable.
     * Used to display full information in the GUI for unplayable (but possible) actions.
     *
     * @param gs - current state
     * @return - list of all actions available, playable and not playable
     */
    public List<AbstractAction> getAllActions(TMGameState gs) {
        // If there is an action in progress (see IExtendedSequence), then delegate to that
        // Regular game loop calls will not reach here, but external calls (e.g. GUI, agents) will and need correct info
        if (gs.isActionInProgress()) {
            return gs.getActionsInProgress().peek()._computeAvailableActions(gs);
        }

        // Calculate all actions

        TMGameParameters params = (TMGameParameters) gs.getGameParameters();
        int player = gs.getCurrentPlayer();
        List<AbstractAction> possibleActions = new ArrayList<>();

        if (gs.getGamePhase() == CorporationSelect) {
            // Decide one card at a time, first one player, then the other
            Deck<TMCard> cardChoice = gs.getPlayerCardChoice()[player];
            if (cardChoice.getSize() == 0) {
                possibleActions.add(new TMAction(player));  // Pass
            } else {
                for (int i = 0; i < cardChoice.getSize(); i++) {
                    possibleActions.add(new BuyCard(player, cardChoice.get(i).getComponentID(), 0));
                }
            }
        } else if (gs.getGamePhase() == Research) {
            // Decide one card at a time, first one player, then the other
            Deck<TMCard> cardChoice = gs.getPlayerCardChoice()[player];
            if (cardChoice.getSize() == 0) {
                possibleActions.add(new TMAction(player));  // Pass
            } else {
                cardChoice.get(0).actionPlayed = false;
                BuyCard a = new BuyCard(player, cardChoice.get(0).getComponentID(), params.getProjectPurchaseCost());
                if (a.canBePlayed(gs)) {
                    possibleActions.add(a);
                }
                possibleActions.add(new DiscardCard(player, cardChoice.get(0).getComponentID(), true));
            }
        } else {

            if (gs.generation == 1) {
                // Check if any players have decided first action from corporations
                TMCard corpCard = gs.playerCorporations[player];
                if (corpCard.firstAction != null && !corpCard.firstActionExecuted) {
                    possibleActions.add(corpCard.firstAction);
                    return possibleActions;
                }
            }

            possibleActions.add(new TMAction(player));  // Can always just pass

            // Play a card actions
            for (int i = 0; i < gs.playerHands[player].getSize(); i++) {
                possibleActions.add(new PlayCard(player, gs.playerHands[player].get(i), false));
            }

            // Buy a standard project
            // - Discard cards for MC
            if (gs.playerHands[player].getSize() > 0) {
                possibleActions.add(new SellProjects(player));
            }

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

            // - Air Scraping, increase Venus parameter for 15MC, if Venus expansion enabled
            if (params.expansions.contains(TMTypes.Expansion.Venus)) {
                possibleActions.add(new ModifyGlobalParameter(StandardProject, MegaCredit, params.nCostVenus, TMTypes.GlobalParameter.Venus, 1, false));
            }

            // Claim a milestone
            int nMilestonesClaimed = gs.getnMilestonesClaimed().getValue();
            int milestoneCost = 0;
            if (!gs.getnMilestonesClaimed().isMaximum()) milestoneCost = params.getnCostMilestone()[nMilestonesClaimed];
            for (Milestone m : gs.milestones) {
                possibleActions.add(new ClaimAwardMilestone(player, m, milestoneCost));
            }
            // Fund an award
            int nAwardsFunded = gs.getnAwardsFunded().getValue();
            int awardCost = 0;
            if (!gs.getnAwardsFunded().isMaximum()) awardCost = params.getnCostAwards()[nAwardsFunded];
            for (Award a : gs.awards) {
                possibleActions.add(new ClaimAwardMilestone(player, a, awardCost));
            }

            // Use an active card action  - only 1, mark as used, then mark unused at the beginning of next generation
            possibleActions.addAll(gs.playerExtraActions[player]);

            // 8 plants into greenery tile
            possibleActions.add(new PlaceTile(TMTypes.BasicResourceAction.PlantToGreenery, params.getnCostGreeneryPlant(), player, TMTypes.Tile.Greenery, TMTypes.MapTileType.Ground));
            // 8 heat into temperature increase
            possibleActions.add(new ModifyGlobalParameter(BasicResourceAction, TMTypes.Resource.Heat, params.getnCostTempHeat(), TMTypes.GlobalParameter.Temperature, 1, false));
        }

        return possibleActions;
    }

    @Override
    protected void illegalActionPlayed(AbstractGameState gameState, AbstractAction action) {
        next(gameState, new TMAction(gameState.getCurrentPlayer()));
    }

    private boolean checkGameEnd(TMGameState gs) {
        boolean ended = true;
        if (gs.getNPlayers() == 1) {
            // If solo, game goes for 14 generations regardless of global parameters
            if (gs.generation < ((TMGameParameters) gs.getGameParameters()).soloMaxGen) ended = false;
        } else {
            for (TMTypes.GlobalParameter p : gs.globalParameters.keySet()) {
                if (p != null && p.countsForEndGame() && !gs.globalParameters.get(p).isMaximum()) ended = false;
            }
        }
//        if (!ended && gs.generation >= 50) ended = true;  // set max generation threshold
        return ended;
    }
}
