package core.interfaces;

public interface ILearner {

    void learnFrom(String... files);

    void writeToFile(String file);

    String name();

}
