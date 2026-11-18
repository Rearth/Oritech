package rearth.oritech.spaceage.init;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.spaceage.OritechSpaceAge;

public final class SpaceAgeSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, OritechSpaceAge.MOD_ID);
    public static final Holder<SoundEvent> PARTICLE_SPACE_LAUNCH = SOUND_EVENTS.register("particle_space_launch", SoundEvent::createVariableRangeEvent);

    private SpaceAgeSounds() {
    }
}
