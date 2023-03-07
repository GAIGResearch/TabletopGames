package core.actions;

public class ActionSpace {
    public final Structure structure;
    public final Flexibility flexibility;
    public final Context context;

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
}
