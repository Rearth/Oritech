package rearth.oritech.init;

import dev.architectury.hooks.fluid.FluidStackHooks;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.*;

// thank gpt for being able to translate the language keys for all this. It would be horror to type these all out
@io.wispforest.owo.config.annotation.Config(name = "oritech-config", wrapperName = "OritechConfig")
@Modmenu(modId = "oritech")
@Sync(Option.SyncMode.OVERRIDE_CLIENT)
public class Config {
    
    @SectionHeader("machineSettings")
    @Nest
    public ProcessingMachines processingMachines = new ProcessingMachines();
    @Nest
    public Generators generators = new Generators();
    @Nest
    public LaserArmConfig laserArmConfig = new LaserArmConfig();
    @Nest
    public DeepDrillConfig deepDrillConfig = new DeepDrillConfig();
    @Nest
    public MachineFrameData destroyerConfig = new MachineFrameData(15, 40, 8, 128);
    @Nest
    public FertilizerConfig fertilizerConfig = new FertilizerConfig();
    @Nest
    public MachineFrameData placerConfig = new MachineFrameData(10, 5, 8, 64);
    @Nest
    public AddonConfig addonConfig = new AddonConfig();
    public boolean additiveAddons = true;
    public boolean layeredExtenders = false;
    public float blockBreakHardnessExponentialFactor = 0.5f;
    
    @SectionHeader("storageBlocks")
    @Nest
    public BasicEnergyMachineData smallEnergyStorage = new BasicEnergyMachineData(1_000_000, 5000, 5000, 0);
    @Nest
    public BasicEnergyMachineData largeEnergyStorage = new BasicEnergyMachineData(20_000_000, 10_000, 10_000, 0);
    public int portableTankCapacityBuckets = 256;
    public int overchargedCrystalChargeRate = 10;
    
    @SectionHeader("logistics")
    public int itemPipeTransferAmount = 8;
    public int itemPipeIntervalDuration = 5;
    public float fluidPipeExtractAmountBuckets = 0.5f;
    public int fluidPipeExtractIntervalDuration = 3;
    public float fluidPipeInternalStorageBuckets = 2f;
    public long energyPipeTransferRate = 10_000;
    public long superConductorTransferRate = 4_194_304;
    @Nest
    public PowerPoleConfig poleConfig = new PowerPoleConfig();
    
    @SectionHeader("equipment")
    @Nest
    public BasicEnergyMachineData charger = new BasicEnergyMachineData(500_000, 10_000, 5_000, 0);
    @Nest
    public JetpackData basicJetpack = new JetpackData(100_000, 4 * FluidStackHooks.bucketAmount(), 128, (int) (10 * (FluidStackHooks.bucketAmount() / 1000)), 1024, 0.4f);
    @Nest
    public JetpackData exoJetpack = new JetpackData(5_000_000, 32 * FluidStackHooks.bucketAmount(), 256, (int) (15 * (FluidStackHooks.bucketAmount() / 1000)), 10_000, 1.5f);
    @Nest
    public JetpackData elytraJetpack = new JetpackData(100_000, 4 * FluidStackHooks.bucketAmount(), 128, (int) (10 * (FluidStackHooks.bucketAmount() / 1000)), 1024, 0.6f);
    @Nest
    public JetpackData exoElytraJetpack = new JetpackData(5_000_000, 32 * FluidStackHooks.bucketAmount(), 256, (int) (15 * (FluidStackHooks.bucketAmount() / 1000)), 10_000, 1.4f);
    @Nest
    public ToolData exoChestplate = new ToolData(5_000_000, 10_000, 10_000);
    @Nest
    public ToolData basicDrill = new ToolData(10_000, 10, 512);
    @Nest
    public ToolData chainSaw = new ToolData(10_000, 10, 512);
    @Nest
    public ElectricMaceData electricMace = new ElectricMaceData();
    @Nest
    public PortableLaserConfig portableLaserConfig = new PortableLaserConfig();
    public boolean chainsawTreeCutting = true;
    
    @SectionHeader("worldGeneration")
    public boolean generateOresFabricOnly = true;
    public boolean easyFindFeatures = true;
    
    @SectionHeader("reactor")
    public boolean safeMode = false;
    public int safeModeCooldown = 2400;
    public int maxSize = 64;
    public int reactorMaxEnergyStored = 50_000_000;
    public int reactorMaxEnergyOutput = 25_000;
    public int rfPerPulse = 64;
    public int absorberRate = 16;
    public int ventBaseRate = 4;
    public int ventRelativeRate = 100;
    public int maxHeat = 2000;
    public int maxUnstableTicks = 600;
    public boolean boringNukes = false;
    
