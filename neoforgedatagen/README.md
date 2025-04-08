## NeoForge datagen

**WARNING** This datagen project should not add any files (tags, etc.) that conflict with the _fabricdatagen_ build. If any compatibility improvements can be done with tags, it should be done in the _fabricdatagen_ build.

## Running NeoForge datagen

The main Oritech build must be run at least once before generating Neoforge data, as the datagen uses the Oritech .jar file.

From the _neoforgedatagen_ folder, run `../gradlew runData` to generate new datafiles under the _neoforge/src/generated_ folder.

The resulting .json files can be added to git.
