package rearth.oritech.datagen;

import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.util.RegistryReflectionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GeoItemModelGenerator implements DataProvider {

    private static final List<DeferredItem<?>> GEO_ITEMS = List.of(
            ToolsContent.ENDERIC_RAILGUN,
            ToolsContent.PROMETHIUM_AXE,
            ToolsContent.PROMETHIUM_PICKAXE,
            ItemContent.SCHRODINGERS_SAFE
    );

    private final PackOutput.PathProvider pathProvider;

    public GeoItemModelGenerator(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        var futures = new ArrayList<CompletableFuture<?>>();

        RegistryReflectionUtil.IterateFields(BlockContent.class, DeferredBlock.class, (field, identifier, value) -> {
            if (!field.isAnnotationPresent(BlockContent.UseGeoBlockItem.class) || field.isAnnotationPresent(BlockContent.NoBlockItem.class)) {
                return;
            }

            var block = (DeferredBlock<?>) value;
            var itemId = block.unwrapKey().orElseThrow().identifier();
            futures.add(DataProvider.saveStable(output, createGeckoLibItemModel(itemId), pathProvider.json(itemId)));
        });

        GEO_ITEMS.forEach(item -> {
            var itemId = item.getId();
            futures.add(DataProvider.saveStable(output, createGeckoLibItemModel(itemId), pathProvider.json(itemId)));
        });

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private JsonObject createGeckoLibItemModel(Identifier itemId) {
        var specialModel = new JsonObject();
        specialModel.addProperty("type", "geckolib:geckolib");

        var model = new JsonObject();
        model.addProperty("type", "minecraft:special");
        model.addProperty("base", Identifier.fromNamespaceAndPath(itemId.getNamespace(), "item/" + itemId.getPath()).toString());
        model.add("model", specialModel);

        var root = new JsonObject();
        root.add("model", model);
        return root;
    }

    @Override
    public String getName() {
        return Oritech.MOD_ID + " geckolib item models";
    }
}
