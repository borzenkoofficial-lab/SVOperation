package ru.svoperation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import ru.svoperation.SVOperation;
import ru.svoperation.entity.GeranEntity;
import ru.svoperation.model.ObjModel;
import ru.svoperation.model.ObjModelLoader;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class GeranRenderer extends EntityRenderer<GeranEntity> {
    private static final ResourceLocation OBJ = ResourceLocation.fromNamespaceAndPath(SVOperation.MOD_ID, "models/obj/geran.obj");
    private static final ResourceLocation MTL = ResourceLocation.fromNamespaceAndPath(SVOperation.MOD_ID, "models/obj/geran.mtl");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SVOperation.MOD_ID, "textures/item/sv01.png");
    private static volatile ObjModel cachedModel;

    public GeranRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.55F;
    }

    @Override
    public void render(GeranEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        ObjModel model = getModel();
        if (model != null) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.12D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(0.72F, 0.72F, 0.72F);
            for (ObjModel.MeshPart part : model.parts()) {
                ObjModel.Material material = model.materials().get(part.material());
                int color = material == null ? 0xFFFFFFFF : material.argb();
                VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
                PoseStack.Pose pose = poseStack.last();
                int end = part.firstVertex() + part.vertexCount();
                for (int i = part.firstVertex(); i < end; i++) {
                    ObjModel.Vertex vertex = model.vertices().get(i);
                    consumer.addVertex(pose, vertex.x(), vertex.y(), vertex.z())
                            .setColor(color).setUv(vertex.u(), vertex.v())
                            .setOverlay(0).setLight(packedLight)
                            .setNormal(pose, vertex.nx(), vertex.ny(), vertex.nz());
                }
            }
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GeranEntity entity) {
        return TEXTURE;
    }

    private static ObjModel getModel() {
        if (cachedModel != null) return cachedModel;
        synchronized (GeranRenderer.class) {
            if (cachedModel != null) return cachedModel;
            try {
                var resources = Minecraft.getInstance().getResourceManager();
                var obj = resources.getResource(OBJ).orElseThrow();
                var mtl = resources.getResource(MTL).orElseThrow();
                try (var objStream = obj.open(); var mtlStream = mtl.open();
                     var objReader = new InputStreamReader(objStream, StandardCharsets.UTF_8);
                     var mtlReader = new InputStreamReader(mtlStream, StandardCharsets.UTF_8)) {
                    cachedModel = ObjModelLoader.parse(objReader, mtlReader);
                }
            } catch (Exception exception) {
                SVOperation.LOGGER.error("Unable to load Geran OBJ/MTL model", exception);
            }
            return cachedModel;
        }
    }
}
