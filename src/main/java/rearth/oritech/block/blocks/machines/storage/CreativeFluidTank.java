package rearth.oritech.block.blocks.machines.storage;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.mixin.transfer.BucketItemAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.machines.storage.CreativeFluidTankEntity;
import rearth.oritech.init.BlockContent;

import java.util.List;

import static rearth.oritech.util.TooltipHelper.addMachineTooltip;

public class CreativeFluidTank extends SmallFluidTank {

    public CreativeFluidTank(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        super.appendTooltip(stack, context, tooltip, options);
        addMachineTooltip(tooltip, this, this);
        if (Screen.hasControlDown())
            tooltip.add(Text.translatable("tooltip.oritech.creative_tank").formatted(Formatting.GRAY));
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ((CreativeFluidTankEntity) world.getBlockEntity(pos)).getComparatorOutput();
    }

    protected List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        var droppedStacks = super.getDroppedStacks(state, builder);

        var blockEntity = (BlockEntity)builder.getOptional(LootContextParameters.BLOCK_ENTITY);
        if (blockEntity instanceof CreativeFluidTankEntity tankEntity)
            droppedStacks.addAll(tankEntity.inventory.getHeldStacks());

        return droppedStacks;
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return getStackWithData(world, pos);
    }

    @NotNull
    private static ItemStack getStackWithData(WorldView world, BlockPos pos) {
        var stack = new ItemStack(BlockContent.CREATIVE_TANK_BLOCK.asItem());
        var tankEntity = (CreativeFluidTankEntity) world.getBlockEntity(pos);

        if (tankEntity.getForDirectFluidAccess().amount > 0) {
            var nbt = new NbtCompound();
            tankEntity.writeNbt(nbt, world.getRegistryManager());
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            var fluidName = FluidVariantAttributes.getName(tankEntity.getForDirectFluidAccess().variant);
            stack.set(DataComponentTypes.CUSTOM_NAME, fluidName.copy().append(" ").append(Text.translatable("block.oritech.creative_tank_block")));
        }

        return stack;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        if (!itemStack.contains(DataComponentTypes.CUSTOM_DATA)) return;

        var tankEntity = (CreativeFluidTankEntity) world.getBlockEntity(pos);
        var nbt = itemStack.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
        if (nbt != null)
            tankEntity.readNbt(nbt, world.getRegistryManager());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeFluidTankEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if(!(world.getBlockEntity(pos) instanceof CreativeFluidTankEntity blockEntity)) return ActionResult.PASS;  // is this necessary?
        ItemStack mainHandStack = player.getMainHandStack();
        if (!world.isClient && mainHandStack.isOf(Items.BUCKET)) {
            blockEntity.removeFluid();
            blockEntity.markDirty();
            return ActionResult.SUCCESS_NO_ITEM_USED;
        }
        if (!world.isClient && !mainHandStack.isEmpty() && mainHandStack.getItem() instanceof BucketItem) {
            blockEntity.setFluid(((BucketItemAccessor) mainHandStack.getItem()).fabric_getFluid());
            blockEntity.markDirty();
            return ActionResult.SUCCESS_NO_ITEM_USED;
        }
        return super.onUse(state, world, pos, player, hit);
    }
}
