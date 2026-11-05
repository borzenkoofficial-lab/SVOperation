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

/** Client-only OBJ item renderer with lightweight client-side weapon action poses. */
@OnlyIn(Dist.CLIENT)
public final class SV01RifleRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation OBJ = ResourceLocation.fromNamespaceAndPath(SVOperation.MOD_ID, "models/obj/sv01.obj");
    private static final ResourceLocation MTL = ResourceLocation.fromNamespaceAndPath(SVOperation.MOD_ID, "models/obj/sv01.mtl");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SVOperation.MOD_ID, "textures/item/sv01.png");
    private static volatile ObjModel cachedModel;
    private static volatile long fireStartedAt;
    private static volatile long reloadStartedAt;

    public SV01RifleRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /** Called on the client when the use action is released and a shot is requested. */
    public static void triggerFire() {
        fireStartedAt = System.nanoTime();
    }

    /** Called on the client when a reload command is requested. */
    public static void triggerReload() {
        reloadStartedAt = System.nanoTime();
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ObjModel model = getModel();
        if (model == null) return;
        poseStack.pushPose();
        applyItemTransform(context, poseStack);
        applyActionPose(stack, context, poseStack);
        for (ObjModel.MeshPart part : model.parts()) {
            poseStack.pushPose();
            applyPartAnimation(part.name(), poseStack);
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            PoseStack.Pose pose = poseStack.last();
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
            poseStack.popPose();
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

    private static void applyActionPose(ItemStack stack, ItemDisplayContext context, PoseStack pose) {
        boolean handContext = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        if (!handContext) return;

        var player = Minecraft.getInstance().player;
        if (player != null && player.isUsingItem() && player.getUseItem() == stack) {
            pose.translate(0.0F, 0.018F, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(1.1F));
        }

        double fireAge = ageSeconds(fireStartedAt);
        if (fireAge >= 0.0 && fireAge < 0.22) {
            float kick = (float) (Math.sin(Math.PI * Math.min(1.0, fireAge / 0.22)) * Math.exp(-fireAge * 8.0));
            pose.translate(0.055F * kick, 0.0F, 0.075F * kick);
            pose.mulPose(Axis.ZP.rotationDegrees(-4.0F * kick));
        }

        double reloadAge = ageSeconds(reloadStartedAt);
        if (reloadAge >= 0.0 && reloadAge < 1.30) {
            float lower = (float) Math.sin(Math.PI * Math.min(1.0, reloadAge / 1.30));
            pose.translate(0.0F, -0.035F * lower, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(2.4F * lower));
        }
    }

    private static void applyPartAnimation(String partName, PoseStack pose) {
        double reloadAge = ageSeconds(reloadStartedAt);
        if ("magazine".equals(partName) && reloadAge >= 0.0 && reloadAge < 1.30) {
            float out = ease(reloadAge, 0.16, 0.38);
            float in = ease(reloadAge, 0.76, 1.02);
            float travel = Math.max(0.0F, out - in);
            pose.translate(-0.02F, -0.15F, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(-7.0F * travel));
            pose.translate(0.02F, 0.15F, 0.0F);
            pose.translate(0.0F, -0.43F * travel, 0.025F * travel);
        }

        double fireAge = ageSeconds(fireStartedAt);
        float bolt = 0.0F;
        if (fireAge >= 0.0 && fireAge < 0.18) {
            bolt = (float) (Math.sin(Math.PI * Math.min(1.0, fireAge / 0.18)) * Math.exp(-fireAge * 5.0));
        } else if (reloadAge >= 0.0 && reloadAge < 1.30) {
            bolt = ease(reloadAge, 0.96, 1.08) - ease(reloadAge, 1.12, 1.25);
        }
        if (("bolt_cover".equals(partName) || "charging_handle".equals(partName)) && bolt > 0.0F) {
            pose.translate(0.10F * bolt, 0.0F, 0.0F);
        }
    }

    private static float ease(double value, double start, double end) {
        if (value <= start) return 0.0F;
        if (value >= end) return 1.0F;
        float t = (float) ((value - start) / (end - start));
        return t * t * (3.0F - 2.0F * t);
    }

    private static double ageSeconds(long startedAt) {
        return startedAt == 0L ? -1.0 : (System.nanoTime() - startedAt) / 1_000_000_000.0;
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
