package core.interfaces;

public interface ILearner {

    Object learnFrom(String... files);

    /**
     * Write the model to the file system.
     * This may either be a single file (in which case prefix is the file stem) or a directory (in which case prefix is the directory name)
     * @param prefix
     */
    void writeToFile(String prefix);

    String name();
}
