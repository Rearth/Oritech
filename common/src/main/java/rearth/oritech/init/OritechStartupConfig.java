package rearth.oritech.init;

import dev.architectury.hooks.fluid.FluidStackHooks;
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

    public static final JetpackConfig basicJetpack = new JetpackConfig(OritechStartupConfig.STARTUP, "basicJetpack", 100_000, 4 * FluidStackHooks.bucketAmount(), 128, (int) (10 * (FluidStackHooks.bucketAmount() / 1000)), 1024, 0.4);
    public static final JetpackConfig exoJetpack = new JetpackConfig(OritechStartupConfig.STARTUP, "exoJetpack", 5_000_000, 32 * FluidStackHooks.bucketAmount(), 256, (int) (15 * (FluidStackHooks.bucketAmount() / 1000)), 10_000, 1.5);
    public static final JetpackConfig elytraJetpack = new JetpackConfig(OritechStartupConfig.STARTUP, "elytraJetpack", 100_000, 4 * FluidStackHooks.bucketAmount(), 128, (int) (10 * (FluidStackHooks.bucketAmount() / 1000)), 1024, 0.6);
    public static final JetpackConfig exoElytraJetpack = new JetpackConfig(OritechStartupConfig.STARTUP, "exoElytraJetpack", 5_000_000, 32 * FluidStackHooks.bucketAmount(), 256, (int) (15 * (FluidStackHooks.bucketAmount() / 1000)), 10_000, 1.4);
    public static final ToolConfig exoChestplate = new ToolConfig(OritechStartupConfig.STARTUP, "exoChestplate", 5_000_000, 10_000, 10_000);
    public static final ToolConfig basicDrill = new ToolConfig(OritechStartupConfig.STARTUP, "basicDrill", 10_000, 10, 512);
    public static final ToolConfig chainSaw = new ToolConfig(OritechStartupConfig.STARTUP, "chainSaw", 10_000, 10, 512);
    public static final ElectricMaceConfig electricMace = new ElectricMaceConfig(OritechStartupConfig.STARTUP);
    public static final PortableLaserConfig portableLaserConfig = new PortableLaserConfig(OritechStartupConfig.STARTUP);
    
    
    public static final ModConfigSpec.BooleanValue tightMachineAddonHitboxes = STARTUP
                                                                                 .comment("Use tighter hitboxes for machine addons")
                                                                                 .define("tightMachineAddonHitboxes", true);
    public static final ModConfigSpec.BooleanValue tightMachineFrameHitboxes = STARTUP
                                                                                 .comment("Use tighter hitboxes for machine frames")
                                                                                 .define("tightMachineFrameHitboxes", false);
    public static final ModConfigSpec.BooleanValue tightCableHitboxes = STARTUP
                                                                          .comment("Use tighter hitboxes for cables")
                                                                          .define("tightCableHitboxes", true);
    
    // finish constructing everything, this must be at end
    public static final ModConfigSpec STARTUP_SPEC = OritechStartupConfig.STARTUP.build();
    
    public static class JetpackConfig {
        public final ModConfigSpec.LongValue energyCapacity;
        public final ModConfigSpec.LongValue fuelCapacity;
        public final ModConfigSpec.IntValue energyUsage;
        public final ModConfigSpec.IntValue fuelUsage;
        public final ModConfigSpec.IntValue chargeSpeed;
        public final ModConfigSpec.DoubleValue speed;
        
        JetpackConfig(ModConfigSpec.Builder b, String name, long defCap, long defFuelCap, int defUsage, int defFuelUsage, int defCharge, double defSpeed) {
            b.push(name);
            energyCapacity = b.defineInRange("energyCapacity", defCap, 0L, Long.MAX_VALUE);
            fuelCapacity = b.defineInRange("fuelCapacity", defFuelCap, 0L, Long.MAX_VALUE);
            energyUsage = b.defineInRange("energyUsage", defUsage, 0, Integer.MAX_VALUE);
            fuelUsage = b.defineInRange("fuelUsage", defFuelUsage, 0, Integer.MAX_VALUE);
            chargeSpeed = b.defineInRange("chargeSpeed", defCharge, 0, Integer.MAX_VALUE);
            speed = b.comment("Flight speed multiplier").defineInRange("speed", defSpeed, 0.0, 100.0);
            b.pop();
        }
    }
    
    public static class ToolConfig {
        public final ModConfigSpec.LongValue energyCapacity;
        public final ModConfigSpec.LongValue energyUsage;
        public final ModConfigSpec.IntValue chargeSpeed;
        
        ToolConfig(ModConfigSpec.Builder b, String name, long defCap, long defUsage, int defCharge) {
            b.push(name);
            energyCapacity = b.defineInRange("energyCapacity", defCap, 0L, Long.MAX_VALUE);
            energyUsage = b.defineInRange("energyUsage", defUsage, 0L, Long.MAX_VALUE);
            chargeSpeed = b.defineInRange("chargeSpeed", defCharge, 0, Integer.MAX_VALUE);
            b.pop();
        }
    }
    
    public static class ElectricMaceConfig {
        public final ModConfigSpec.LongValue energyCapacity;
        public final ModConfigSpec.IntValue energyUsage;
        public final ModConfigSpec.IntValue chargeSpeed;
        public final ModConfigSpec.IntValue lightningCostMultiplier;
        
        ElectricMaceConfig(ModConfigSpec.Builder b) {
            b.push("electricMace");
            energyCapacity = b.defineInRange("energyCapacity", 500_000L, 0L, Long.MAX_VALUE);
            energyUsage = b.comment("RF consumed per hit").worldRestart().defineInRange("energyUsage", 2048, 0, Integer.MAX_VALUE);
            chargeSpeed = b.defineInRange("chargeSpeed", 50_000, 0, Integer.MAX_VALUE);
            lightningCostMultiplier = b.comment("Lightning attack RF usage multiplier").defineInRange("lightningCostMultiplier", 8, 0, Integer.MAX_VALUE);
            b.pop();
        }
    }
    
    public static class PortableLaserConfig {
        public final ModConfigSpec.LongValue energyCapacity;
        public final ModConfigSpec.IntValue energyPerTick;
        public final ModConfigSpec.IntValue energyPerBoom;
        public final ModConfigSpec.DoubleValue blockBreakSpeed;
        public final ModConfigSpec.IntValue damageBase;
        public final ModConfigSpec.IntValue explosionStrength;
        
        PortableLaserConfig(ModConfigSpec.Builder b) {
            b.push("portableLaserConfig");
            energyCapacity = b.defineInRange("energyCapacity", 5_000_000L, 0L, Long.MAX_VALUE);
            energyPerTick = b.comment("RF consumed per tick while firing").defineInRange("energyPerTick", 4096, 0, Integer.MAX_VALUE);
            energyPerBoom = b.comment("RF consumed per explosion").defineInRange("energyPerBoom", 100_000, 0, Integer.MAX_VALUE);
            blockBreakSpeed = b.comment("Block breaking speed multiplier").defineInRange("blockBreakSpeed", 0.125, 0.0, 100.0);
            damageBase = b.comment("Base damage to entities").defineInRange("damageBase", 4, 0, Integer.MAX_VALUE);
            explosionStrength = b.comment("Explosion power").defineInRange("explosionStrength", 6, 0, Integer.MAX_VALUE);
            b.pop();
        }
    }
}
