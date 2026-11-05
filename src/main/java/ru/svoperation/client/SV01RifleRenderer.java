package ru.svoperation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ru.svoperation.SVOperation;
import ru.svoperation.model.ObjModel;
import ru.svoperation.model.ObjModelLoader;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Client-only OBJ item renderer. Material Kd values tint the shared albedo texture. */
@OnlyIn(Dist.CLIENT)
public final class SV01RifleRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation OBJ = ResourceLocation.fromNamespaceAndPath(SVOperation.MOD_ID, "models/obj/sv01.obj");
    private static final ResourceLocation MTL = ResourceLocation.fromNamespaceAndPath(SVOperation.MOD_ID, "models/obj/sv01.mtl");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SVOperation.MOD_ID, "textures/item/sv01.png");
    private static volatile ObjModel cachedModel;

    public SV01RifleRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ObjModel model = getModel();
        if (model == null) return;
        poseStack.pushPose();
        applyItemTransform(context, poseStack);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        for (ObjModel.MeshPart part : model.parts()) {
            ObjModel.Material material = model.materials().get(part.material());
            int color = material == null ? fallbackColor(part.material()) : material.argb();
            int end = part.firstVertex() + part.vertexCount();
            for (int i = part.firstVertex(); i < end; i++) {
                ObjModel.Vertex vertex = model.vertices().get(i);
                consumer.addVertex(pose, vertex.x(), vertex.y(), vertex.z())
                        .setColor(color).setUv(vertex.u(), vertex.v())
                        .setOverlay(packedOverlay).setLight(packedLight)
                        .setNormal(pose, vertex.nx(), vertex.ny(), vertex.nz());
            }
        }
        poseStack.popPose();
    }

    private static void applyItemTransform(ItemDisplayContext context, PoseStack pose) {
        switch (context) {
            case GUI -> {
                pose.translate(0.5F, 0.48F, 0.5F);
                pose.mulPose(Axis.YP.rotationDegrees(135));
                pose.mulPose(Axis.XP.rotationDegrees(18));
                pose.scale(0.19F, 0.19F, 0.19F);
            }
            case GROUND, FIXED -> {
                pose.translate(0.5F, 0.28F, 0.5F);
                pose.mulPose(Axis.YP.rotationDegrees(90));
                pose.scale(0.20F, 0.20F, 0.20F);
            }
            default -> {
                pose.translate(0.5F, 0.38F, 0.5F);
                pose.mulPose(Axis.YP.rotationDegrees(90));
                pose.scale(0.22F, 0.22F, 0.22F);
            }
        }
        pose.translate(0.15F, 0.0F, 0.0F);
    }

    private static int fallbackColor(String material) {
        return switch (material) {
            case "polymer" -> 0xFFB8B8B8;
            case "detail" -> 0xFFD0D0D0;
            default -> 0xFFFFFFFF;
        };
    }

    private static ObjModel getModel() {
        ObjModel model = cachedModel;
        if (model != null) return model;
        synchronized (SV01RifleRenderer.class) {
            if (cachedModel != null) return cachedModel;
            var objResource = Minecraft.getInstance().getResourceManager().getResource(OBJ).orElseThrow();
            var mtlResource = Minecraft.getInstance().getResourceManager().getResource(MTL).orElseThrow();
            try (var objStream = objResource.open();
                 var mtlStream = mtlResource.open();
                 var objReader = new InputStreamReader(objStream, StandardCharsets.UTF_8);
                 var mtlReader = new InputStreamReader(mtlStream, StandardCharsets.UTF_8)) {
                cachedModel = ObjModelLoader.parse(objReader, mtlReader);
                SVOperation.LOGGER.info("Loaded SV-01 OBJ mesh: {} vertices, {} parts, {} materials",
                        cachedModel.vertices().size(), cachedModel.parts().size(), cachedModel.materials().size());
                return cachedModel;
            } catch (Exception exception) {
                SVOperation.LOGGER.error("Unable to load SV-01 OBJ/MTL model", exception);
                return null;
            }
        }
    }
}
