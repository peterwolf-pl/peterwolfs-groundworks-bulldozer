package com.piotrek.groundworksbulldozer;

import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksbulldozer.entity.BulldozerState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BladeSaveLoadTest {

    @BeforeAll
    static void init() {
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("Save and load roundtrip of blade position, angle, and carried granular material")
    void testSaveLoadRoundtrip() {
        BulldozerState original = new BulldozerState(
                -0.35F, // Lowered blade height
                -4.2F,  // Blade angle
                384,    // Carried units
                GranularMaterialRegistry.GRAVEL.id(), // Gravel material ID
                0.08F,  // Left track speed
                0.08F,  // Right track speed
                2.5F,   // Ground pitch
                -1.2F   // Ground roll
        );

        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        original.save(output);
        CompoundTag tag = output.buildResult();

        HolderLookup.Provider lookup = HolderLookup.Provider.create(Stream.empty());
        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, lookup, tag);
        BulldozerState loaded = BulldozerState.load(input);

        assertEquals(original.bladeHeight(), loaded.bladeHeight(), 1e-4F, "Blade height must persist exactly");
        assertEquals(original.bladeAngle(), loaded.bladeAngle(), 1e-4F, "Blade angle must persist exactly");
        assertEquals(original.carriedUnits(), loaded.carriedUnits(), "Carried units must persist exactly");
        assertEquals(original.carriedMaterialId(), loaded.carriedMaterialId(), "Carried material ID must persist exactly");
        assertEquals(original.leftTrackSpeed(), loaded.leftTrackSpeed(), 1e-4F);
        assertEquals(original.rightTrackSpeed(), loaded.rightTrackSpeed(), 1e-4F);
        assertEquals(original.vehiclePitch(), loaded.vehiclePitch(), 1e-4F);
        assertEquals(original.vehicleRoll(), loaded.vehicleRoll(), 1e-4F);
    }
}
