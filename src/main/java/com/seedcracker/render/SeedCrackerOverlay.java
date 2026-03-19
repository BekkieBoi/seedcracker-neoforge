package com.seedcracker.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.seedcracker.finder.FoundStructure;
import com.seedcracker.finder.StructureFinder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Draws coloured box outlines at found structure positions in the world.
 * Rendered after the level geometry pass so boxes appear through terrain.
 */
public final class SeedCrackerOverlay {

    private SeedCrackerOverlay() {}

    public static void render(Camera camera, PoseStack poseStack, Matrix4f projMatrix) {
        List<FoundStructure> structures = StructureFinder.getFoundStructures();
        if (structures.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        var pos = camera.getPosition();

        for (FoundStructure structure : structures) {
            drawBox(poseStack, structure.pos(), pos.x, pos.y, pos.z,
                    structure.color());
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawBox(PoseStack poseStack, BlockPos blockPos,
                                 double camX, double camY, double camZ,
                                 int color) {
        double x = blockPos.getX() - camX;
        double y = blockPos.getY() - camY;
        double z = blockPos.getZ() - camZ;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8)  & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;

        BufferBuilder buf = Tesselator.getInstance().begin(
                VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        drawEdges(buf, poseStack.last().pose(), (float)x, (float)y, (float)z, r, g, b);

        BufferUploader.drawWithShader(buf.build());
    }

    private static void drawEdges(BufferBuilder buf, Matrix4f mat,
                                   float x, float y, float z,
                                   float r, float g, float b) {
        float x1 = x, y1 = y, z1 = z;
        float x2 = x + 1, y2 = y + 1, z2 = z + 1;
        float a = 0.8f;

        // Bottom face
        edge(buf, mat, x1,y1,z1, x2,y1,z1, r,g,b,a);
        edge(buf, mat, x2,y1,z1, x2,y1,z2, r,g,b,a);
        edge(buf, mat, x2,y1,z2, x1,y1,z2, r,g,b,a);
        edge(buf, mat, x1,y1,z2, x1,y1,z1, r,g,b,a);
        // Top face
        edge(buf, mat, x1,y2,z1, x2,y2,z1, r,g,b,a);
        edge(buf, mat, x2,y2,z1, x2,y2,z2, r,g,b,a);
        edge(buf, mat, x2,y2,z2, x1,y2,z2, r,g,b,a);
        edge(buf, mat, x1,y2,z2, x1,y2,z1, r,g,b,a);
        // Verticals
        edge(buf, mat, x1,y1,z1, x1,y2,z1, r,g,b,a);
        edge(buf, mat, x2,y1,z1, x2,y2,z1, r,g,b,a);
        edge(buf, mat, x2,y1,z2, x2,y2,z2, r,g,b,a);
        edge(buf, mat, x1,y1,z2, x1,y2,z2, r,g,b,a);
    }

    private static void edge(BufferBuilder buf, Matrix4f mat,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float r, float g, float b, float a) {
        buf.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x2, y2, z2).setColor(r, g, b, a);
    }
}
