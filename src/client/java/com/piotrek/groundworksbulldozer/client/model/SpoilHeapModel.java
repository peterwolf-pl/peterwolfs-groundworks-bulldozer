package com.piotrek.groundworksbulldozer.client.model;

import com.piotrek.groundworksbulldozer.client.render.BulldozerRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * Three spoil heaps that ride the blade and wear the cargo block texture.
 * A 16-unit cube maps 1:1 onto a 16x16 block texture.
 */
public class SpoilHeapModel extends EntityModel<BulldozerRenderState> {

    private final ModelPart arms;
    private final ModelPart blade;
    private final ModelPart small;
    private final ModelPart medium;
    private final ModelPart large;
    private final float armScale;

    public SpoilHeapModel(ModelPart root, float armScale) {
        super(root);
        this.armScale = armScale;
        this.arms = root.getChild("arms");
        this.blade = this.arms.getChild("blade");
        this.small = this.blade.getChild("spoil_small");
        this.medium = this.blade.getChild("spoil_medium");
        this.large = this.blade.getChild("spoil_large");
    }

    public static SpoilHeapModel heavy() {
        return new SpoilHeapModel(create(10.0F, -6.0F, 56.0F, 18.0F, 8.0F, 6).bakeRoot(), 0.38F);
    }

    public static SpoilHeapModel standard() {
        return new SpoilHeapModel(create(15.0F, 0.0F, 34.0F, 8.0F, 5.0F, 3).bakeRoot(), 0.45F);
    }

    public static Identifier texture(int materialId) {
        String block = switch (materialId) {
            case 2 -> "sand";
            case 3 -> "gravel";
            default -> "dirt";
        };
        return Identifier.withDefaultNamespace("textures/block/" + block + ".png");
    }

    @Override
    public void setupAnim(BulldozerRenderState state) {
        float armAngle = state.bladeHeight * this.armScale;
        this.arms.xRot = armAngle;
        this.blade.xRot = -armAngle + (float) Math.toRadians(state.bladeAngle);
        float fill = state.carriedUnits <= 0 ? 0.0F : Math.max(0.01F, state.fillRatio);
        this.small.visible = fill > 0.0F && fill < 0.34F;
        this.medium.visible = fill >= 0.34F && fill < 0.67F;
        this.large.visible = fill >= 0.67F;
    }

    private static LayerDefinition create(
            float armY,
            float armZ,
            float bladeZ,
            float edgeY,
            float spoilZ,
            int widthBlocks
    ) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition arms = root.addOrReplaceChild("arms", CubeListBuilder.create(), PartPose.offset(0.0F, armY, armZ));
        PartDefinition blade = arms.addOrReplaceChild("blade", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, bladeZ));
        blade.addOrReplaceChild("spoil_small", heap(Math.max(1, widthBlocks / 3), 1, edgeY, spoilZ), PartPose.ZERO);
        blade.addOrReplaceChild("spoil_medium", heap(Math.max(2, widthBlocks * 2 / 3), 1, edgeY, spoilZ), PartPose.ZERO);
        blade.addOrReplaceChild("spoil_large", heap(widthBlocks, 2, edgeY, spoilZ), PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16);
    }

    private static CubeListBuilder heap(int blocksWide, int blocksHigh, float edgeY, float spoilZ) {
        CubeListBuilder cubes = CubeListBuilder.create();
        float x0 = -blocksWide * 8.0F;
        int crown = Math.max(1, blocksWide / 2);
        float crownX = -crown * 8.0F;
        for (int x = 0; x < blocksWide; x++) {
            cubes.texOffs(0, 0).addBox(x0 + x * 16.0F, edgeY - 16.0F, spoilZ, 16.0F, 16.0F, 16.0F);
        }
        if (blocksHigh > 1) {
            for (int x = 0; x < crown; x++) {
                cubes.texOffs(0, 0).addBox(crownX + x * 16.0F, edgeY - 32.0F, spoilZ, 16.0F, 16.0F, 16.0F);
            }
        }
        return cubes;
    }
}