    @SectionHeader("arcane")
    public int enchanterCostMultiplier = 5;
    public int catalystBaseSouls = 50;
    public int catalystRFPerSoul = 20;
    public int catalystCostMultiplier = 2;
    public int catalystHyperMultiplier = 2;
    public float catalystHyperExpFactor = 1.15f;
    public int spawnerCostMultiplier = 1;
    
    @SectionHeader("particleAccelerator")
    public int maxGateDist = 10;
    public float bendFactor = 2.5f;
    public int accelerationRFCost = 10;
    public long acceleratorMotorRFCapacity = 5_000_000L;
    public int endPortalRequiredSpeed = 10000;
    public int netherPortalRequiredSpeed = 5000;
    public int blackHoleRequiredSpeed = 15000;
    public int collectorEnergyStorage = 1_000_000;
    public float tachyonCollisionEnergyFactor = 1f;

    @SectionHeader("blackHole")
    public int pullTimeMultiplier = 8;
    public int pullRange = 16;
    public int idleWaitTicks = 200;
    public int blackHoleTachyonEnergy = 50_000;
    public long unstableContainerBaseCapacity = 20_000_000;
    
    @SectionHeader("augments")
    public long augmenterMaxEnergy = 500_000_000L;
    
    @SectionHeader("clientSettings")
    @Sync(Option.SyncMode.NONE)
    @RestartRequired
    public boolean tightMachineAddonHitboxes = true;
    @Sync(Option.SyncMode.NONE)
    @RestartRequired
    public boolean tightMachineFrameHitboxes = false;
    @Sync(Option.SyncMode.NONE)
    @RestartRequired
    public boolean tightCableHitboxes = true;
    @Sync(Option.SyncMode.NONE)
    public float machineVolumeMultiplier = 1f;
    @Sync(Option.SyncMode.NONE)
    public boolean showMachinePreview = true;
    @Sync(Option.SyncMode.NONE)
    public boolean enableHelpButton = true;
    
    public static class ProcessingMachines {
        
        public int machineFrameMaxLength = 64;
        
        @Nest
        public BasicEnergyMachineData assemblerData = new BasicEnergyMachineData(50000, 128 * 8, 0, 128);
        @Nest
        public BasicEnergyMachineData atomicForgeData = new BasicEnergyMachineData(1024, 0, 0, 1024);
        @Nest
        public CentrifugeConfig centrifugeData = new CentrifugeConfig();
        @Nest
        public BasicEnergyMachineData foundryData = new BasicEnergyMachineData(50000, 128 * 8, 0, 128);
        @Nest
        public BasicEnergyMachineData coolerData = new BasicEnergyMachineData(50000, 32 * 8, 0, 32);
        @Nest
        public BasicEnergyMachineData fragmentForgeData = new BasicEnergyMachineData(50000, 256 * 8, 0, 256);
        @Nest
        public FurnaceConfig furnaceData = new FurnaceConfig();
        @Nest
        public BasicEnergyMachineData pulverizerData = new BasicEnergyMachineData(25000, 32 * 8, 0, 32);
        @Nest
        public BasicEnergyMachineData refineryData = new BasicEnergyMachineData(50000, 64 * 8, 0, 64);
    }
    
    public static class Generators {
        
        public float animationSpeedMultiplier = 10;
        public String steamId = "oritech:still_steam";
        
        @Nest
        public BasicEnergyMachineData basicGeneratorData = new BasicEnergyMachineData(50_000, 0, 32 * 8, 32);
        @Nest
        public BasicEnergyMachineData bioGeneratorData = new BasicEnergyMachineData(100_000, 0, 64 * 8, 64);
        @Nest
        public BasicEnergyMachineData lavaGeneratorData = new BasicEnergyMachineData(100_000, 0, 64 * 8, 64);
        @Nest
        public BasicEnergyMachineData fuelGeneratorData = new BasicEnergyMachineData(250_000, 0, 256 * 8, 256);
        @Nest
        public SteamEngineData steamEngineData = new SteamEngineData(100_000, 50_000, 2, 1, false, true, 8);
        @Nest
        public BasicEnergyMachineData solarGeneratorData = new BasicEnergyMachineData(100_000, 0, 32 * 8, 32);
    }
    
    public static class LaserArmConfig {
        public long energyCapacity = 20000;
        public long maxEnergyInsertion = 128 * 8;
        public long energyPerTick = 128;
        public int blockBreakEnergyBase = 1024; // multiplied by block hardness
        public float damageTickBase = 2;
        public int range = 128;
    }
    
    public static class PortableLaserConfig {
        public long energyCapacity = 5_000_000;
        public int energyPerTick = 4096;
        public int energyPerBoom = 100_000;
        public float blockBreakSpeed = 0.125f; // multiplied by block hardness
        public int damageBase = 4;
        public int explosionStrength = 6;
    }
    
