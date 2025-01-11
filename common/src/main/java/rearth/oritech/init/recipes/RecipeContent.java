package rearth.oritech.init.recipes;

import rearth.oritech.Oritech;
import rearth.oritech.util.ArchitecturyRecipeRegistryContainer;

public class RecipeContent implements ArchitecturyRecipeRegistryContainer {

    public static final OritechRecipeType PULVERIZER = new OritechRecipeType(Oritech.id("pulverizer"));
    public static final OritechRecipeType GRINDER = new OritechRecipeType(Oritech.id("grinder"));
    public static final OritechRecipeType ASSEMBLER = new OritechRecipeType(Oritech.id("assembler"));
    public static final OritechRecipeType FOUNDRY = new OritechRecipeType(Oritech.id("foundry"));
    public static final OritechRecipeType CENTRIFUGE = new OritechRecipeType(Oritech.id("centrifuge"));
    public static final OritechRecipeType CENTRIFUGE_FLUID = new OritechRecipeType(Oritech.id("centrifuge_fluid"));
    public static final OritechRecipeType ATOMIC_FORGE = new OritechRecipeType(Oritech.id("atomic_forge"));
    public static final OritechRecipeType BIO_GENERATOR = new OritechRecipeType(Oritech.id("bio_generator"));
    public static final OritechRecipeType FUEL_GENERATOR = new OritechRecipeType(Oritech.id("fuel_generator"));
    public static final OritechRecipeType LAVA_GENERATOR = new OritechRecipeType(Oritech.id("lava_generator"));
    public static final OritechRecipeType STEAM_ENGINE = new OritechRecipeType(Oritech.id("steam_engine"));
    public static final OritechRecipeType DEEP_DRILL = new OritechRecipeType(Oritech.id("deep_drill"));
    public static final OritechRecipeType PARTICLE_COLLISION = new OritechRecipeType(Oritech.id("particle_collision"));
    public static final OritechRecipeType COOLER = new OritechRecipeType(Oritech.id("cooler"));
    public static final OritechRecipeType REACTOR = new OritechRecipeType(Oritech.id("reactor"));
    public static final OritechRecipeType LASER = new OritechRecipeType(Oritech.id("laser"));
    
    public static final AugmentRecipeType AUGMENT = new AugmentRecipeType(Oritech.id("augment"));
    
}
