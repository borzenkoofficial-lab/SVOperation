package ru.svoperation.client;

import ru.svoperation.item.SV01RifleItem;
import ru.svoperation.model.SV01RifleModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class SV01RifleRenderer extends GeoItemRenderer<SV01RifleItem> {
    public SV01RifleRenderer() { super(new SV01RifleModel()); }
}
