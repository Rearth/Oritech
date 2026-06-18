package rearth.oritech.init.datapack;

import com.mojang.serialization.JsonOps;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import rearth.oritech.datagen.OritechRecipeGenerator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;

// todo move this to datagen
public class OritechAugmentDataGenerator implements DataProvider {

    private final PackOutput.PathProvider pathProvider;

    public OritechAugmentDataGenerator(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "augments");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        var augmentData = new LinkedHashMap<Identifier, AugmentData>();
        OritechRecipeGenerator.addAugmentData((id, data) -> {
            var previous = augmentData.put(id, data);
            if (previous != null) {
                throw new IllegalStateException("Duplicate augment definition for id " + id);
            }
        });

        var tasks = new ArrayList<CompletableFuture<?>>();
        augmentData.forEach((id, data) -> {
            var encoded = AugmentData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow(IllegalStateException::new);
            tasks.add(DataProvider.saveStable(output, encoded, pathProvider.json(id)));
        });

        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Oritech augment data";
    }
}

