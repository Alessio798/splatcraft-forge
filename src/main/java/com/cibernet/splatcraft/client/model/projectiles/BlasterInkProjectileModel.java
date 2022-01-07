package com.cibernet.splatcraft.client.model.projectiles;// Made with Blockbench 3.8.0
// Exported for Minecraft version 1.15
// Paste this class into your mod and generate all required imports


import com.cibernet.splatcraft.entities.InkProjectileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.model.ModelRenderer;

public class BlasterInkProjectileModel extends InkProjectileModel
{
    private final ModelRenderer main;

    public BlasterInkProjectileModel()
    {
        texWidth = 16;
        texHeight = 16;

        main = new ModelRenderer(this);
        main.setPos(0.0F, -1.0F, 0.0F);
        main.texOffs(0, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, -0.5F, false);
        main.texOffs(0, 8).addBox(-1.5F, -1.5F, 1.25F, 3.0F, 3.0F, 1.0F, -0.2F, false);
    }

    @Override
    public void setupAnim(InkProjectileEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        main.zRot = ageInTicks * 0.6f;
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        main.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z)
    {
        modelRenderer.xRot = x;
        modelRenderer.yRot = y;
        modelRenderer.zRot = z;
    }
}
