package rearth.oritech.api.screen.data;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.block.entity.arcane.EnchanterBlockEntity;
import rearth.oritech.block.entity.generators.BasicGeneratorEntity;
import rearth.oritech.util.ScreenProvider;
import rearth.oritech.util.TooltipHelper;

import java.util.function.Supplier;

public abstract class DisplayDataSource {
    private final long capacity;
    private final Supplier<Long> amountSupplier;
    private final Supplier<Component> tooltipSupplier;
    private final ScreenProvider.BarConfiguration config;
    
    public DisplayDataSource(long capacity, Supplier<Long> amountSupplier, Supplier<Component> tooltipSupplier, ScreenProvider.BarConfiguration config) {
        this.capacity = capacity;
        this.amountSupplier = amountSupplier;
        this.tooltipSupplier = tooltipSupplier;
        this.config = config;
    }
    
    public long capacity() {
        return capacity;
    }
    
    public Supplier<Long> amountSupplier() {
        return amountSupplier;
    }
    
    public ScreenProvider.BarConfiguration config() {
        return config;
    }
    
    public Supplier<Component> getTooltipSupplier() {
        return tooltipSupplier;
    }
    
    public static class FluidDataSource extends DisplayDataSource {
        
        private final Supplier<FluidStack> fluidSupplier;
        private final FluidApi.SingleSlotStorage storage;
        private int tankIndex;
        
        private FluidDataSource(FluidApi.SingleSlotStorage storage, long capacity, Supplier<FluidStack> fluidSupplier, Supplier<Component> tooltipSupplier, ScreenProvider.BarConfiguration config) {
            super(capacity, () -> fluidSupplier.get().getAmount(), tooltipSupplier, config);
            this.storage = storage;
            this.fluidSupplier = fluidSupplier;
        }
        
        public Supplier<FluidStack> getFluidSupplier() {
            return fluidSupplier;
        }
        
        public FluidApi.SingleSlotStorage getStorage() {
            return storage;
        }
        
        public int getTankIndex() {
            return tankIndex;
        }
        
        public void setTankIndex(int tankIndex) {
            this.tankIndex = tankIndex;
        }
    }
    
    public static FluidDataSource CreateFluid(FluidApi.SingleSlotStorage storage, ScreenProvider.BarConfiguration config, ScreenProvider provider) {
        
        return new FluidDataSource(
          storage,
          storage.getCapacity(),
          storage::getStack,
          () -> getFluidTooltip(storage.getStack()),
          config);
    }
    
    public static Component getFluidTooltip(FluidStack stack) {
        
        return stack.getAmount() > 0
                            ? Component.translatable("tooltip.oritech.tank_content", stack.getAmount() * 1000 / FluidStackHooks.bucketAmount(), FluidStackHooks.getName(stack).getString())
                            : Component.translatable("tooltip.oritech.tank_empty");
    }
    
    public static class EnergyDataSource extends DisplayDataSource {
        
        private EnergyDataSource(long capacity, Supplier<Long> amountSupplier, Supplier<Component> tooltipSupplier, ScreenProvider.BarConfiguration config) {
            super(capacity, amountSupplier, tooltipSupplier, config);
        }
    }
    
    public static EnergyDataSource CreateEnergy(EnergyApi.EnergyStorage storage, ScreenProvider.BarConfiguration config, ScreenProvider provider) {
        
        return new EnergyDataSource(
          storage.getCapacity(),
          storage::getAmount,
          () -> getEnergyTooltip(storage.getAmount(), storage.getCapacity(), (long) provider.getDisplayedEnergyUsage(), (long) provider.getDisplayedEnergyTransfer(), provider.showEnergyUsage(), provider.showEnergyTransfer()),
          config);
    }
    
    public static Component getEnergyTooltip(long amount, long max, long usage, long transfer, boolean showUsage, boolean showTransfer) {
        float percentage = (float) amount / max;
        
        var res = Component.translatable("tooltip.oritech.energy_usage_base",
          TooltipHelper.getEnergyText(amount),
          TooltipHelper.getEnergyText(max),
          String.format("%.1f", percentage * 100));
        
        if (showUsage)
            res = res.append(Component.translatable("tooltip.oritech.energy_usage_usage", TooltipHelper.getEnergyText(usage)));
        if (showTransfer)
            res = res.append(Component.translatable("tooltip.oritech.energy_usage_transfer", TooltipHelper.getEnergyText(transfer)));
        
        
        return res;
    }

    public static class SoulDataSource extends DisplayDataSource {

        private SoulDataSource(long capacity, Supplier<Long> amountSupplier, Supplier<Component> tooltipSupplier, ScreenProvider.BarConfiguration config) {
            super(capacity, amountSupplier, tooltipSupplier, config);
        }
    }

    public static SoulDataSource CreateSoul(long capacity, Supplier<Long> amountSupplier, ScreenProvider.BarConfiguration config) {

        return new SoulDataSource(
          capacity,
          amountSupplier,
          () -> getSoulTooltip(amountSupplier.get(), capacity),
          config);
    }

    public static Component getSoulTooltip(long amount, long max) {
        return Component.translatable("tooltip.oritech.spawner.collected_souls", amount, max);
    }

    public static class ProgressDataSource extends DisplayDataSource {

        private ProgressDataSource(long capacity, Supplier<Long> amountSupplier, Supplier<Component> tooltipSupplier, ScreenProvider.BarConfiguration config) {
            super(capacity, amountSupplier, tooltipSupplier, config);
        }
    }

    public static ProgressDataSource CreateProgress(ScreenProvider provider, BlockEntity blockEntity) {
        
        var config = provider.getIndicatorConfiguration();
        
        return new ProgressDataSource(
          1000,
            () -> (long) Math.round(provider.getProgress() * 1000),
          () -> getProgressTooltip(blockEntity),
          new ScreenProvider.BarConfiguration(config.x(), config.y(), config.width(), config.height()));
    }

    public static Component getProgressTooltip(BlockEntity blockEntity) {
        if (blockEntity instanceof MachineBlockEntity machineEntity
            && (machineEntity.getRecipeDuration() > 0 || machineEntity.progress > 0)) {

            var progressTicks = machineEntity.progress;
            var recipeDurationTicks = machineEntity.getRecipeDuration();
            var effectiveDurationTicks = (int) (recipeDurationTicks * machineEntity.getSpeedMultiplier());

            if (machineEntity instanceof UpgradableGeneratorBlockEntity generatorBlock) {
                if (recipeDurationTicks <= 0)
                    recipeDurationTicks = (int) (generatorBlock.currentMaxBurnTime / generatorBlock.getSpeedMultiplier() * generatorBlock.getEfficiencyMultiplier());
                effectiveDurationTicks = generatorBlock.currentMaxBurnTime;
            }

            if (machineEntity instanceof BasicGeneratorEntity generatorEntity)
                recipeDurationTicks = generatorEntity.currentMaxBurnTime;
            

            return Component.translatable("tooltip.oritech.progress_indicator", progressTicks, effectiveDurationTicks, recipeDurationTicks);
        } else if (blockEntity instanceof EnchanterBlockEntity enchanterBlock && enchanterBlock.progress > 0) {
            var maxTicks = enchanterBlock.maxProgress * 5;
            var progress = enchanterBlock.progress * 5;
            return Component.translatable("tooltip.oritech.progress_indicator", progress, maxTicks, maxTicks);
        }

        return Component.empty();
    }
    
}
