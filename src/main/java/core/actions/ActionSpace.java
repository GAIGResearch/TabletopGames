package core.actions;

import core.AbstractGameState;

import java.util.Objects;

/**
 * See {@link games.loveletter.LoveLetterForwardModel#_computeAvailableActions(AbstractGameState, ActionSpace)}
 * for example implementations of structured action spaces.
 */
public class ActionSpace {
    public final Structure structure;
    public final Flexibility flexibility;  // TODO: no agents to take advantage of this yet, not supported in any games
    public final Context context;

    public static ActionSpace Default = new ActionSpace();
    public final boolean isDefault() {
        return this.equals(Default);
    }

    public ActionSpace() {
        this.structure = Structure.Default;
        this.flexibility = Flexibility.Default;
        this.context = Context.Default;
    }
    public ActionSpace(Structure structure, Flexibility flexibility, Context context) {
        this.structure = structure;
        this.flexibility = flexibility;
        this.context = context;
    }
    public ActionSpace(Structure structure) {
        this.structure = structure;
        this.flexibility = Flexibility.Default;
        this.context = Context.Default;
    }
    public ActionSpace(Flexibility flexibility) {
        this.structure = Structure.Default;
        this.flexibility = flexibility;
        this.context = Context.Default;
    }
    public ActionSpace(Context context) {
        this.structure = Structure.Default;
        this.flexibility = Flexibility.Default;
        this.context = context;
    }

    public enum Structure {
        Default,
        Flat,
        Deep
    }
    public enum Flexibility {
        Default,
        Rigid,
        Elastic
    }
    public enum Context {
        Default,
        Independent,
        Dependent
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActionSpace)) return false;
        ActionSpace that = (ActionSpace) o;
        return structure == that.structure && flexibility == that.flexibility && context == that.context;
    }

    @Override
    public int hashCode() {
        return Objects.hash(structure, flexibility, context);
    }
}
