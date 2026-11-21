package dev.sublevelmanager.client.xaero;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import xaero.map.element.MapElementRenderer;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

public final class StructureMapRenderer
        extends MapElementRenderer<StructureMapElement, StructureMapContext, StructureMapRenderer> {

    public StructureMapRenderer(final StructureMapContext context) {
        super(context, new StructureMapProvider(context), new StructureMapReader());
    }

    @Override
    public void beforeRender(
            final int location,
            final Minecraft minecraft,
            final GuiGraphics graphics,
            final double cameraX,
            final double cameraZ,
            final double mouseX,
            final double mouseZ,
            final float brightness,
            final double scale,
            final double screenSizeBasedScale,
            final TextureManager textureManager,
            final Font font,
            final MultiBufferSource.BufferSource bufferSource,
            final MultiTextureRenderTypeRendererProvider rendererProvider,
            final boolean pre) {
        if (minecraft.level != null) {
            getContext().setDimension(minecraft.level.dimension());
        }
    }

    @Override
    public void afterRender(
            final int location,
            final Minecraft minecraft,
            final GuiGraphics graphics,
            final double cameraX,
            final double cameraZ,
            final double mouseX,
            final double mouseZ,
            final float brightness,
            final double scale,
            final double screenSizeBasedScale,
            final TextureManager textureManager,
            final Font font,
            final MultiBufferSource.BufferSource bufferSource,
            final MultiTextureRenderTypeRendererProvider rendererProvider,
            final boolean pre) {
    }

    @Override
    public void renderElementPre(
            final int location,
            final StructureMapElement element,
            final boolean hovered,
            final Minecraft minecraft,
            final GuiGraphics graphics,
            final double cameraX,
            final double cameraZ,
            final double mouseX,
            final double mouseZ,
            final float brightness,
            final double scale,
            final double screenSizeBasedScale,
            final TextureManager textureManager,
            final Font font,
            final MultiBufferSource.BufferSource bufferSource,
            final MultiTextureRenderTypeRendererProvider rendererProvider,
            final float optionalScale,
            final double partialX,
            final double partialY,
            final boolean cave,
            final float partialTicks) {
    }

    @Override
    public boolean renderElement(
            final int location,
            final StructureMapElement element,
            final boolean hovered,
            final Minecraft minecraft,
            final GuiGraphics graphics,
            final double cameraX,
            final double cameraZ,
            final double mouseX,
            final double mouseZ,
            final float brightness,
            final double scale,
            final double screenSizeBasedScale,
            final TextureManager textureManager,
            final Font font,
            final MultiBufferSource.BufferSource bufferSource,
            final MultiTextureRenderTypeRendererProvider rendererProvider,
            final int elementIndex,
            final double optionalDepth,
            final float optionalScale,
            final double partialX,
            final double partialY,
            final boolean cave,
            final float partialTicks) {
        final PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(partialX, partialY, 0.0);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(45.0f));

        final int color;
        if (hovered) {
            color = 0xFFFFD24A;
        } else if (element.entry().loaded()) {
            color = 0xFF5BD0C8;
        } else {
            color = 0xFF9A9A9A;
        }
        graphics.fill(-9, -9, 9, 9, 0xCC101018);
        graphics.fill(-7, -7, 7, 7, color);
        pose.popPose();

        if (hovered) {
            final String owner = element.entry().ownerLabel();
            final String name = element.entry().name().isBlank()
                    ? element.entry().id().toString().substring(0, 8)
                    : element.entry().name();
            graphics.drawString(font, Component.literal(owner + " · " + name),
                    (int) partialX + 14, (int) partialY - 4, 0xFFFFFFFF, true);
        }
        return true;
    }

    @Override
    public void renderElementShadow(
            final StructureMapElement element,
            final boolean hovered,
            final float optionalScale,
            final double partialX,
            final double partialY,
            final ElementRenderInfo renderInfo,
            final GuiGraphics graphics,
            final MultiBufferSource.BufferSource bufferSource,
            final MultiTextureRenderTypeRendererProvider rendererProvider) {
    }

    @Override
    public boolean shouldRender(final int location, final boolean shadow) {
        return !shadow && ElementRenderLocation.fromIndex(location) == ElementRenderLocation.WORLD_MAP;
    }
}
