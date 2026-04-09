package rearth.oritech;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.api.attachment.Attachment;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;

import java.util.ServiceLoader;

public interface OritechPlatform {
    
    OritechPlatform INSTANCE = ServiceLoader.load(OritechPlatform.class)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Failed to load platform service."));

    // Network
    void sendBlockHandle(BlockEntity blockEntity, CustomPacketPayload message);

    void sendPlayerHandle(CustomPacketPayload message, ServerPlayer player);

    void sendToServer(CustomPacketPayload message);

    <T extends CustomPacketPayload> void registerToClient(CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> packetCodec, TriConsumer<T, Level, RegistryAccess> consumer);

    <T extends CustomPacketPayload> void registerToServer(CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> packetCodec, TriConsumer<T, Player, RegistryAccess> consumer);
    
    // Attachment
    <T> void register(Attachment<T> attachment);

    <T> boolean hasAttachment(LivingEntity entity, Attachment<T> attachment);

    <T> T getAttachmentValue(LivingEntity entity, Attachment<T> attachment);

    <T> void setAttachment(LivingEntity entity, Attachment<T> attachment, T value);

    <T> void removeAttachment(LivingEntity entity, Attachment<T> attachment);

    // FakeMachinePlayer
    ServerPlayer create(ServerLevel world, GameProfile profile, SimpleInventoryStorage inventory);
    
    void resetCapabilities(ServerLevel world, BlockPos pos);
    
    /**
     * Fires a BlockEvent.BreakEvent/PlayerBlockBreakEvents.BEFORE and returns whether it was allowed (not cancelled).
     *
     * @param level  The level
     * @param pos    The block position
     * @param state  The current block state
     * @param player The player (can be null for non-player breaks)
     * @return true if the player can break the target block
     */
    boolean canPlayerBreakBlock(Level level, BlockPos pos, BlockState state, @NotNull Player player);
    
    /**
     * Fires a LivingDamageEvent.Pre or fabric equivalent and returns whether it was allowed (not cancelled).
     *
     */
    boolean canAttackBeDone(Level level, LivingEntity target, float amount, DamageSource damageSource);
}
