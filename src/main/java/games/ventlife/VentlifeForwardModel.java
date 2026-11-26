package games.ventlife;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.grid.HexCoord;
import core.components.grid.SparseGrid;
import games.ventlife.actions.CreatureStep;
import games.ventlife.actions.DraftSpecies;
import games.ventlife.actions.PlaceTile;
import games.ventlife.components.*;

import java.util.*;

public class VentlifeForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        VentlifeGameState state = (VentlifeGameState) firstState;
        VentlifeParameters params = state.getParams();
        int nPlayers = state.getNPlayers();

        state.field = new SparseGrid<>("Field");
        state.creatures = new SparseGrid<>("Creatures");
        state.tilesPlaced = 0;

        state.drawDeck = new Deck<>("Draw deck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        for (VentTile t : params.tilesFor(nPlayers))
            state.drawDeck.add(t);
        state.drawDeck.shuffle(state.getRnd());
        state.hands = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++) {
            Deck<VentTile> hand = new Deck<>("Hand of player " + p, p, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
            hand.add(state.drawDeck.draw());
            state.hands.add(hand);
        }

        state.speciesInPlay = new ArrayList<>();
        switch (params.speciesSelection) {
            case FIRST_GAME -> state.speciesInPlay.addAll(VentlifeUtils.FIRST_GAME_SPECIES);
            case RANDOM -> {
                List<Species> all = new ArrayList<>(List.of(Species.values()));
                Collections.shuffle(all, state.getRnd());
                state.speciesInPlay.addAll(all.subList(0, params.nSpecies));
            }
            // the species are drafted before the first tile, and the supplies filled once the draft is over
            case DRAFT -> {
            }
        }
        state.drafted = new ArrayList<>();
        fillSupplies(state);

        state.setGamePhase(params.speciesSelection == VentlifeParameters.SpeciesSelection.DRAFT
                ? VentlifeGameState.Phase.DRAFT : VentlifeGameState.Phase.PLACE_TILE);
    }

    /**
     * Gives every player tokensPerSpecies of each species in play, and none of the others.
     */
    private void fillSupplies(VentlifeGameState state) {
        state.supply = new int[state.getNPlayers()][Species.values().length];
        for (int p = 0; p < state.getNPlayers(); p++)
            for (Species s : state.speciesInPlay)
                state.supply[p][s.ordinal()] = state.getParams().tokensPerSpecies;
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        // the creature step and the covering decisions are sequences, which offer their own actions
        VentlifeGameState state = (VentlifeGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        if (state.getGamePhase() == VentlifeGameState.Phase.DRAFT) {
            for (Species s : Species.values())
                if (!state.drafted.contains(s))
                    actions.add(new DraftSpecies(s));
            return actions;
        }
        VentTile tile = state.hands.get(state.getCurrentPlayer()).peek();
        // the first tile could go anywhere, so it goes at the origin: only its orientation is a choice
        if (state.field.isEmpty()) {
            for (int o = 0; o < 6; o++)
                actions.add(new PlaceTile(tile, HexCoord.ORIGIN, o));
            return actions;
        }
        // the shore: uncovered positions next to the field. A seafloor tile touches the field along an edge exactly
        // when one of its hexes is on the shore, so its Smoker is on the shore or next to it
        Set<HexCoord> shore = new HashSet<>();
        Set<HexCoord> candidates = new HashSet<>();
        for (Map.Entry<HexCoord, HexCell> e : state.field.asMap().entrySet()) {
            if (e.getValue().terrain() == Terrain.BLACK_SMOKER)
                candidates.add(e.getKey());
            for (HexCoord n : e.getKey().neighbours())
                if (!state.field.contains(n))
                    shore.add(n);
        }
        for (HexCoord s : shore) {
            candidates.add(s);
            for (HexCoord n : s.neighbours())
                if (!state.field.contains(n))
                    candidates.add(n);
        }
        // sorted, so that equal states list their actions in the same order
        List<HexCoord> sorted = new ArrayList<>(candidates);
        Collections.sort(sorted);
        for (HexCoord smoker : sorted) {
            boolean seafloor = !state.field.contains(smoker);
            for (int o = 0; o < 6; o++)
                if (seafloor ? canPlaceOnSeafloor(state, shore, smoker, o) : VentlifeUtils.isPlateauPlacement(state, smoker, o))
                    actions.add(new PlaceTile(tile, smoker, o));
        }
        return actions;
    }

    /**
     * Whether a tile may go on the seafloor with its (uncovered) Smoker there: its other two hexes uncovered too, and
     * at least one of the three on the shore.
     */
    private boolean canPlaceOnSeafloor(VentlifeGameState state, Set<HexCoord> shore, HexCoord smoker, int orientation) {
        HexCoord left = smoker.neighbour((orientation + 1) % 6), right = smoker.neighbour(orientation);
        if (state.field.contains(left) || state.field.contains(right))
            return false;
        return shore.contains(smoker) || shore.contains(left) || shore.contains(right);
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        VentlifeGameState state = (VentlifeGameState) currentState;
        if (state.isActionInProgress())
            return;
        int player = state.getCurrentPlayer();
        if (state.getGamePhase() == VentlifeGameState.Phase.DRAFT) {
            afterDraftPick(state);
            return;
        }
        // after the tile (and any covering), the creature step if the player can place anything; it is compulsory
        if (state.getGamePhase() == VentlifeGameState.Phase.PLACE_TILE) {
            CreatureStep step = new CreatureStep(player);
            if (step.hasPlacement(state)) {
                state.setGamePhase(VentlifeGameState.Phase.PLACE_CREATURES);
                state.setActionInProgress(step);
                return;
            }
        }
        state.setGamePhase(VentlifeGameState.Phase.PLACE_TILE);
        if (state.drawDeck.getSize() > 0)
            state.hands.get(player).add(state.drawDeck.draw());
        if (state.drawDeck.getSize() == 0 && state.hands.stream().allMatch(h -> h.getSize() == 0)) {
            endGame(state);
            return;
        }
        int next = (player + 1) % state.getNPlayers();
        if (next == state.getFirstPlayer())
            endRound(state);
        else
            endPlayerTurn(state, next);
    }

    private void afterDraftPick(VentlifeGameState state) {
        int nPlayers = state.getNPlayers();
        if (state.drafted.size() < VentlifeUtils.draftPicks(state.getParams().nSpecies, nPlayers)) {
            endPlayerTurn(state, (state.getCurrentPlayer() + 1) % nPlayers);
            return;
        }
        // at 3 players the species drafted are those not used; the first player then places the first tile, whoever
        // made the last pick
        for (Species s : Species.values())
            if (state.drafted.contains(s) != (nPlayers == 3))
                state.speciesInPlay.add(s);
        fillSupplies(state);
        state.setGamePhase(VentlifeGameState.Phase.PLACE_TILE);
        endPlayerTurn(state, state.getFirstPlayer());
    }
}
