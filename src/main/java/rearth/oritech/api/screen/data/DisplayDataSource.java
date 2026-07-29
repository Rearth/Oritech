package rearth.oritech.api.screen.data;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.block.entity.arcane.StabilizedEnchanterBlockEntity;
import rearth.oritech.block.entity.generators.BasicGeneratorEntity;
import rearth.oritech.util.ScreenProvider;
import rearth.oritech.util.TooltipHelper;

import java.util.function.Supplier;

public abstract class DisplayDataSource {
    private final Supplier<Long> capacity;
    private final Supplier<Long> amountSupplier;
    private final Supplier<Component> tooltipSupplier;
    private final ScreenProvider.BarConfiguration config;

    public DisplayDataSource(Supplier<Long> capacity, Supplier<Long> amountSupplier, Supplier<Component> tooltipSupplier, ScreenProvider.BarConfiguration config) {
        this.capacity = capacity;
        this.amountSupplier = amountSupplier;
        this.tooltipSupplier = tooltipSupplier;
        this.config = config;
    }

    public Supplier<Long> capacitySupplier() {
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
        private final ResourceHandler<FluidResource> storage;
        private int tankIndex;

        private FluidDataSource(ResourceHandler<FluidResource> storage, Supplier<Long> capacity, Supplier<FluidStack> fluidSupplier, Supplier<Component> tooltipSupplier, ScreenProvider.BarConfiguration config) {
            super(capacity, () -> (long) fluidSupplier.get().amount(), tooltipSupplier, config);
            this.storage = storage;
            this.fluidSupplier = fluidSupplier;
        }

        public Supplier<FluidStack> getFluidSupplier() {
            return fluidSupplier;
        }

        public ResourceHandler<FluidResource> getStorage() {
            return storage;
        }

        public int getTankIndex() {
            return tankIndex;
        }

        public void setTankIndex(int tankIndex) {
            this.tankIndex = tankIndex;
        }
    }

    public static FluidDataSource CreateFluid(ResourceHandler<FluidResource> storage, ScreenProvider.BarConfiguration config, ScreenProvider provider) {

        return new FluidDataSource(
                storage,
                () -> storage.getCapacityAsLong(0, storage.getResource(0)),
                () -> storage.getResource(0).toStack(storage.getAmountAsInt(0)),
                () -> getFluidTooltip(storage.getResource(0).toStack(storage.getAmountAsInt(0))),
                config);
    }

    public static Component getFluidTooltip(FluidStack stack) {

        return stack.amount() > 0
                ? Component.translatable("tooltip.oritech.tank_content", stack.amount() * 1000L / FluidType.BUCKET_VOLUME, stack.getHoverName().getString())
                : Component.translatable("tooltip.oritech.tank_empty");
    }

    public static class EnergyDataSource extends DisplayDataSource {

        private EnergyDataSource(Supplier<Long> capacity, Supplier<Long> amountSupplier, Supplier<Component> tooltipSupplier, ScreenProvider.BarConfiguration config) {
            super(capacity, amountSupplier, tooltipSupplier, config);
        }
    }

    public static EnergyDataSource CreateEnergy(EnergyHandler storage, ScreenProvider.BarConfiguration config, ScreenProvider provider) {

        return new EnergyDataSource(
                storage::getCapacityAsLong,
                storage::getAmountAsLong,
                () -> getEnergyTooltip(storage.getAmountAsLong(), storage.getCapacityAsLong(), (long) provider.getDisplayedEnergyUsage(), (long) provider.getDisplayedEnergyTransfer(), provider.showEnergyUsage(), provider.showEnergyTransfer()),
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

        private SoulDataSource(Supplier<Long> capacity, Supplier<Long> amountSupplier, Supplier<Component> tooltipSupplier, ScreenProvider.BarConfiguration config) {
            super(capacity, amountSupplier, tooltipSupplier, config);
        }
    }

    public static SoulDataSource CreateSoul(Supplier<Long> capacity, Supplier<Long> amountSupplier, ScreenProvider.BarConfiguration config) {

        return new SoulDataSource(
                capacity,
                amountSupplier,
                () -> getSoulTooltip(amountSupplier.get(), capacity.get()),
                config);
    }

    public static Component getSoulTooltip(long amount, long max) {
        return Component.translatable("tooltip.oritech.spawner.collected_souls", amount, max);
    }

    public static class ProgressDataSource extends DisplayDataSource {

        private ProgressDataSource(long capacity, Supplier<Long> amountSupplier, Supplier<Component> tooltipSupplier, ScreenProvider.BarConfiguration config) {
            super(() -> capacity, amountSupplier, tooltipSupplier, config);
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
                && (machineEntity.getRecipeDuration() > 0 || machineEntity.getProgress() > 0)) {

            var progressTicks = machineEntity.progress.get();
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
        } else if (blockEntity instanceof StabilizedEnchanterBlockEntity stabilized_enchanterBlock && stabilized_enchanterBlock.progress > 0) {
            var maxTicks = stabilized_enchanterBlock.maxProgress * 5;
            var progress = stabilized_enchanterBlock.progress * 5;
            return Component.translatable("tooltip.oritech.progress_indicator", progress, maxTicks, maxTicks);
        }

        return Component.empty();
    }

}
