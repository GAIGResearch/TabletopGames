
# Executable Jar Files
These JAR files are built with all dependencies included. This is why they are rather large.

[http://www.tabletopgames.ai/wiki/running/](http://www.tabletopgames.ai/wiki/running/) has full details on these entry points.

Several of the games (especially if you are running with a GUI using FrontEnd) need access to data files, which are not part of the JARs. 

If you get an error due to being unable to find or open a file in <tt>data/...</tt> then this is the cause. 
You will need to create a local directory of <tt>data/...</tt> and copy in the files from corresponding directory in this repository.

## FrontEnd.jar
This will launch a window allowing you to select a game to play with a GUI, and specifying the detail of the agents.
Run using:
```dtd
java -jar FrontEnd.jar
```

## RunGames.jar
This will run a batch of games with the specified agents and report the desired metrics to file.
An example of running this is below, but for full details of the options available consult the [website](http://tabletopgames.ai/wiki/running/).
```dtd
java -jar RunGames.jar game=LoveLetter
        playerDirectory=Battlelore_Agents
        nPlayers=2
        mode=random
        matchups=100
        listener=GeneralGameResultListener.json
        destDir=LoveLetterResults
```

## Build date
The JARs were built on 2023-07-24.