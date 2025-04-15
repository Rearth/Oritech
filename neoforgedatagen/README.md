## NeoForge datagen

**WARNING** This datagen project should not add any files (tags, etc.) that conflict with the _fabricdatagen_ build. If any compatibility improvements can be done with tags, it should be done in the _fabricdatagen_ build.

## Running NeoForge datagen

The main Oritech build must be run at least once before generating Neoforge data, as the datagen uses the Oritech .jar file.

From the main Oritech folder, run `./gradlew runDatagenNeoforge` to generate new datafiles under the _neoforge/src/main/generated_ folder.

You can also run `../gradlew runData` or `../gradlew clean` from the _neoforgedatagen_ folder. It will do the same thing.

The resulting .json files can be added to git.
