package rearth.oritech.block.blocks.pipes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.pipes.SmartSplitterBlockEntity;

import java.util.List;
import java.util.function.Consumer;

public class SmartSplitterBlock extends Block implements EntityBlock, TooltipProvider {

    public static final EnumProperty<SideMode> NORTH = EnumProperty.create("north", SideMode.class);
    public static final EnumProperty<SideMode> EAST = EnumProperty.create("east", SideMode.class);
    public static final EnumProperty<SideMode> SOUTH = EnumProperty.create("south", SideMode.class);
    public static final EnumProperty<SideMode> WEST = EnumProperty.create("west", SideMode.class);

    public SmartSplitterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(NORTH, SideMode.CLOSED)
                .setValue(EAST, SideMode.CLOSED)
                .setValue(SOUTH, SideMode.CLOSED)
                .setValue(WEST, SideMode.CLOSED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmartSplitterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof SmartSplitterBlockEntity splitter) splitter.serverTick(tickerLevel);
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SmartSplitterBlockEntity splitter)) return InteractionResult.PASS;

        var clickedSide = hit.getDirection();
        if (!player.isShiftKeyDown() && clickedSide.getAxis().isHorizontal()) {
            var property = propertyFor(clickedSide);
            var enabled = state.getValue(property) != SideMode.OUTPUT;
            var newSideMode = enabled ? SideMode.OUTPUT : SideMode.CLOSED;
            level.setBlock(pos, state.setValue(property, newSideMode), Block.UPDATE_ALL);
            splitter.onOutputConfigurationChanged();
            player.sendSystemMessage(Component.translatable(
                    enabled ? "message.oritech.smart_splitter.output_enabled" : "message.oritech.smart_splitter.output_disabled",
                    Component.translatable("tooltip.oritech.input_dir." + clickedSide.getName())
            ));
        } else {
            var mode = splitter.cycleMode();
            player.sendSystemMessage(Component.translatable("message.oritech.smart_splitter.mode", Component.translatable(mode.translationKey())));
        }

        level.playSound(null, pos, SoundEvents.BAMBOO_WOOD_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.8f, 1.1f);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var drops = super.getDrops(state, builder);
        var blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof SmartSplitterBlockEntity splitter) {
            drops.addAll(splitter.inventory.getStacks());
            splitter.inventory.getStacks().clear();
        }
        return drops;
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter componentGetter) {
        consumer.accept(Component.translatable("tooltip.oritech.smart_splitter").withStyle(ChatFormatting.GRAY));
        consumer.accept(Component.translatable("tooltip.oritech.smart_splitter.configure").withStyle(ChatFormatting.GRAY));
    }

    public static boolean isOutput(BlockState state, Direction direction) {
        return getSideMode(state, direction) == SideMode.OUTPUT;
    }

    public static SideMode getSideMode(BlockState state, Direction direction) {
        return direction.getAxis().isHorizontal() ? state.getValue(propertyFor(direction)) : SideMode.CLOSED;
    }

    public static BlockState setSideMode(BlockState state, Direction direction, SideMode mode) {
        return state.setValue(propertyFor(direction), mode);
    }

    private static EnumProperty<SideMode> propertyFor(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException("Not a horizontal direction: " + direction);
        };
    }

    public enum SideMode implements StringRepresentable {
        CLOSED("closed"),
        INPUT("input"),
        OUTPUT("output");

        private final String serializedName;

        SideMode(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
