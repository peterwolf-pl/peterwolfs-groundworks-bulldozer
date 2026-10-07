package com.piotrek.groundworksbulldozer.client.sound;

import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import com.piotrek.groundworksbulldozer.entity.GroundworksBulldozerEntity;
import com.piotrek.groundworksbulldozer.vehicle.EngineSoundProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Crossfades the same idle and loaded diesel layers used by the Groundworks excavator. */
public final class BulldozerEngineSoundController {

    private static final Map<Integer, EnginePair> ACTIVE = new HashMap<>();
    private static ClientLevel activeLevel;

    private BulldozerEngineSoundController() {}

    public static void clientTick(Minecraft client) {
        if (activeLevel != client.level) {
            ACTIVE.values().forEach(EnginePair::stopNow);
            ACTIVE.clear();
            activeLevel = client.level;
        }
        if (client.level == null) return;

        Iterator<EnginePair> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            EnginePair pair = iterator.next();
            if (pair.stopped()) {
                pair.stopNow();
                iterator.remove();
            }
        }

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof GroundworksBulldozerEntity bulldozer)
                    || !bulldozer.isEngineRunning()
                    || ACTIVE.containsKey(bulldozer.getId())) {
                continue;
            }
            EnginePair pair = new EnginePair(bulldozer);
            ACTIVE.put(bulldozer.getId(), pair);
            client.getSoundManager().play(pair.idle);
            client.getSoundManager().play(pair.load);
        }
    }

    private record EnginePair(EngineLoop idle, EngineLoop load) {
        private EnginePair(GroundworksBulldozerEntity bulldozer) {
            this(
                    new EngineLoop(bulldozer, GroundworksBulldozerMod.ENGINE_LOOP, false),
                    new EngineLoop(bulldozer, GroundworksBulldozerMod.ENGINE_LOAD, true)
            );
        }

        private boolean stopped() {
            return idle.isStopped() || load.isStopped();
        }

        private void stopNow() {
            idle.stopNow();
            load.stopNow();
        }
    }

    private static final class EngineLoop extends AbstractTickableSoundInstance {

        private final GroundworksBulldozerEntity bulldozer;
        private final boolean loadLayer;

        private EngineLoop(GroundworksBulldozerEntity bulldozer, SoundEvent sound, boolean loadLayer) {
            super(sound, SoundSource.NEUTRAL, RandomSource.create());
            this.bulldozer = bulldozer;
            this.loadLayer = loadLayer;
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

            EngineSoundProfile.Mix mix = EngineSoundProfile.forMachineLoad(
                    bulldozer.isPushing() ? 1.0F : 0.0F,
                    bulldozer.getTrackLeftSpeed(),
                    bulldozer.getTrackRightSpeed()
            );

            this.volume = loadLayer ? mix.loadVolume() : mix.volume();
            this.pitch = loadLayer ? mix.loadPitch() : mix.pitch();
        }

        private void stopNow() {
            stop();
        }
    }
}
