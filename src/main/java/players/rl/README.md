# qWeights

The qWeights folder is generated for each game during Training, along with a `qWeightsDB.csv` file. Each entry in the .csv file can also be found in the corresponding `{id}.json` file. **These .json files can be renamed or deleted at will**. The database and filenames will automatically update next time `players.rl.DataProcessor` is run.

## Important

IDs are generated to be unique. Manually editing the .csv, or changing the .json files' metadata, *especially changing the IDs*, is **NOT** recommended.

## Automatic Generation and Maintenance of IDs and Database

 When running `players.rl.RLTrainer`, `players.rl.DataProcessor` updates the .csv to remove deleted files, and also updates all .json files names to start with their index. For example, if you rename the file `7.json` to `GoodWeights.json`, DataProcessor will then rename that file to `7_GoodWeights.json`. This simply ensures being able to easily identify the files when trying to find a file after looking into the .csv database.

Special examples of renaming. Assume that the ID in the metadata of the file is 7:
* "6_name.json" -> "7_6_name.json"
* "7name.json" -> "7_7name.json"