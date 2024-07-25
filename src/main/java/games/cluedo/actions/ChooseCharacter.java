package games.cluedo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.cluedo.CluedoGameState;

import java.util.Objects;

public class ChooseCharacter extends AbstractAction {
    int player = -1;
    int character = -1;

    public ChooseCharacter(int playerId, int characterIndex) {
        player = playerId;
        character = characterIndex;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CluedoGameState cgs = (CluedoGameState) gs;
        if (player != -1 && character != -1) {
            cgs.characterToPlayerMap.put(character, player);
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new ChooseCharacter(player, character);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChooseCharacter)) return false;
        ChooseCharacter that = (ChooseCharacter) o;
        return player == that.player && character == that.character;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, character);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "ChooseCharacterAction{" +
                "player=" + (player != -1? player : "player-not-found") +
                "character=" + (character != -1? character : "character-not-found");
    }

    public Integer getCharacter() {
        return character;
    }
}
