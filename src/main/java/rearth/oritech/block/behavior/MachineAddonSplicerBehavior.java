package rearth.oritech.block.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import rearth.oritech.Oritech;
import rearth.oritech.api.splicer.SplicerBehavior;
import rearth.oritech.api.splicer.SplicerBehaviors;
import rearth.oritech.api.splicer.SplicerContext;
import rearth.oritech.api.splicer.SplicerOperation;
import rearth.oritech.block.blocks.addons.HeartOfTheMachineAddonBlock;
import rearth.oritech.block.entity.addons.ShrunkAddonData;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ComponentContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default splicer behavior: compacts all machine addons connected to the splicer into a heart of the machine.
 */
public class MachineAddonSplicerBehavior implements SplicerBehavior {

    public static void registerDefaults() {
        SplicerBehaviors.register(Oritech.id("machine_addons"), 0, new MachineAddonSplicerBehavior());
    }

    @Override
    public Optional<SplicerOperation> findOperation(SplicerContext context) {

        var splicer = context.splicer();
        var addons = List.copyOf(splicer.getConnectedAddons());
        if (addons.isEmpty()) return Optional.empty();

        var level = context.level();
        var fluid = false;
        var quarryCount = 0;
        var hunterCount = 0;
        var yieldCount = 0;
        var cropFilter = false;
        var silk = false;

        for (var addonPos : addons) {
            var block = level.getBlockState(addonPos).getBlock();
            if (block.equals(BlockContent.MACHINE_FLUID_ADDON.get())) fluid = true;
            if (block.equals(BlockContent.QUARRY_ADDON.get())) quarryCount++;
            if (block.equals(BlockContent.MACHINE_HUNTER_ADDON.get())) hunterCount++;
            if (block.equals(BlockContent.MACHINE_YIELD_ADDON.get())) yieldCount++;
            if (block.equals(BlockContent.CROP_FILTER_ADDON.get())) cropFilter = true;
            if (block.equals(BlockContent.MACHINE_SILK_TOUCH_ADDON.get())) silk = true;
        }

        var data = new ShrunkAddonData(splicer.getBaseAddonData(), fluid, quarryCount, hunterCount, yieldCount, cropFilter, silk);
        return Optional.of(new AddonOperation(data, addons));
    }

    private record AddonOperation(ShrunkAddonData data, List<BlockPos> addons) implements SplicerOperation {

        @Override
        public List<Component> getTooltip() {
            var lines = new ArrayList<Component>();
            lines.add(Component.translatable("tooltip.oritech.addon_splicer_action"));
            lines.addAll(HeartOfTheMachineAddonBlock.getShrinkTooltip(data));
            return lines;
        }

        @Override
        public ItemStack getPreviewResult() {
            var stack = new ItemStack(BlockContent.HEART_OF_THE_MACHINE_ADDON.asItem());
            stack.set(ComponentContent.ADDON_DATA.get(), data);
            return stack;
        }

        @Override
        public List<BlockPos> getAffectedPositions() {
            return addons;
        }

        @Override
        public ItemStack execute(ServerLevel level) {
            for (var addonPos : addons.reversed()) {
                level.setBlock(addonPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            return getPreviewResult();
        }
    }
}
