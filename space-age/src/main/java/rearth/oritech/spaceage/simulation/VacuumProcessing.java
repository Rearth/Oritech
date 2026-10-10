package rearth.oritech.spaceage.simulation;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.spaceage.recipe.VacuumRecipe;

import java.util.Collection;

/**
 * Pure matching and explicit processing: previews work on owned copies, never placed machines.
 */
public final class VacuumProcessing {

    private static final Identifier NO_RECIPE = Identifier.parse("minecraft:empty");

    private VacuumProcessing() {
    }

    public static Match match(BlockState first, BlockState second, Collection<RecipeHolder<VacuumRecipe>> recipes) {

        var matches = recipes.stream().filter(r -> r.value().matches(first, second)).toList();
        // Do not pick a recipe by registry order when datapacks define conflicting pairs.
        if (matches.isEmpty()) return new Match(NO_RECIPE, null, "no_valid_input_pair");
        if (matches.size() > 1) return new Match(NO_RECIPE, null, "ambiguous_recipe");
        var holder = matches.getFirst();
        return new Match(holder.id().identifier(), holder.value(), "");
    }

    public static boolean zeroG(double x, double y, Collection<SpaceSimulation.SpaceObjectData> objects) {

        return objects.stream().filter(o -> RocketTransferRoute.hasSurface(o) || o.type() == SpaceObjects.ObjectType.ASTEROID)
                .noneMatch(o -> Math.hypot(x - o.x(), y - o.y()) < exclusionRadius(o));
    }

    public static boolean zeroGForDuration(double x, double y, double vx, double vy, double ticks, Collection<SpaceSimulation.SpaceObjectData> objects) {

        var dx = vx * ticks / 20;
        var dy = vy * ticks / 20;
        var lengthSquared = dx * dx + dy * dy;
        for (var object : objects) {
            if (!RocketTransferRoute.hasSurface(object) && object.type() != SpaceObjects.ObjectType.ASTEROID) continue;

            // Check the closest point of the whole coast, so passing through gravity also pauses crafting.
            var t = lengthSquared == 0 ? 0 : Math.clamp(((object.x() - x) * dx + (object.y() - y) * dy) / lengthSquared, 0, 1);
            if (Math.hypot(x + dx * t - object.x(), y + dy * t - object.y()) < exclusionRadius(object))
                return false;
        }
        return true;
    }

    private static double exclusionRadius(SpaceSimulation.SpaceObjectData object) {

        // Low orbit sits exactly on this boundary; rounding a projected position must not disable crafting.
        return object.radius() + (object.id().equals(SpaceObjects.EARTH_ID) ? 10_000 : 1_000) - .01;
    }

    public record Match(Identifier id, VacuumRecipe recipe, String issue) {

        public boolean valid() {

            return recipe != null && issue.isEmpty();
        }
    }
}
