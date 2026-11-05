package ru.svoperation.model;

import net.minecraft.resources.ResourceLocation;
import ru.svoperation.SVOperation;
import ru.svoperation.item.SV01RifleItem;
import software.bernie.geckolib.model.GeoModel;

public final class SV01RifleModel extends GeoModel<SV01RifleItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(SVOperation.MOD_ID, "geo/sv01.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SVOperation.MOD_ID, "textures/item/sv01.png");
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(SVOperation.MOD_ID, "animations/sv01.animation.json");
    @Override public ResourceLocation getModelLocation(SV01RifleItem item) { return MODEL; }
    @Override public ResourceLocation getTextureLocation(SV01RifleItem item) { return TEXTURE; }
    @Override public ResourceLocation getAnimationFileLocation(SV01RifleItem item) { return ANIMATIONS; }
}