    public static class ElectricMaceData {
        public long energyCapacity = 500_000;
        public int energyUsage = 2048;
        public int chargeSpeed = 50_000;
        public int baseDamage = 8;
        public int lightningCostMultiplier = 8;
    }
    
    public static class PowerPoleConfig {
        public long energyCapacity = 1_000_000;
        public int minRange = 50;
        public int maxRange = 1000;
    }
    
    public static class DeepDrillConfig {
        public long energyCapacity = 20000;
        public int stepsPerOre = 20;
        public int energyPerStep = 1024;
    }
    
    public static class AddonConfig {
        public float speedAddonSpeed = 0.5f;
        public float speedAddonEfficiency = 1.2f;
        public float efficiencyAddonEfficiency = 0.8f;
        public float ultimateAddonSpeed = 0.25f;
        public float ultimateAddonEfficiency = 1.1f;
        public float chamberAddonEfficiency = 1.5f;
        public float burstAddonSpeedMultiplier = 8f;
        public float burstAddonThrottleMultiplier = 1.2f;
        public int burstAddonTicks = 240;
        public long addonShrinkerRF = 50_000_000;
    }
    
    public static class CentrifugeConfig {
        public long energyCapacity = 10000;
        public long maxEnergyInsertion = 64 * 8;
        public int energyPerTick = 64;
        public long tankSizeInBuckets = 8;
    }
    
    public static class FurnaceConfig {
        public long energyCapacity = 10000;
        public long maxEnergyInsertion = 32 * 8;
        public int energyPerTick = 32;
        public float speedMultiplier = 0.5f;
    }
    
    public static class FertilizerConfig {
        public int moveDuration = 10;
        public int workDuration = 20;
        public int moveEnergyUsage = 8;
        public int workEnergyUsage = 128;
        public float liquidPerBlockUsage = 0.25f;
    }
    
    public static class BasicEnergyMachineData {
        public long energyCapacity;
        public long maxEnergyInsertion;
        public long maxEnergyExtraction;
        public int energyPerTick;  // usage rate for most machines, production rate for generators
        
        public BasicEnergyMachineData(long energyCapacity, long maxEnergyInsertion, long maxEnergyExtraction, int energyPerTick) {
            this.energyCapacity = energyCapacity;
            this.maxEnergyInsertion = maxEnergyInsertion;
            this.maxEnergyExtraction = maxEnergyExtraction;
            this.energyPerTick = energyPerTick;
        }
    }
    
    public static class SteamEngineData {
        public long energyCapacity;
        public long maxEnergyExtraction;
        public float rfToSteamRatio;    // used for generators
        public int steamToRfRatio;  // used for steam engines
        public boolean stopOnEnergyFull;
        public boolean stopOnWaterFull;
        public float steamBoilerCapacityBuckets;
        
        public SteamEngineData(long energyCapacity, long maxEnergyExtraction, float rfToSteamRatio, int steamToRfRatio, boolean stopOnEnergyFull, boolean stopOnWaterFull, float steamBoilerCapacityBuckets) {
            this.energyCapacity = energyCapacity;
            this.maxEnergyExtraction = maxEnergyExtraction;
            this.steamToRfRatio = steamToRfRatio;
            this.rfToSteamRatio = rfToSteamRatio;
            this.stopOnEnergyFull = stopOnEnergyFull;
            this.stopOnWaterFull = stopOnWaterFull;
            this.steamBoilerCapacityBuckets = steamBoilerCapacityBuckets;
        }
    }
    
    public static class JetpackData {
        public long energyCapacity;
        public long fuelCapacity;
        public int energyUsage;
        public int fuelUsage;
        public int chargeSpeed;
        public float speed;
        
        public JetpackData(long energyCapacity, long fuelCapacity, int energyUsage, int fuelUsage, int chargeSpeed, float speed) {
            this.energyCapacity = energyCapacity;
            this.fuelCapacity = fuelCapacity;
            this.energyUsage = energyUsage;
            this.fuelUsage = fuelUsage;
            this.chargeSpeed = chargeSpeed;
            this.speed = speed;
        }
    }
    
    public static class ToolData {
        public long energyCapacity;
        public long energyUsage;
        public int chargeSpeed;
        
        public ToolData(long energyCapacity, long energyUsage, int chargeSpeed) {
            this.energyCapacity = energyCapacity;
            this.energyUsage = energyUsage;
            this.chargeSpeed = chargeSpeed;
        }
    }
    
    public static class MachineFrameData {
        public int moveDuration;
        public int workDuration;
        public int moveEnergyUsage;
        public int workEnergyUsage;
        
        public MachineFrameData(int moveDuration, int workDuration, int moveEnergyUsage, int workEnergyUsage) {
            this.moveDuration = moveDuration;
            this.workDuration = workDuration;
            this.moveEnergyUsage = moveEnergyUsage;
            this.workEnergyUsage = workEnergyUsage;
        }
    }
    
}
