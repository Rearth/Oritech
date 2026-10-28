package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.simulation.SpaceObjects.AsteroidMaterial;
import rearth.oritech.spaceage.simulation.SpaceObjects.DetectionState;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/** Fixed, inexhaustible deposits loaded from the world's datapack registry. */
public final class ResourceBodies {

    public static final ResourceKey<Registry<Definition>> REGISTRY = ResourceKey.createRegistryKey(OritechSpaceAge.id("resource_bodies"));

    private ResourceBodies() {
    }

    public static void register(DataPackRegistryEvent.NewRegistry event) {

        event.dataPackRegistry(REGISTRY, Definition.CODEC, Definition.CODEC);
    }

    public static UUID id(Identifier key) {

        return UUID.nameUUIDFromBytes(("resource_body:" + key).getBytes(StandardCharsets.UTF_8));
    }

    public static List<SpaceObjectData> objects(RegistryAccess registries) {

        return registries.lookupOrThrow(REGISTRY).listElements()
                .map(holder -> holder.value().object(holder.key().identifier())).toList();
    }

    public static Definition find(RegistryAccess registries, UUID objectId) {

        return registries.lookupOrThrow(REGISTRY).listElements()
                .filter(holder -> id(holder.key().identifier()).equals(objectId))
                .map(holder -> holder.value()).findFirst().orElse(null);
    }

    public record Definition(String name, String description, float x, float y, float radius,
                             Identifier modelBlock, List<AsteroidMaterial> resources) {

        public static final Codec<Definition> CODEC = RecordCodecBuilder.<Definition>create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Definition::name),
                Codec.STRING.fieldOf("description").forGetter(Definition::description),
                Codec.FLOAT.fieldOf("x").forGetter(Definition::x),
                Codec.FLOAT.fieldOf("y").forGetter(Definition::y),
                Codec.FLOAT.fieldOf("radius").forGetter(Definition::radius),
                Identifier.CODEC.fieldOf("model_block").forGetter(Definition::modelBlock),
                AsteroidMaterial.CODEC.listOf().fieldOf("resources").forGetter(Definition::resources)
        ).apply(i, Definition::new)).validate(definition -> definition.radius > 0 && !definition.resources.isEmpty()
                && definition.resources.stream().allMatch(material -> material.amount() > 0)
                ? DataResult.success(definition) : DataResult.error(() -> "Resource bodies need a positive radius and positive resource weights"));

        public Definition {

            resources = List.copyOf(resources);
        }

        public SpaceObjectData object(Identifier key) {

            return new SpaceObjectData(id(key), ObjectType.RESOURCE_BODY, x, y, 0, 0, radius, 0, 0,
                    DetectionState.PRECISE, name, resources);
        }
    }
}
