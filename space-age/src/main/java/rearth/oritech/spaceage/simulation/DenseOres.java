package rearth.oritech.spaceage.simulation;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.simulation.SpaceObjects.AsteroidMaterial;

import java.util.List;

/**
 * Mining yield is loot, never extra physical volume or tug mass. Mass is measured in tonnes.
 */
public final class DenseOres {

    private DenseOres() {
    }

    public static double density(Identifier block) {

        var family = family(block);
        return family == null ? 1 : family.mass;
    }

    public static float mass(List<AsteroidMaterial> materials) {

        return (float) materials.stream().mapToDouble(material -> material.amount() * density(material.block())).sum();
    }

    public static Family family(Identifier block) {

        for (var family : Family.values()) {
            if (family.id().equals(block)) return family;
        }
        return null;
    }

    /**
     * Host rock variants share one dense family. Special non-ore blocks pass through unchanged.
     */
    public static Identifier denseVariant(Identifier block) {

        var path = block.getPath();
        for (var family : Family.values()) {
            if (path.equals(family.key + "_ore") || path.equals("deepslate_" + family.key + "_ore")
                    || path.equals("nether_" + family.key + "_ore") || path.equals("endstone_" + family.key + "_ore")
                    || path.equals("ancient_debris") && family == Family.ANCIENT_DEBRIS) {
                var namespace = family.drop.getNamespace();
                if (block.getNamespace().equals(namespace)) return family.id();
            }
        }
        return block;
    }

    public enum Family {
        COAL("coal", "minecraft:coal", 2, false),
        IRON("iron", "minecraft:raw_iron", 4, false),
        COPPER("copper", "minecraft:raw_copper", 4, false),
        GOLD("gold", "minecraft:raw_gold", 6, false),
        REDSTONE("redstone", "minecraft:redstone", 3, false),
        LAPIS("lapis", "minecraft:lapis_lazuli", 3, false),
        DIAMOND("diamond", "minecraft:diamond", 4, true),
        EMERALD("emerald", "minecraft:emerald", 4, true),
        QUARTZ("quartz", "minecraft:quartz", 3, false),
        ANCIENT_DEBRIS("ancient_debris", "minecraft:ancient_debris", 8, true),
        NICKEL("nickel", "oritech:raw_nickel", 4, false),
        PLATINUM("platinum", "oritech:raw_platinum", 6, true),
        URANIUM("uranium", "oritech:raw_uranium", 6, true);

        public final String key;
        public final Identifier drop;
        public final double mass;
        public final boolean diamondTool;

        Family(String key, String drop, double mass, boolean diamondTool) {

            this.key = key;
            this.drop = Identifier.parse(drop);
            this.mass = mass;
            this.diamondTool = diamondTool;
        }

        public Identifier id() {

            return OritechSpaceAge.id("superdense_" + key + "_ore");
        }

        public Block block() {

            return BuiltInRegistries.BLOCK.getValue(id());
        }

        public Item output() {

            return BuiltInRegistries.ITEM.getValue(drop);
        }
    }
}
