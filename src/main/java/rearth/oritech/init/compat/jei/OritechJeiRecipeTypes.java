package rearth.oritech.init.compat.jei;

import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import rearth.oritech.Oritech;
import rearth.oritech.init.recipes.OritechRecipe;

final class OritechJeiRecipeTypes {

    static final IRecipeHolderType<OritechRecipe> PULVERIZER = create("pulverizer");
    static final IRecipeHolderType<OritechRecipe> GRINDER = create("grinder");
    static final IRecipeHolderType<OritechRecipe> ASSEMBLER = create("assembler");
    static final IRecipeHolderType<OritechRecipe> REFINERY = create("refinery");
    static final IRecipeHolderType<OritechRecipe> FOUNDRY = create("foundry");
    static final IRecipeHolderType<OritechRecipe> CENTRIFUGE = create("centrifuge");
    static final IRecipeHolderType<OritechRecipe> CENTRIFUGE_FLUID = create("centrifuge_fluid");
    static final IRecipeHolderType<OritechRecipe> ATOMIC_FORGE = create("atomic_forge");
    static final IRecipeHolderType<OritechRecipe> BIO_GENERATOR = create("bio_generator");
    static final IRecipeHolderType<OritechRecipe> FUEL_GENERATOR = create("fuel_generator");
    static final IRecipeHolderType<OritechRecipe> LAVA_GENERATOR = create("lava_generator");
    static final IRecipeHolderType<OritechRecipe> STEAM_ENGINE = create("steam_engine");
    static final IRecipeHolderType<OritechRecipe> PARTICLE_COLLISION = create("particle_collision");
    static final IRecipeHolderType<OritechRecipe> INDUSTRIAL_CHILLER = create("industrial_chiller");
    static final IRecipeHolderType<OritechRecipe> REACTOR = create("reactor");
    static final IRecipeHolderType<OritechRecipe> LASER = create("laser");
    static final IRecipeType<OritechJeiTaintedRefineryCreation.CreationInfo> TAINTED_REFINERY_CREATION =
            IRecipeType.create(Oritech.id("tainted_refinery_creation"),
                    OritechJeiTaintedRefineryCreation.CreationInfo.class);
    static final IRecipeType<OritechJeiTaintedRefineryBonuses.BonusInfo> TAINTED_REFINERY_BONUSES =
            IRecipeType.create(Oritech.id("tainted_refinery_bonuses"),
                    OritechJeiTaintedRefineryBonuses.BonusInfo.class);

    private OritechJeiRecipeTypes() {
    }

    private static IRecipeHolderType<OritechRecipe> create(String path) {
        return IRecipeHolderType.create(Oritech.id(path));
    }
}
