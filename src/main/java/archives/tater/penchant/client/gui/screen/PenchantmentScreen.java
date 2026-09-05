package archives.tater.penchant.client.gui.screen;

import archives.tater.penchant.Penchant;
import archives.tater.penchant.client.gui.ScrollbarComponent;
import archives.tater.penchant.client.gui.widget.EnchantmentSlotWidget;
import archives.tater.penchant.menu.PenchantmentMenu;
import archives.tater.penchant.util.PenchantmentHelper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static net.minecraft.util.Mth.clamp;
import static net.minecraft.util.Mth.lerp;

public class PenchantmentScreen extends AbstractContainerScreen<PenchantmentMenu> {
    private static final ResourceLocation TEXTURE = Penchant.id("textures/gui/container/enchanting_table.png");
    private static final ResourceLocation BOOK_TEXTURE = new ResourceLocation("textures/entity/enchanting_table_book.png");
    private static final ResourceLocation SCROLLER_TEXTURE = Penchant.id("textures/gui/sprites/container/enchanting_table/scroller.png");
    private static final ResourceLocation BOOK_ICON = Penchant.id("textures/gui/sprites/container/enchanting_table/book.png");
    private static final ResourceLocation GRINDSTONE_ICON = Penchant.id("textures/gui/sprites/container/enchanting_table/grindstone.png");
    private static final ResourceLocation LAPIS_SLOT = new ResourceLocation("item/empty_slot_lapis_lazuli");
    private static final ResourceLocation BOOK_SLOT = Penchant.id("item/empty_slot_book");
    private static final List<ResourceLocation> INGREDIENT_SLOT_TEXTURES = List.of(LAPIS_SLOT, BOOK_SLOT);
    private static final List<ResourceLocation> INGREDIENT_SLOT_TEXTURES_NO_DISENCHANT = List.of(LAPIS_SLOT);
    private static final int INFO_ICON_SIZE = 8;

    private static final Component ENCHANTING_SLOT_TOOLTIP = Component.translatable("container.penchant.enchant.slot.enchant");
    private static final Component INGREDIENT_SLOT_TOOLTIP = Component.translatable("container.penchant.enchant.slot.ingredient");
    private static final Component INGREDIENT_SLOT_DISENCHANT_TOOLTIP = Component.translatable("container.penchant.enchant.slot.ingredient.disenchant");
    private static final int TOOLTIP_WIDTH = 115;

    private final ScrollbarComponent scrollbar = new ScrollbarComponent(
            SCROLLER_TEXTURE, 6, 19, 60, EnchantmentSlotWidget.WIDTH + 1, 60, this::rebuildEnchantmentButtons
    );
    private final CyclingSlotBackground secondSlotBackground = new CyclingSlotBackground(1);
    private final RandomSource random = RandomSource.create();
    private final List<EnchantmentSlotWidget> enchantmentButtons = new ArrayList<>();

    private @Nullable BookModel bookModel;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    private ItemStack last = ItemStack.EMPTY;

    public PenchantmentScreen(PenchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 206;
        imageHeight = 172;
        inventoryLabelX = 23;
        inventoryLabelY = imageHeight - 94;
        menu.setSlotChangeListener(this::rebuildEnchantmentButtons);
    }

