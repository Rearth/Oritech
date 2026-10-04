# Oritech: Space Age

This subproject builds the Oritech: Space Age addon as a separate JAR. It has a
one-way project dependency on the root Oritech project.

- `gradlew runClient` launches Oritech without the addon.
- `gradlew runSpaceAgeClient` launches Oritech and Oritech: Space Age from their live source outputs.
- The matching addon tasks are `runSpaceAgeServer`, `runSpaceAgeGameTestServer`, and `runSpaceAgeData`.
- `gradlew build` builds both JARs.
- `gradlew :space-age:build` builds the addon and its required Oritech project dependency.

The addon JAR is written to `space-age/build/libs`.

## Flight paths

`RocketTransferRoute` gives each navigation card one cubic Bezier curve, independent
of later cards. `FlightCurve` maps distance to curve position using a bounded arc-length
table. Endpoint tangents use inherited velocity, radial surface directions, or the
target direction. An interior polynomial bow prevents collinear reversals from folding
back on themselves without changing endpoint tangents. Stopped craft remember their previous arrival heading and receive
a deterministic 12-20 degree departure offset. Planet discs do not obstruct transfers;
only explicitly targeted surfaces have arrival/impact consequences.

The existing stage-aware full-power burn/coast/burn solver controls speed over the
curve's length. Maximum arrival omits final braking. Near planets, inverse-square
gravity is projected onto the departure and arrival curve tangents. It changes powered
acceleration and braking time, consumes the resulting extra engine time, and prevents
a stage below local gravity from departing. Gravity between those endpoint regions,
orbital motion, and curved coasting remain deliberate approximations. Atmospheric legs
conservatively use reduced ion thrust throughout. This is not an exact orbital solver.

Bounded position/velocity samples encode the timed curve for missions and previews.
`FlightMotion` evaluates cubic Hermite intervals; `RocketMapPaths` draws those same
cubics with adaptive screen-space subdivision. No client-only offsets are applied.
Live forecasts stay anchored to the transfer start. Heading is persisted across
stops, reloads and separation. Arc-length lookup and timed sampling are approximations.

`SpaceBalance` contains the 2,000-block X + Z surface mapping period, 25-degree target
spread, stopped-departure angle, reversal separation and steering cost. Launch
coordinates are captured with the assembler scan.

Run `gradlew :space-age:check` for transfer, routing, rendering and packet regressions.
In-game visual and FPS testing uses `runSpaceAgeClient`.
