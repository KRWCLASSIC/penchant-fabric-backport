package archives.tater.penchant.client;

import archives.tater.penchant.Penchant;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class FontUtils {
    private FontUtils() {}

    public static final String BAR_SEGMENT = "!";
    public static final String THIN_SPACE = "\u2009";

    public static final ResourceLocation FONT = Penchant.id("bar");
    public static final ResourceLocation FONT_TEXTURE = Penchant.id("textures/font/bar.png");

    public static final Component THIN_SPACE_TEXT = Component.literal(THIN_SPACE).withStyle(FontUtils::setCustomFont);

    private static Style setCustomFont(Style style) {
        return style.withFont(FONT);
    }

    public static MutableComponent getBar(int width, int progress) {
        int filled = Mth.clamp(progress, 0, width);
        int empty = width - filled;
        MutableComponent bar = Component.empty();
        if (filled > 0) {
            bar.append(Component.literal(BAR_SEGMENT.repeat(filled))
                    .withStyle(style -> style.withFont(FONT).withColor(ChatFormatting.LIGHT_PURPLE)));
        }
        if (empty > 0) {
            bar.append(Component.literal(BAR_SEGMENT.repeat(empty))
                    .withStyle(style -> style.withFont(FONT).withColor(ChatFormatting.DARK_GRAY)));
        }
        return bar;
    }
}