    @Override
    protected void init() {
        super.init();
        bookModel = new BookModel(minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));
        rebuildEnchantmentButtons();
    }

    private void rebuildEnchantmentButtons() {
        enchantmentButtons.forEach(this::removeWidget);
        enchantmentButtons.clear();

        List<Enchantment> displayed = menu.getDisplayedEnchantments();
        ItemStack stack = menu.getEnchantingStack();
        scrollbar.update(leftPos + 192, topPos + 14, leftPos + 60, topPos + 14, displayed.size() - 5);

        if (!menu.isEnchanting() && !menu.isDisenchanting()) return;

        boolean creative = requireNonNull(minecraft.player).getAbilities().instabuild;
        boolean disenchanting = menu.isDisenchanting();

        for (int i = 0; i < 5; i++) {
            int index = scrollbar.getPosition() + i;
            if (index >= displayed.size()) break;
            Enchantment enchantment = displayed.get(index);

            EnchantmentSlotWidget button;
            if (disenchanting) {
                button = new EnchantmentSlotWidget(
                        leftPos + 60, topPos + 14 + i * EnchantmentSlotWidget.HEIGHT,
                        enchantment,
                        getIncompatible(stack, enchantment),
                        true,
                        creative || PenchantmentHelper.getBookRequirement(enchantment) <= menu.getBookCount(),
                        () -> menu.clientSelect(enchantment)
                );
            } else {
                boolean hasLapis = creative || PenchantmentMenu.isEnchantingIngredient(menu.getIngredientStack());
                button = new EnchantmentSlotWidget(
                        leftPos + 60, topPos + 14 + i * EnchantmentSlotWidget.HEIGHT,
                        enchantment,
                        getIncompatible(stack, enchantment),
                        PenchantmentHelper.canEnchant(stack, enchantment),
                        PenchantmentHelper.hasEnchantment(stack, enchantment),
                        hasLapis,
                        creative || PenchantmentHelper.getBookRequirement(enchantment) <= menu.getBookCount(),
                        creative || PenchantmentHelper.getXpLevelCost(enchantment) <= menu.getPlayerXp(),
                        creative || menu.isAvailable(enchantment),
                        () -> menu.clientSelect(enchantment)
                );
            }
            enchantmentButtons.add(button);
            addRenderableWidget(button);
        }
    }

    private List<Enchantment> getIncompatible(ItemStack stack, Enchantment enchantment) {
        if (PenchantmentHelper.hasEnchantment(stack, enchantment)) return List.of();
        return PenchantmentHelper.getEnchantments(stack).keySet().stream()
                .filter(other -> !enchantment.equals(other) && !PenchantmentHelper.areCompatible(enchantment, other))
                .toList();
    }

    @Override
    protected void containerTick() {
        secondSlotBackground.tick(menu.canDisenchant() ? INGREDIENT_SLOT_TEXTURES : INGREDIENT_SLOT_TEXTURES_NO_DISENCHANT);
        tickBook();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return scrollbar.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return scrollbar.mouseDragged(mouseX, mouseY, button) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrollbar.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return scrollbar.mouseScrolled(mouseX, mouseY, delta) || super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        renderBook(graphics, leftPos, topPos, partialTick);

        var font = Minecraft.getInstance().font;
        String bookCount = Integer.toString(menu.getBookCount());
        String status = menu.hasDisenchanter() ? "✔" : "❌";
        int gap = font.width(" ");
        int thin = 2;
        int infoWidth = INFO_ICON_SIZE + thin + font.width(bookCount) + gap + INFO_ICON_SIZE + thin + font.width(status);
        int x = leftPos + 32 - infoWidth / 2;
        int y = topPos + 18;
        float gray = 0x60 / 255f;
        graphics.setColor(gray, gray, gray, 1f);
        graphics.blit(BOOK_ICON, x, y, 0, 0, INFO_ICON_SIZE, INFO_ICON_SIZE, INFO_ICON_SIZE, INFO_ICON_SIZE);
        x += INFO_ICON_SIZE + thin;
        graphics.setColor(1f, 1f, 1f, 1f);
        graphics.drawString(font, bookCount, x, y, 0xFF606060, false);
        x += font.width(bookCount) + gap;
        graphics.setColor(gray, gray, gray, 1f);
        graphics.blit(GRINDSTONE_ICON, x, y, 0, 0, INFO_ICON_SIZE, INFO_ICON_SIZE, INFO_ICON_SIZE, INFO_ICON_SIZE);
        x += INFO_ICON_SIZE + thin;
        graphics.setColor(1f, 1f, 1f, 1f);
        graphics.drawString(font, status, x, y, 0xFF606060, false);

        secondSlotBackground.render(menu, graphics, partialTick, leftPos, topPos);
        scrollbar.render(graphics);
    }

    private void renderBook(GuiGraphics guiGraphics, int x, int y, float partialTick) {
        float open = lerp(partialTick, oOpen, this.open);
        float flip = lerp(partialTick, oFlip, this.flip);
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(x + 14 + 19, y + 14 + 16, 50.0F);
        float f2 = 1.0F - open;
        f2 = 1.0F - f2 * f2 * f2;
        f2 *= 90.0F / (flip + 90.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F - f2 * 20.0F));
        float f3 = lerp(partialTick, oFlip, this.flip);
        poseStack.translate(1.0F, (f3 + 0.25F) * 0.1F, 0.0F);
        float f4 = 1.0F - (f3 + 0.75F) * 0.1F;
        poseStack.scale(f4, f4, f4);
        float f5 = -(f3 + 0.2F) * 20.0F;
        poseStack.mulPose(Axis.XP.rotationDegrees(f5));
        poseStack.mulPose(Axis.YP.rotationDegrees(80.0F));
        requireNonNull(bookModel).setupAnim(0.0F, f3, Mth.clamp(open, 0.0F, 1.0F), 1.0F);
        var bufferSource = guiGraphics.bufferSource();
        bookModel.render(poseStack, bufferSource.getBuffer(RenderType.entitySolid(BOOK_TEXTURE)), 15728880, 0, 1f, 1f, 1f, 1f);
        bufferSource.endBatch();
        poseStack.popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index <= 1) {
            graphics.renderTooltip(font, font.split(
                    hoveredSlot.index == 0
                            ? ENCHANTING_SLOT_TOOLTIP
                            : menu.canDisenchant() ? INGREDIENT_SLOT_DISENCHANT_TOOLTIP : INGREDIENT_SLOT_TOOLTIP,
                    TOOLTIP_WIDTH), mouseX, mouseY);
        }
    }

    public void tickBook() {
        ItemStack stack = menu.getEnchantingStack();
        if (!ItemStack.matches(stack, last)) {
            last = stack.copy();
            do {
                flipT += random.nextInt(4) - random.nextInt(4);
            } while (flip <= flipT + 1.0F && flip >= flipT - 1.0F);
        }
        oFlip = flip;
        oOpen = open;
        open = clamp(open + (!menu.getDisplayedEnchantments().isEmpty() ? 0.2F : -0.2F), 0.0F, 1.0F);
        float f = clamp((flipT - flip) * 0.4F, -0.2F, 0.2F);
        flipA += (f - flipA) * 0.9F;
        flip += flipA;
    }
}
