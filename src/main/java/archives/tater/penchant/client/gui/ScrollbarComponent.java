package archives.tater.penchant.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import static archives.tater.penchant.client.gui.PenchantGuiUtil.containsPoint;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static net.minecraft.util.Mth.clamp;

public class ScrollbarComponent {
    private final ResourceLocation texture;
    private final int width;
    private final int scrollerHeight;
    private final int trackHeight;
    private final int regionWidth;
    private final int regionHeight;
    private final Runnable onScrolled;

    private final int maxScrollerYOffset;

    private int x;
    private int y;
    private int regionX;
    private int regionY;
    private int maxStep;

    private int position = 0;

    private boolean dragging = false;
    private double mouseYOffset = 0;

    public ScrollbarComponent(ResourceLocation texture, int width, int height, int trackHeight, int regionWidth, int regionHeight, Runnable onScrolled) {
        this.texture = texture;
        this.width = width;
        this.scrollerHeight = height;
        this.trackHeight = trackHeight;
        this.regionWidth = regionWidth;
        this.regionHeight = regionHeight;
        this.onScrolled = onScrolled;
        maxScrollerYOffset = trackHeight - scrollerHeight;
    }

    public void update(int x, int y, int regionX, int regionY, int stepCount) {
        this.x = x;
        this.y = y;
        this.regionX = regionX;
        this.regionY = regionY;
        this.maxStep = max(stepCount, 0);
        position = clamp(position, 0, maxStep);
    }

    public boolean canScroll() {
        return maxStep > 0;
    }

    private int getScrollerYOffset() {
        return canScroll() ? clamp(maxScrollerYOffset * position / maxStep, 0, maxScrollerYOffset) : 0;
    }

    private void setPositionForOffset(double yOffset) {
        setPosition(round((float) (maxStep * yOffset / maxScrollerYOffset)));
    }

    private int getScrollerY() {
        return y + getScrollerYOffset();
    }

    public void render(GuiGraphics graphics) {
        if (!canScroll()) return;
        graphics.blit(texture, x, getScrollerY(), 0, 0, width, scrollerHeight, width, scrollerHeight);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        if (!containsPoint(x, y, width, trackHeight, mouseX, mouseY)
                && !containsPoint(regionX, regionY, regionWidth, regionHeight, mouseX, mouseY)) {
            return false;
        }
        if (scrollAmount >= 0 && position <= 0 || scrollAmount <= 0 && position >= maxStep) return false;
        setPosition(position - (int) Math.signum(scrollAmount));
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !containsPoint(x, y, width, trackHeight, mouseX, mouseY)) return false;
        if (containsPoint(x, getScrollerY(), width, scrollerHeight, mouseX, mouseY)) {
            dragging = true;
            mouseYOffset = mouseY - getScrollerY();
        } else {
            setPositionForOffset(mouseY - y);
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!dragging || button != 0) return false;
        setPositionForOffset(mouseY - y - mouseYOffset);
        return true;
    }

    public void mouseReleased() {
        dragging = false;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        position = clamp(position, 0, maxStep);
        if (position == this.position) return;
        this.position = position;
        onScrolled.run();
    }

    public void reset() {
        if (position == 0) return;
        position = 0;
        onScrolled.run();
    }
}
