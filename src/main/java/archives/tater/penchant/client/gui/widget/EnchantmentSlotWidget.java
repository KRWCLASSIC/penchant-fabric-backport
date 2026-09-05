package archives.tater.penchant.client.gui.widget;

import archives.tater.penchant.Penchant;
import archives.tater.penchant.util.PenchantmentHelper;

import cc.cassian.item_descriptions.client.DescriptionKey;
import cc.cassian.item_descriptions.client.ModClient;
import cc.cassian.item_descriptions.client.helpers.ModStyle;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EnchantmentSlotWidget extends AbstractButton {
    public static final int WIDTH = 131;
    public static final int HEIGHT = 12;

    private static final ResourceLocation SLOT = Penchant.id("textures/gui/sprites/container/enchanting_table/slot.png");
    private static final ResourceLocation SLOT_DISABLED = Penchant.id("textures/gui/sprites/container/enchanting_table/slot_disabled.png");
    private static final ResourceLocation SLOT_HIGHLIGHTED = Penchant.id("textures/gui/sprites/container/enchanting_table/slot_highlighted.png");
    private static final ResourceLocation ALT_FONT = new ResourceLocation("minecraft", "alt");

    public static final int DISABLED_COLOR = 0xFF685E4A;
    public static final int INSUFFICIENT_COLOR = 0xffff5555;
    public static final int XP_COLOR = 0xFF80FF20;
    public static final int BOOK_COLOR = 0xFFFFAA00;

    private final Enchantment enchantment;
    private final Component text;
    private final @Nullable Component costText;
    private final boolean isCurse;
    private final Runnable onClick;

    private EnchantmentSlotWidget(
            int x, int y, Enchantment enchantment, List<Enchantment> incompatible, boolean remove,
            boolean showXpCost, boolean showBookCost, boolean canUse, boolean alreadyAdded,
            boolean hasIngredient, boolean hasEnoughBooks, boolean hasEnoughXp, boolean isUnlocked,
            Runnable onClick
    ) {
        super(x, y, WIDTH, HEIGHT, Component.translatable(enchantment.getDescriptionId()));
        this.enchantment = enchantment;
        this.onClick = onClick;
        this.isCurse = enchantment.isCurse();

        MutableComponent label = Component.translatable(enchantment.getDescriptionId());
        if (!isUnlocked && !alreadyAdded) {
            label.withStyle(style -> style.withFont(ALT_FONT));
        }
        this.text = label;

        int xpCost = PenchantmentHelper.getXpLevelCost(enchantment);
        int bookRequirement = PenchantmentHelper.getBookRequirement(enchantment);

        List<Component> costTexts = new ArrayList<>(3);
        if (!canUse && !incompatible.isEmpty()) {
            costTexts.add(Component.translatable("widget.penchant.enchantment_slot.incompatible").withStyle(style -> style.withColor(INSUFFICIENT_COLOR)));
        }
        if (showBookCost) {
            costTexts.add(Component.literal(Integer.toString(bookRequirement))
                    .withStyle(style -> style.withColor(alreadyAdded ? DISABLED_COLOR : !hasEnoughBooks ? INSUFFICIENT_COLOR : BOOK_COLOR)));
        }
        if (showXpCost) {
            costTexts.add(Component.literal(Integer.toString(xpCost))
                    .withStyle(style -> style.withColor(alreadyAdded ? DISABLED_COLOR : !hasEnoughXp ? INSUFFICIENT_COLOR : XP_COLOR)));
        }
        this.costText = costTexts.isEmpty() ? null : ComponentUtils.formatList(costTexts, Component.literal(" "));

        if (alreadyAdded) {
            setTooltip(Tooltip.create(Component.empty()
                    .append(PenchantmentHelper.getName(enchantment))
                    .append("\n")
                    .append(Component.translatable("widget.penchant.enchantment_slot.tooltip.added").withStyle(ChatFormatting.GRAY))));
        } else if (!isUnlocked) {
            String name = Component.translatable(enchantment.getDescriptionId()).getString();
            int cut = Math.max(1, name.length() * 2 / 3);
            setTooltip(Tooltip.create(Component.literal(name.substring(0, cut).trim())
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable("widget.penchant.enchantment_slot.tooltip.name_locked"))
                    .append("\n")
                    .append(Component.translatable("widget.penchant.enchantment_slot.tooltip.locked").withStyle(ChatFormatting.RED))));
        } else {
            MutableComponent tooltip = remove
                    ? Component.translatable("widget.penchant.enchantment_slot.tooltip.remove", Component.translatable(enchantment.getDescriptionId()))
                    : PenchantmentHelper.getName(enchantment).copy();

            if (!canUse && !incompatible.isEmpty()) {
                tooltip.append("\n").append(Component.translatable(
                        "widget.penchant.enchantment_slot.tooltip.incompatible",
                        ComponentUtils.formatList(incompatible, Component.literal(", "), e -> Component.translatable(e.getDescriptionId()))
                ).withStyle(ChatFormatting.RED));
            }
            if (showBookCost) {
                tooltip.append("\n").append(Component.translatable("widget.penchant.enchantment_slot.tooltip.book_requirement", bookRequirement)
                        .withStyle(style -> style.withColor(hasEnoughBooks ? BOOK_COLOR : INSUFFICIENT_COLOR)));
            }
            if (showXpCost) {
                tooltip.append("\n").append(Component.translatable("widget.penchant.enchantment_slot.tooltip.xp_cost", xpCost)
                        .withStyle(style -> style.withColor(hasEnoughXp ? XP_COLOR : INSUFFICIENT_COLOR)));
            }
            if (PenchantmentHelper.ITEM_DESCRIPTIONS_INSTALLED) {
                try {
                    addItemDescription(tooltip, enchantment);
                } catch (Throwable ignored) {}
            }
            setTooltip(Tooltip.create(tooltip));
        }

        active = hasIngredient && hasEnoughBooks && hasEnoughXp && isUnlocked && canUse && !alreadyAdded;
    }

    private void addItemDescription(MutableComponent tooltip, Enchantment enchantment) {
        if (ModClient.CONFIG.enchantmentDescriptions.enable.value()
                && ModClient.CONFIG.enchantmentDescriptions.enchantingTable.value()) {
            DescriptionKey key = new DescriptionKey(enchantment.getDescriptionId());
            if (key.hasTranslation()) {
                tooltip.append("\n").append(key.toText().withStyle(ModStyle.ENCHANTMENT_DESCRIPTIONS));
            }
        }
    }

    public EnchantmentSlotWidget(
            int x, int y, Enchantment enchantment, List<Enchantment> incompatible,
            boolean canAdd, boolean alreadyAdded, boolean hasIngredient, boolean hasEnoughBooks,
            boolean hasEnoughXp, boolean isUnlocked, Runnable onClick
    ) {
        this(x, y, enchantment, isUnlocked ? incompatible : List.of(), false, isUnlocked, isUnlocked,
                canAdd, alreadyAdded, hasIngredient, hasEnoughBooks, hasEnoughXp, isUnlocked, onClick);
    }

    public EnchantmentSlotWidget(
            int x, int y, Enchantment enchantment, List<Enchantment> incompatible,
            boolean canRemove, boolean hasEnoughBooks, Runnable onClick
    ) {
        this(x, y, enchantment, incompatible, true, false, true, canRemove, false, true, hasEnoughBooks, true, true, onClick);
    }

    public Enchantment getEnchantment() {
        return enchantment;
    }

    @Override
    public void onPress() {
        onClick.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation texture = !active ? SLOT_DISABLED : (isHoveredOrFocused() ? SLOT_HIGHLIGHTED : SLOT);
        graphics.blit(texture, getX(), getY(), 0, 0, width, height, width, height);

        var font = Minecraft.getInstance().font;
        int color = active && isHoveredOrFocused() ? 0xFFFCFC7E : isCurse ? 0xFF891D13 : 0xFF332E25;
        graphics.drawString(font, text, getX() + 2, getY() + 2, color, false);
        if (costText != null) {
            graphics.drawString(font, costText, getX() + width - 2 - font.width(costText), getY() + 2, 0xFF404040, false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
