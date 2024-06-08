package rearth.oritech.init;

import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;

public class SoundContent implements AutoRegistryContainer<SoundEvent> {
    
    public static final SoundEvent CABLE_MOVING = SoundEvent.of(new Identifier(Oritech.MOD_ID, "cable_moving"));
    public static final SoundEvent SERVO_NOISES = SoundEvent.of(new Identifier(Oritech.MOD_ID, "servo_noises"));
    public static final SoundEvent MECHANICAL_CLICK = SoundEvent.of(new Identifier(Oritech.MOD_ID, "mechanical_click"));
    public static final SoundEvent WELDING1 = SoundEvent.of(new Identifier(Oritech.MOD_ID, "welding1"));
    public static final SoundEvent WELDING2 = SoundEvent.of(new Identifier(Oritech.MOD_ID, "welding2"));
    public static final SoundEvent SAW_WORKING = SoundEvent.of(new Identifier(Oritech.MOD_ID, "saw_working"));
    
    @Override
    public Registry<SoundEvent> getRegistry() {
        return Registries.SOUND_EVENT;
    }
    
    @Override
    public Class<SoundEvent> getTargetFieldType() {
        return SoundEvent.class;
    }
}
