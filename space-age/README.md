# Oritech: Space Age

This subproject builds the Oritech: Space Age addon as a separate JAR. It has a
one-way project dependency on the root Oritech project.

- `gradlew runClient` launches Oritech without the addon.
- `gradlew runSpaceAgeClient` launches Oritech and Oritech: Space Age from their live source outputs.
- The matching addon tasks are `runSpaceAgeServer`, `runSpaceAgeGameTestServer`, and `runSpaceAgeData`.
- `gradlew build` builds both JARs.
- `gradlew :space-age:build` builds the addon and its required Oritech project dependency.

The addon JAR is written to `space-age/build/libs`.
