package ru.svoperation;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SVOperation.MOD_ID)
public final class SVOperation {
    public static final String MOD_ID = "svoperation";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SVOperation() {
        LOGGER.info("SVOperation {} initialized", "0.1.0");
    }
}
