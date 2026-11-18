package games.ventlife;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import games.ventlife.actions.CreatureStep;
import games.ventlife.actions.DraftSpecies;
import games.ventlife.actions.PlaceTile;
import games.ventlife.components.*;

import java.util.*;

public class VentlifeForwardModel extends StandardForwardModel {

    private static final Hex ORIGIN = new Hex(0, 0);

    @Override
    protected void _setup(AbstractGameState firstState) {
        VentlifeGameState state = (VentlifeGameState) firstState;
        VentlifeParameters params = state.getParams();
        int nPlayers = state.getNPlayers();

        state.field = new HashMap<>();
        state.creatures = new HashMap<>();
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
                actions.add(new PlaceTile(tile, ORIGIN, o));
            return actions;
        }
        // the shore: uncovered positions next to the field. A seafloor tile touches the field along an edge exactly
        // when one of its hexes is on the shore, so its Smoker is on the shore or next to it
        Set<Hex> shore = new HashSet<>();
        Set<Hex> candidates = new HashSet<>();
        for (Map.Entry<Hex, HexCell> e : state.field.entrySet()) {
            if (e.getValue().terrain() == Terrain.BLACK_SMOKER)
                candidates.add(e.getKey());
            for (Hex n : e.getKey().neighbours())
                if (!state.field.containsKey(n))
                    shore.add(n);
        }
        for (Hex s : shore) {
            candidates.add(s);
            for (Hex n : s.neighbours())
                if (!state.field.containsKey(n))
                    candidates.add(n);
        }
        // sorted, so that equal states list their actions in the same order
        List<Hex> sorted = new ArrayList<>(candidates);
        sorted.sort(VentlifeUtils.HEX_ORDER);
        for (Hex smoker : sorted) {
            boolean seafloor = !state.field.containsKey(smoker);
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
    private boolean canPlaceOnSeafloor(VentlifeGameState state, Set<Hex> shore, Hex smoker, int orientation) {
        Hex left = smoker.neighbour((orientation + 1) % 6), right = smoker.neighbour(orientation);
        if (state.field.containsKey(left) || state.field.containsKey(right))
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
