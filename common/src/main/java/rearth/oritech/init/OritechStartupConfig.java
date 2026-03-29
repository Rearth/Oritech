package rearth.oritech.init;

import net.neoforged.neoforge.common.ModConfigSpec;

public class OritechStartupConfig {
    // Client config
    public static final ModConfigSpec.Builder STARTUP = new ModConfigSpec.Builder();
    
    
    public static final ModConfigSpec.BooleanValue generateOresFabricOnly = STARTUP
                                                                              .comment("[Fabric Only] Whether ores are generated. On NeoForge, use a datapack instead.")
                                                                              .define("generateOresFabricOnly", true);
    
    public static final ModConfigSpec.DoubleValue speedAddonSpeed = STARTUP
        .comment("Speed addon processing speed multiplier")
        .defineInRange("speedAddonSpeed", 0.5, 0.0, 100.0);
    
    public static final ModConfigSpec.DoubleValue speedAddonEfficiency = STARTUP
        .comment("Speed addon energy efficiency multiplier")
        .defineInRange("speedAddonEfficiency", 1.2, 0.0, 100.0);
    
    public static final ModConfigSpec.DoubleValue efficiencyAddonEfficiency = STARTUP
        .comment("Efficiency addon energy multiplier")
        .defineInRange("efficiencyAddonEfficiency", 0.8, 0.0, 100.0);
    
    public static final ModConfigSpec.DoubleValue ultimateAddonSpeed = STARTUP
        .comment("Ultimate addon speed multiplier")
        .defineInRange("ultimateAddonSpeed", 0.25, 0.0, 100.0);
    
    public static final ModConfigSpec.DoubleValue ultimateAddonEfficiency = STARTUP
        .comment("Ultimate addon efficiency multiplier")
        .defineInRange("ultimateAddonEfficiency", 1.1, 0.0, 100.0);
    
    public static final ModConfigSpec.DoubleValue chamberAddonEfficiency = STARTUP
        .comment("Processing chamber efficiency multiplier")
        .defineInRange("chamberAddonEfficiency", 1.5, 0.0, 100.0);
    
    public static final ModConfigSpec.IntValue burstAddonTicks = STARTUP
        .comment("Burst time in ticks per addon")
        .defineInRange("burstAddonTicks", 240, 0, Integer.MAX_VALUE);
    
    // Solar panel energy per tick - must be startup because it is baked into the block at registration time
    public static final ModConfigSpec.IntValue solarEnergyPerTick = STARTUP
        .comment("Big solar panel base energy production per tick in RF")
        .defineInRange("solarEnergyPerTick", 32, 0, Integer.MAX_VALUE);
    
    public static final ModConfigSpec STARTUP_SPEC = OritechStartupConfig.STARTUP.build();
}
