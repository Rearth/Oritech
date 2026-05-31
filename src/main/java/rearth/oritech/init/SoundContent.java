package rearth.oritech.init;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.Oritech;

public class SoundContent {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Oritech.MOD_ID);

    // public static final Holder<SoundEvent> CABLE_MOVING = SOUND_EVENTS.register("cable_moving", () -> SoundEvent.createVariableRangeEvent(SOUND_EVENTS.id("cable_moving")));
    public static final Holder<SoundEvent> CABLE_MOVING = SOUND_EVENTS.register("cable_moving", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> SERVO_NOISES = SOUND_EVENTS.register("servo_noises", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> MECHANICAL_CLICK = SOUND_EVENTS.register("mechanical_click", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> WELDING1 = SOUND_EVENTS.register("welding1", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> WELDING2 = SOUND_EVENTS.register("welding2", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> SAW_WORKING = SOUND_EVENTS.register("saw_working", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> DYNAMO = SOUND_EVENTS.register("dynamo", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> FURNACE_BURN = SOUND_EVENTS.register("furnace_burn", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> GRINDER_WORKING = SOUND_EVENTS.register("grinder_working", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> LAVA_BUBBLES = SOUND_EVENTS.register("lava_bubbles", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> LIQUID_FLOW = SOUND_EVENTS.register("liquid_flow", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> QUADCOPTER_FLYING = SOUND_EVENTS.register("quadcopter_flying", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> QUADCOPTER_TAKEOFF = SOUND_EVENTS.register("quadcopter_takeoff", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> SIZZLING_SOUND = SOUND_EVENTS.register("sizzling_sound", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> SQUISH = SOUND_EVENTS.register("squish", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> PRESS = SOUND_EVENTS.register("press", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> PARTICLE_MOVING = SOUND_EVENTS.register("particle_moving", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> SHORT_SERVO = SOUND_EVENTS.register("short_servo", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> WRENCH_TURN = SOUND_EVENTS.register("wrench_turn", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> REACTOR = SOUND_EVENTS.register("reactor", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> REACTOR_LOADING = SOUND_EVENTS.register("reactor_loading", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> REACTOR_WARNING = SOUND_EVENTS.register("reactor_warning", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> NUKE_EXPLOSION = SOUND_EVENTS.register("nuke_explosion", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> ELECTRIC_SHOCK = SOUND_EVENTS.register("electric_shock", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> BEDROCK_EXTRACTOR = SOUND_EVENTS.register("bedrock_extractor", SoundEvent::createVariableRangeEvent);

}
