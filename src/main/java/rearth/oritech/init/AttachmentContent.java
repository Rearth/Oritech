package rearth.oritech.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.entity.augmenter.api.Augment;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AttachmentContent {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Oritech.MOD_ID);

    public static final Supplier<AttachmentType<GlobalPos>> PORTAL_TARGET = ATTACHMENT_TYPES.register(
            "portal_target", () -> AttachmentType.builder(() -> GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO))
                    .sync(GlobalPos.STREAM_CODEC)
                    .serialize(GlobalPos.MAP_CODEC)
                    .copyOnDeath()
                    .build()
    );

    @SuppressWarnings("unchecked")
    public static final Supplier<AttachmentType<Map<Identifier, Augment.AugmentState>>> ACTIVE_AUGMENTS = ATTACHMENT_TYPES.register(
            "playeraugments", () -> AttachmentType.builder(() -> new HashMap<Identifier, Augment.AugmentState>())
                    .sync(ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, NetworkManager.getAutoCodec(Augment.AugmentState.class)))
                    .serialize(Codec.unboundedMap(Identifier.CODEC, Augment.AugmentState.CODEC).fieldOf("augments"))
                    .copyOnDeath()
                    .build()
    );

}
