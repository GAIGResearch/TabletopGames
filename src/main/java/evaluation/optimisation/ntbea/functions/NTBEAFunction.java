package evaluation.optimisation.ntbea.functions;

public abstract class NTBEAFunction {
    public abstract double functionValue(double[] x);

    public abstract int dimension();
}

class Standard extends NTBEAFunction{

    NTBEAFunction fn;

    public Standard(String name) {
        if (name.equals("Branin"))
            fn = new Branin();
        else if (name.equals("Hartmann3"))
            fn = Hartmann.Hartmann3;
        else if (name.equals("Hartmann6"))
            fn = Hartmann.Hartmann6;
        else if (name.equals("GoldsteinPrice"))
            fn = new GoldsteinPrice();
        else if (name.equals("Test001"))
            fn = new TestFunction001();
        else if (name.equals("Test002"))
            fn = new TestFunction002();
        else
            throw new AssertionError("Unknown function name: " + name);
    }

    @Override
    public double functionValue(double[] x) {
        return fn.functionValue(x);
    }

    @Override
    public int dimension() {
        return fn.dimension();
    }
}

