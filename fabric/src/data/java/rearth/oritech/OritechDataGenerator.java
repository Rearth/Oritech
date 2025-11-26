package rearth.oritech;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.generator.*;

public class OritechDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		var pack = fabricDataGenerator.createPack();
		
        switch (System.getProperty("oritech.datagen.loader")) {
            case "fabric":
                pack.addProvider(RecipeGenerator::new);
                break;
            case "common":
                pack.addProvider(ModelGenerator::new);
                pack.addProvider(BlockLootGenerator::new);
                pack.addProvider(BlockTagGenerator::new);
                pack.addProvider(ItemTagGenerator::new);
                pack.addProvider(FluidTagGenerator::new);
                pack.addProvider(AdvancementGenerator::new);
                pack.addProvider(EntityTagGenerator::new);
                break;
            default:
                throw new IllegalStateException("Unknown oritech.datagen.loader value");
        }
    }
	
	@Override
	public @Nullable String getEffectiveModId() {
		return Oritech.MOD_ID;
	}
}
