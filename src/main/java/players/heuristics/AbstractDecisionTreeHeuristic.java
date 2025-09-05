package players.heuristics;

import com.globalmentor.apache.hadoop.fs.BareLocalFileSystem;
import org.apache.hadoop.fs.FileSystem;
import org.apache.spark.ml.regression.DecisionTreeRegressionModel;
import org.apache.spark.sql.SparkSession;

public abstract class AbstractDecisionTreeHeuristic {

    static SparkSession spark = SparkSession
            .builder()
            .appName("Java Spark SQL basic example")
            //     .config("spark.driver.memory", "1g")
            .master("local").getOrCreate();
    static {
        // And the hack to get this to work on Windows (without the Winutils.exe and hadoop.dll nightmare)
        spark.sparkContext().hadoopConfiguration().setClass("fs.file.impl", BareLocalFileSystem.class, FileSystem.class);
    }

    protected DecisionTreeRegressionModel drModel;

    public AbstractDecisionTreeHeuristic(DecisionTreeRegressionModel drModel) {
        this.drModel = drModel;
    }

    public AbstractDecisionTreeHeuristic(String directory) {
        // load in the Decision Tree model from the directory
        if (directory == null || directory.isEmpty()) {
            System.out.println("No directory specified for Decision Tree model");
            return;
        }
        drModel = DecisionTreeRegressionModel.load(directory);
    }

    public static String prettifyDecisionTreeDescription(DecisionTreeRegressionModel model, String[] featureNames) {
        // the debug string of model contains labels of the form 'feature nn', where nn is the index of the feature
        // We want to replace these with the actual feature names
        // we go in reverse to stop replacing 'feature 10' with 'nameOfFeature0' etc.
        String debugString = model.toDebugString();
        for (int i = featureNames.length-1; i >= 0; i--) {
            debugString = debugString.replace("feature " + i, featureNames[i]);
        }
        return debugString;
    }

}
