package rearth.oritech.block.blocks.machines.storage;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.machines.storage.SmallFluidTankEntity;
import rearth.oritech.init.BlockContent;

import java.util.List;

public class SmallFluidTank extends Block implements BlockEntityProvider {
    public static final BooleanProperty LIT = Properties.LIT;
    
    public SmallFluidTank(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(LIT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SmallFluidTankEntity(pos, state, false);
    }
    
    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }
    
    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ((SmallFluidTankEntity) world.getBlockEntity(pos)).getComparatorOutput();
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (!world.isClient) {
            var handler = (ExtendedScreenHandlerFactory) world.getBlockEntity(pos);
            player.openHandledScreen(handler);
            
        }
        
        return ActionResult.SUCCESS;
    }

    protected List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        var droppedStacks = super.getDroppedStacks(state, builder);

        var blockEntity = (BlockEntity)builder.getOptional(LootContextParameters.BLOCK_ENTITY);
        if (blockEntity instanceof SmallFluidTankEntity tankEntity)
            droppedStacks.addAll(tankEntity.inventory.getHeldStacks());

        return droppedStacks;
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return getStackWithData(world, pos);
    }
    
    @NotNull
    private static ItemStack getStackWithData(WorldView world, BlockPos pos) {
        var tankEntity = (SmallFluidTankEntity) world.getBlockEntity(pos);
        var stack = getBasePickStack(tankEntity.isCreative);
        
        if (tankEntity.getForDirectFluidAccess().amount > 0) {
            var nbt = new NbtCompound();
            tankEntity.writeNbt(nbt, world.getRegistryManager());
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            var fluidName = FluidVariantAttributes.getName(tankEntity.getForDirectFluidAccess().variant);
            stack.set(DataComponentTypes.CUSTOM_NAME, fluidName.copy().append(" ").append(Text.translatable(tankEntity.isCreative ? "block.oritech.creative_tank_block" : "block.oritech.small_tank_block")));
        }
        
        return stack;
    }
    
    public static ItemStack getBasePickStack(boolean creative) {
        return new ItemStack(creative ? BlockContent.CREATIVE_TANK_BLOCK.asItem() : BlockContent.SMALL_TANK_BLOCK.asItem());
    }
    
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        
        if (!itemStack.contains(DataComponentTypes.CUSTOM_DATA)) return;
        
        var tankEntity = (SmallFluidTankEntity) world.getBlockEntity(pos);
        var nbt = itemStack.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
        if (nbt != null)
            tankEntity.readNbt(nbt, world.getRegistryManager());
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
    
}
