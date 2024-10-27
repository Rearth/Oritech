package rearth.oritech.init;

import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;

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
    
    @SectionHeader("storageBlocks")
    @Nest
    public BasicEnergyMachineData smallEnergyStorage = new BasicEnergyMachineData(1_000_000, 1000, 1000, 0);
    @Nest
    public BasicEnergyMachineData largeEnergyStorage = new BasicEnergyMachineData(20_000_000, 5000, 5000, 0);
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
    
    @SectionHeader("equipment")
    @Nest
    public BasicEnergyMachineData charger = new BasicEnergyMachineData(500_000, 10_000, 5000, 0);
    @Nest
    public JetpackData basicJetpack = new JetpackData(100_000, 4 * FluidConstants.BUCKET, 128, (int) (10 * (FluidConstants.BUCKET / 1000)), 1024, 0.4f);
    @Nest
    public JetpackData exoJetpack = new JetpackData(5_000_000, 32 * FluidConstants.BUCKET, 256, (int) (15 * (FluidConstants.BUCKET / 1000)), 10_000, 1.5f);
    @Nest
    public JetpackData elytraJetpack = new JetpackData(100_000, 4 * FluidConstants.BUCKET, 128, (int) (10 * (FluidConstants.BUCKET / 1000)), 1024, 0.6f);
    @Nest
    public JetpackData exoElytraJetpack = new JetpackData(5_000_000, 32 * FluidConstants.BUCKET, 256, (int) (15 * (FluidConstants.BUCKET / 1000)), 10_000, 1.4f);
    @Nest
    public ToolData exoChestplate = new ToolData(5_000_000, 10_000, 10_000);
    @Nest
    public ToolData basicDrill = new ToolData(10_000, 10, 512);
    @Nest
    public ToolData chainSaw = new ToolData(10_000, 10, 512);
    
    @SectionHeader("worldGeneration")
    public boolean generateOres = true;
    public boolean easyFindFeatures = true;
    
    @SectionHeader("arcane")
    public int enchanterCostMultiplier = 5;
    public int catalystBaseSouls = 50;
    public int catalystRFPerSoul = 20;
    public int catalystCostMultiplier = 2;
    public int catalystHyperMultiplier = 2;
    public int spawnerCostMultiplier = 1;
    
    @SectionHeader("particleAccelerator")
    public int maxGateDist = 10;
    public int bendFactor = 3;
    public int accelerationRFCost = 10;
    public int endPortalRequiredSpeed = 10000;
    public int netherPortalRequiredSpeed = 5000;
    public int blackHoleRequiredSpeed = 15000;

    @SectionHeader("blackHole")
    public int pullTimeMultiplier = 8;
    public int pullRange = 16;
    public int idleWaitTicks = 200;
    
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
    
    public static class ProcessingMachines {
        
        public int machineFrameMaxLength = 64;
        
        @Nest
        public BasicEnergyMachineData assemblerData = new BasicEnergyMachineData(10000, 1024, 0, 128);
        @Nest
        public BasicEnergyMachineData atomicForgeData = new BasicEnergyMachineData(1024, 0, 0, 1024);
        @Nest
        public CentrifugeConfig centrifugeData = new CentrifugeConfig();
        @Nest
        public BasicEnergyMachineData foundryData = new BasicEnergyMachineData(10000, 1024, 0, 128);
        @Nest
        public BasicEnergyMachineData fragmentForgeData = new BasicEnergyMachineData(10000, 2048, 0, 256);
        @Nest
        public FurnaceConfig furnaceData = new FurnaceConfig();
        @Nest
        public BasicEnergyMachineData pulverizerData = new BasicEnergyMachineData(10000, 256, 0, 32);
    }
    
    public static class Generators {
        
        public float animationSpeedMultiplier = 10;
        public float rfToSteamRation = 2;
        
        @Nest
        public BasicEnergyMachineData basicGeneratorData = new BasicEnergyMachineData(50_000, 0, 512, 32);
        @Nest
        public BasicEnergyMachineData bioGeneratorData = new BasicEnergyMachineData(100_000, 0, 1024, 64);
        @Nest
        public BasicEnergyMachineData lavaGeneratorData = new BasicEnergyMachineData(100_000, 0, 1024, 64);
        @Nest
        public BasicEnergyMachineData fuelGeneratorData = new BasicEnergyMachineData(250_000, 0, 2048, 256);
        @Nest
        public BasicEnergyMachineData steamEngineData = new BasicEnergyMachineData(100_000, 0, 10_000, 1);
        @Nest
        public BasicEnergyMachineData solarGeneratorData = new BasicEnergyMachineData(100_000, 0, 1024, 32);
    }
    
    public static class LaserArmConfig {
        public long energyCapacity = 20000;
        public long maxEnergyInsertion = 512;
        public long energyPerTick = 128;
        public int blockBreakEnergyBase = 1024; // multiplied by block hardness
        public float damageTickBase = 2;
        public int range = 128;
    }
    
    public static class DeepDrillConfig {
        public long energyCapacity = 20000;
        public int stepsPerOre = 20;
        public int energyPerStep = 1024;
    }
    
    public static class CentrifugeConfig {
        public long energyCapacity = 10000;
        public long maxEnergyInsertion = 512;
        public int energyPerTick = 64;
        public long tankSizeInBuckets = 8;
    }
    
    public static class FurnaceConfig {
        public long energyCapacity = 10000;
        public long maxEnergyInsertion = 256;
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
