package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class Revolt extends AbstractAction {
    public final int playerID;
    public final int locationID;
    public RootCard cardToDiscard1;
    public RootCard cardToDiscard2;
    public final boolean passSubGamePhase;

    public Revolt(int playerID, int location, RootCard cardToDiscard1, RootCard cardToDiscard2, boolean passSubGamePhase) {
        this.playerID = playerID;
        this.locationID = location;
        this.cardToDiscard1 = cardToDiscard1;
        this.cardToDiscard2 = cardToDiscard2;
        this.passSubGamePhase = passSubGamePhase;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if (!canExecute(state)) {
            System.out.println("Action cannot be executed because the current player does not correspond to the woodland alliance faction.");
            return false;
        }

        Deck<RootCard> hand = state.getSupporters();
        if (!attemptToDiscard(hand, state)) {
            return false;
        }

        RootBoardNodeWithRootEdges place = state.getGameMap().getNodeByID(locationID);
        if (!place.getSympathy()) {
            System.out.println("Trying to revolt at location which is not sympathetic");
            return false;
        }

        executeRevolt(state, place);
        return true;
    }

    private boolean canExecute(RootGameState state) {
        return playerID == state.getCurrentPlayer() &&
                state.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance;
    }

    private boolean attemptToDiscard(Deck<RootCard> hand, RootGameState state) {
        int index1 = findCardIndex(hand, cardToDiscard1);
        int index2 = findCardIndex(hand, cardToDiscard2);

        if (index1 == -1 || index2 == -1) {
            System.out.println("Required cards not found in hand for discarding.");
            return false;
        }
        state.getDiscardPile().add(hand.get(index1));
        state.getDiscardPile().add(hand.get(index2));
        hand.remove(index1);
        if (index1 < index2) index2--;
        hand.remove(index2);
        return true;
    }

    private int findCardIndex(Deck<RootCard> hand, RootCard card) {
        for (int i = 0; i < hand.getSize(); i++) {
            if (hand.get(i).equals(card)) {
                return i;
            }
        }
        return -1;
    }

    private void executeRevolt(RootGameState state, RootBoardNodeWithRootEdges place) {
        //Place Base
        place.build(getBuildingType(place.getClearingType()));

        //Add Warriors
        for (int e = 0; e < Math.min(state.getWoodlandWarriors(), state.getGameMap().getSympatheticClearingsOfTypeCount(place.getClearingType())); e++) {
            place.addWarrior(RootParameters.Factions.WoodlandAlliance);
            state.removeWoodlandWarrior();
        }

        //Add Officer
        if (state.getWoodlandWarriors() > 1) {
            try {
                state.addOfficer();
            } catch (Exception e) {
                System.out.println("Trying to add an officer with no available warriors");
            }
        }
        //Remove Other pieces
        //Cat
        for (int i = 0; i < place.getWarrior(RootParameters.Factions.MarquiseDeCat); i++) {
            place.removeWarrior(RootParameters.Factions.MarquiseDeCat);
            state.addCatWarrior();
        }
        for (int i = 0; i < place.getWorkshops(); i++) {
            place.removeBuilding(RootParameters.BuildingType.Workshop);
            state.addBuilding(RootParameters.BuildingType.Workshop);
            state.addGameScorePLayer(playerID,1);
        }
        for (int i = 0; i < place.getRecruiters(); i++) {
            place.removeBuilding(RootParameters.BuildingType.Recruiter);
            state.addBuilding(RootParameters.BuildingType.Recruiter);
            state.addGameScorePLayer(playerID,1);
        }
        for (int i = 0; i < place.getSawmill(); i++) {
            place.removeBuilding(RootParameters.BuildingType.Sawmill);
            state.addBuilding(RootParameters.BuildingType.Sawmill);
            state.addGameScorePLayer(playerID,1);
        }
        for (int i = 0; i < place.getWood(); i++) {
            place.removeWood();
            state.addWood();
            state.addGameScorePLayer(playerID,1);
        }

        //Bird
        for (int e = 0; e < place.getWarrior(RootParameters.Factions.EyrieDynasties); e++) {
            place.removeWarrior(RootParameters.Factions.EyrieDynasties);
            state.addBirdWarrior();
        }
        for (int e = 0; e < place.getRoost(); e++) {
            place.removeBuilding(RootParameters.BuildingType.Roost);
            state.addBuilding(RootParameters.BuildingType.Roost);
            state.addGameScorePLayer(playerID,1);
        }
    }

    private RootParameters.BuildingType getBuildingType(RootParameters.ClearingTypes clearingType) {
        switch (clearingType) {
            case Fox:
                return RootParameters.BuildingType.FoxBase;
            case Mouse:
                return RootParameters.BuildingType.MouseBase;
            case Rabbit:
                return RootParameters.BuildingType.RabbitBase;
            default:
                return null;
        }
    }

    @Override
    public AbstractAction copy() {
        return new Revolt(playerID, locationID, cardToDiscard1, cardToDiscard2, passSubGamePhase);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Revolt) {
            Revolt other = (Revolt) obj;
            return playerID == other.playerID && locationID==other.locationID && cardToDiscard1.equals(other.cardToDiscard1) && cardToDiscard2.equals(other.cardToDiscard2) && passSubGamePhase == other.passSubGamePhase;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Revolt", playerID, locationID, cardToDiscard1.hashCode(), cardToDiscard2.hashCode(), passSubGamePhase);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString()  + " revolts at " + gs.getGameMap().getNodeByID(locationID).identifier + " by discarding " + cardToDiscard1.cardtype.toString() + " and " + cardToDiscard2.cardtype.toString();
    }
}
