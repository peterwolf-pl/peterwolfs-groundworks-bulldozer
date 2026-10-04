package com.piotrek.groundworksbulldozer.client.sound;

import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import com.piotrek.groundworksbulldozer.entity.GroundworksBulldozerEntity;
import com.piotrek.groundworksbulldozer.vehicle.EngineSoundProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Starts and tracks one positional diesel loop for each running bulldozer. */
public final class BulldozerEngineSoundController {

    private static final Map<Integer, EngineLoop> ACTIVE = new HashMap<>();
    private static ClientLevel activeLevel;

    private BulldozerEngineSoundController() {}

    public static void clientTick(Minecraft client) {
        if (activeLevel != client.level) {
            ACTIVE.values().forEach(EngineLoop::stopNow);
            ACTIVE.clear();
            activeLevel = client.level;
        }
        if (client.level == null) return;

        Iterator<EngineLoop> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isStopped()) iterator.remove();
        }

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof GroundworksBulldozerEntity bulldozer)
                    || !bulldozer.isEngineRunning()
                    || ACTIVE.containsKey(bulldozer.getId())) {
                continue;
            }
            EngineLoop sound = new EngineLoop(bulldozer);
            ACTIVE.put(bulldozer.getId(), sound);
            client.getSoundManager().play(sound);
        }
    }

    private static final class EngineLoop extends AbstractTickableSoundInstance {

        private final GroundworksBulldozerEntity bulldozer;

        private EngineLoop(GroundworksBulldozerEntity bulldozer) {
            super(GroundworksBulldozerMod.ENGINE_LOOP, SoundSource.NEUTRAL, RandomSource.create());
            this.bulldozer = bulldozer;
            this.looping = true;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            updateSound();
        }

        @Override
        public void tick() {
            if (bulldozer.isRemoved() || !bulldozer.isEngineRunning()) {
                stop();
                return;
            }
            updateSound();
        }

        private void updateSound() {
            this.x = bulldozer.getX();
            this.y = bulldozer.getY() + 1.0D;
            this.z = bulldozer.getZ();
            EngineSoundProfile.Mix mix = EngineSoundProfile.forTrackSpeeds(
                    bulldozer.getTrackLeftSpeed(), bulldozer.getTrackRightSpeed());
            this.volume = mix.volume();
            this.pitch = mix.pitch();
        }

        private void stopNow() {
            stop();
        }
    }
}
