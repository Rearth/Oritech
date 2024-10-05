package rearth.oritech.util;

import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class MovingSoundInstance extends PositionedSoundInstance {
    public MovingSoundInstance(Identifier id, SoundCategory category, float volume, float pitch, Random random, boolean repeat, int repeatDelay, AttenuationType attenuationType, double x, double y, double z, boolean relative) {
        super(id, category, volume, pitch, random, repeat, repeatDelay, attenuationType, x, y, z, relative);
    }
    
    public void setVolume(float volume) {
        this.volume = volume;
    }
    
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
    
    public void setPosition(Vec3d pos) {
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
    }
}
