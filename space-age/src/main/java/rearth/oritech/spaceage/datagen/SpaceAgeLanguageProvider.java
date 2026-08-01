package rearth.oritech.spaceage.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

public class SpaceAgeLanguageProvider extends LanguageProvider {

    public SpaceAgeLanguageProvider(PackOutput output) {
        super(output, OritechSpaceAge.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemgroup.oritech_space_age.main", "Oritech: Space Age");
        add(SpaceAgeBlocks.ROCKET_ASSEMBLER.get(), "Rocket Assembler");
        add(SpaceAgeBlocks.ROCKET_PAD.get(), "Rocket Pad");
        add(SpaceAgeBlocks.ROCKET_ENGINE_TIER_1.get(), "Rocket Engine Mk I");
        add(SpaceAgeBlocks.ROCKET_ENGINE_TIER_2.get(), "Rocket Engine Mk II");
        add(SpaceAgeBlocks.ROCKET_ENGINE_TIER_3.get(), "Rocket Engine Mk III");
    }
}
