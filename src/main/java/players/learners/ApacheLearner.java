package players.learners;

import com.globalmentor.apache.hadoop.fs.BareLocalFileSystem;
import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;
import org.apache.hadoop.fs.FileSystem;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

public abstract class ApacheLearner extends AbstractLearner {

    Dataset<Row> apacheData;
    boolean debug = false;

    static SparkSession spark = SparkSession
            .builder()
            .appName("Java Spark SQL basic example")
            //     .config("spark.driver.memory", "1g")
            .master("local").getOrCreate();
    static {
        // And the hack to get this to work on Windows (without the Winutils.exe and hadoop.dll nightmare)
        spark.sparkContext().hadoopConfiguration().setClass("fs.file.impl", BareLocalFileSystem.class, FileSystem.class);
    }

    public ApacheLearner() {
        super();
    }
    public ApacheLearner(double gamma, Target target, IStateFeatureVector stateFeatureVector) {
        super(gamma, target, stateFeatureVector);
    }
    public ApacheLearner(double gamma, Target target, IStateFeatureVector stateFeatureVector, IActionFeatureVector actionFeatureVector) {
        super(gamma, target, stateFeatureVector, actionFeatureVector);
    }


    @Override
    public Object learnFrom(String... files) {
        loadData(files);
        // first add the target to the data array so that we can convert to an apache dataset (we just add on the target)
        double[][] apacheDataArray = new double[dataArray.length][dataArray[0].length];
        for (int i = 0; i < dataArray.length; i++) {
            // we skip the BIAS at the front here, as we add that in separately
            System.arraycopy(dataArray[i], 1, apacheDataArray[i], 0, descriptions.length);
            apacheDataArray[i][descriptions.length] = target[i][0]; // add target to end
        }
        // convert the raw data into Rows
        List<Row> rowList = Arrays.stream(apacheDataArray)
                .map(doubleArray -> Arrays.stream(doubleArray).boxed().toArray())
                .map(RowFactory::create)
                .collect(toList());
        // use the header to get the names, and all of them are double by design
        String[] apacheHeader = new String[descriptions.length + 1];
        System.arraycopy(descriptions, 0, apacheHeader, 0, descriptions.length);
        apacheHeader[descriptions.length] = "target";
        // set up the column names
        StructType schema = new StructType(Arrays.stream(apacheHeader)
                .map(name -> new StructField(name, DataTypes.DoubleType, true, Metadata.empty()))
                .toArray(StructField[]::new)
        );

        // and convert to an apache Dataset
        apacheData = spark.createDataFrame(rowList, schema);

        if (debug)
            apacheData.show(10);

        return learnFromApacheData();
    }

    abstract Object learnFromApacheData();

}
