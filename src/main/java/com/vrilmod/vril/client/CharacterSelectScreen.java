package com.vrilmod.vril.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.vrilmod.vril.VrilMod;
import com.vrilmod.vril.character.CharacterType;
import com.vrilmod.vril.character.Characters;
import com.vrilmod.vril.network.ChooseCharacterPacket;
import com.vrilmod.vril.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Character selection. Cannot be dismissed with ESC.
 *
 * The whole screen is one image (textures/gui/character_select.png, 215x215).
 * The three buttons in it are baked in, and each is drawn again on top as a
 * separate full-size button texture (exact pixel copies, so they cover the baked
 * ones perfectly) which lets them react to the mouse: a hover texture swaps in
 * with a lighter outline, and a disabled button is dimmed.
 *
 *   Back button     top-left      button_small(.png / _hover.png)
 *   Forward button  top-right     button_small
 *   Choose button   bottom        button_wide
 *   name            under the portrait, between the two arrows
 *   bio + traits    the open middle area, scrollable (wheel, trackpad,
 *                   dragging the bar, or the arrow / page keys)
 *
 * All positions below are in the image's own 215x215 pixel space and get
 * scaled to fit whatever size the window is.
 */
public class CharacterSelectScreen extends Screen {
    private static final ResourceLocation BACKGROUND = tex("character_select");
    private static final ResourceLocation SMALL = tex("button_small");
    private static final ResourceLocation SMALL_HOVER = tex("button_small_hover");
    private static final ResourceLocation WIDE = tex("button_wide");
    private static final ResourceLocation WIDE_HOVER = tex("button_wide_hover");

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(VrilMod.MOD_ID, "textures/gui/" + name + ".png");
    }

    private static final int IMG = 215;

    // How much of the window's shorter side the screen takes up (1.0 = edge to edge).
    private static final float SCREEN_FILL = 0.8f;

    // Buttons in the image: {x, y, width, height}, outline included.
    private static final int[] BACK_BTN = {9, 9, 50, 22};
    private static final int[] FORWARD_BTN = {157, 9, 50, 22};
    private static final int[] CHOOSE_BTN = {9, 181, 121, 24};

    private static final int[] TEXT_AREA = {12, 50, 203, 178}; // x1, y1, x2, y2
    private static final int NAME_Y = 40;

    private static final int PAD = 4;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLL_STEP = 12;

    private final List<CharacterType> characters = Characters.ALL;
    private int index = 0; // opens on character 1

    private Button backButton;
    private Button forwardButton;
    private Button chooseButton;

    // Where the image sits on screen; set in init().
    private float scale = 1f;
    private int originX = 0;
    private int originY = 0;

    // Scrolling state. Geometry is refreshed every render.
    private double scroll = 0;
    private int maxScroll = 0;
    private int contentHeight = 0;
    private int viewHeight = 0;
    private int trackLeft, trackTop, trackHeight, thumbHeight;
    private boolean draggingBar = false;
    private double grabOffset = 0;

    public CharacterSelectScreen() {
        super(Component.translatable("screen.vril.select_character"));
    }

    @Override
    protected void init() {
        // Fit the square image into the window with a margin, centred.
        this.scale = Math.min(this.width, this.height) * SCREEN_FILL / IMG;
        this.originX = Math.round((this.width - IMG * scale) / 2f);
        this.originY = Math.round((this.height - IMG * scale) / 2f);

        this.backButton = this.addRenderableWidget(
                hitButton(BACK_BTN, Component.translatable("screen.vril.back"), () -> shift(-1)));
        this.forwardButton = this.addRenderableWidget(
                hitButton(FORWARD_BTN, Component.translatable("screen.vril.forward"), () -> shift(1)));
        this.chooseButton = this.addRenderableWidget(
                hitButton(CHOOSE_BTN, Component.translatable("screen.vril.choose"), this::choose));

        // Nothing to click through yet; they light up once a second character exists.
        boolean multiple = characters.size() > 1;
        this.backButton.active = multiple;
        this.forwardButton.active = multiple;
    }

    // Image-space to screen-space helpers.
    private int sx(int imgX) {
        return originX + Math.round(imgX * scale);
    }

    private int sy(int imgY) {
        return originY + Math.round(imgY * scale);
    }

    /** Click area = the whole button texture. */
    private Button hitButton(int[] r, Component label, Runnable action) {
        int x = sx(r[0]);
        int y = sy(r[1]);
        return new HitButton(x, y, sx(r[0] + r[2]) - x, sy(r[1] + r[3]) - y, label, b -> action.run());
    }

    private void shift(int delta) {
        index = Math.floorMod(index + delta, characters.size());
        scroll = 0;
        draggingBar = false;
    }

    private void choose() {
        ModNetwork.CHANNEL.sendToServer(new ChooseCharacterPacket(characters.get(index).id()));
        this.onClose();
    }

    // Text area geometry on screen.
    private int areaLeft() {
        return sx(TEXT_AREA[0]);
    }

    private int areaTop() {
        return sy(TEXT_AREA[1]);
    }

    private int areaRight() {
        return sx(TEXT_AREA[2]);
    }

    private int areaBottom() {
        return sy(TEXT_AREA[3]);
    }

    /** Positive effects first, then neutral, then negative; each group keeps its defined order. */
    private static List<CharacterType.Trait> orderedTraits(CharacterType c) {
        return c.traits().stream()
                .sorted(java.util.Comparator.comparingInt(t -> switch (t.kind()) {
                    case POSITIVE -> 0;
                    case NEUTRAL -> 1;
                    case NEGATIVE -> 2;
                }))
                .toList();
    }

    /** Draws one button texture over its baked twin: normal, hover (lighter outline) or dimmed. */
    private void drawButton(GuiGraphics g, int[] r, ResourceLocation normal, ResourceLocation hover,
                            Button b, int mouseX, int mouseY) {
        boolean hot = b.active && (b.isMouseOver(mouseX, mouseY) || b.isFocused());
        if (!b.active) {
            RenderSystem.setShaderColor(0.6f, 0.6f, 0.6f, 1f);
        }
        g.blit(hot ? hover : normal, r[0], r[1], 0, 0, r[2], r[3], r[2], r[3]);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        // The whole screen: the image, scaled to fit, with the button textures on top
        // in the same scaled space so they line up with the image's pixels.
        g.pose().pushPose();
        g.pose().translate(originX, originY, 0);
        g.pose().scale(scale, scale, 1f);
        g.blit(BACKGROUND, 0, 0, 0, 0, IMG, IMG, IMG, IMG);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        drawButton(g, BACK_BTN, SMALL, SMALL_HOVER, backButton, mouseX, mouseY);
        drawButton(g, FORWARD_BTN, SMALL, SMALL_HOVER, forwardButton, mouseX, mouseY);
        drawButton(g, CHOOSE_BTN, WIDE, WIDE_HOVER, chooseButton, mouseX, mouseY);
        g.pose().popPose();

        CharacterType c = characters.get(index);

        // name, under the portrait baked into the image
        Component name = Component.translatable("character.vril." + c.id());
        g.drawCenteredString(this.font, name, originX + Math.round(IMG * scale / 2f), sy(NAME_Y), 0xFFFFFF);

        // bio + traits inside the open area
        int left = areaLeft();
        int right = areaRight();
        int top = areaTop();
        int bottom = areaBottom();

        int textLeft = left + PAD;
        int textWidth = right - left - PAD * 2 - SCROLLBAR_WIDTH - 2;
        viewHeight = bottom - top - PAD * 2;

        // Lay out the content to measure it, drawing only what falls inside the area.
        g.enableScissor(left, top, right, bottom);
        int y = top + PAD - (int) Math.round(scroll);
        int startY = y;

        Component bio = Component.translatable("character.vril." + c.id() + ".bio");
        for (FormattedCharSequence line : this.font.split(bio, textWidth)) {
            g.drawString(this.font, line, textLeft, y, 0xE8E8E8, true);
            y += 10;
        }
        y += 8;

        for (CharacterType.Trait trait : orderedTraits(c)) {
            Component traitName = Component.translatable("trait.vril." + trait.id() + ".name")
                    .withStyle(trait.kind().color);
            g.drawString(this.font, traitName, textLeft, y, 0xFFFFFF, true);
            y += 10;

            Component desc = Component.translatable("trait.vril." + trait.id() + ".desc");
            for (FormattedCharSequence line : this.font.split(desc, textWidth)) {
                g.drawString(this.font, line, textLeft, y, 0xC4C4C4, true);
                y += 10;
            }
            y += 6;
        }
        g.disableScissor();

        contentHeight = y - startY;
        maxScroll = Math.max(0, contentHeight - viewHeight);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        renderScrollbar(g, mouseX, mouseY, right, top, bottom);

        super.render(g, mouseX, mouseY, partialTick);
    }

    /**
     * Minecraft-style scrollbar (same look as the vanilla list scrollbar: dark track,
     * grey thumb with a light top-left bevel), but semi-transparent. Only shown when
     * there is something to scroll.
     */
    private void renderScrollbar(GuiGraphics g, int mouseX, int mouseY, int right, int top, int bottom) {
        if (maxScroll <= 0) {
            draggingBar = false;
            trackHeight = 0;
            return;
        }
        trackLeft = right - SCROLLBAR_WIDTH - 2;
        trackTop = top + 2;
        trackHeight = bottom - top - 4;
        thumbHeight = Math.max(16, Math.min(trackHeight, trackHeight * viewHeight / contentHeight));
        int thumbTop = thumbTop();

        boolean hot = draggingBar || overTrack(mouseX, mouseY);
        g.fill(trackLeft, trackTop, trackLeft + SCROLLBAR_WIDTH, trackTop + trackHeight, 0x80000000);
        int shade = hot ? 0xB0909090 : 0x90808080;
        int light = hot ? 0xB0D0D0D0 : 0x90C0C0C0;
        g.fill(trackLeft, thumbTop, trackLeft + SCROLLBAR_WIDTH, thumbTop + thumbHeight, shade);
        g.fill(trackLeft, thumbTop, trackLeft + SCROLLBAR_WIDTH - 1, thumbTop + thumbHeight - 1, light);
    }

    private int thumbTop() {
        if (maxScroll <= 0) {
            return trackTop;
        }
        return trackTop + (int) ((trackHeight - thumbHeight) * (scroll / maxScroll));
    }

    private boolean overTrack(double mx, double my) {
        return maxScroll > 0 && trackHeight > 0
                && mx >= trackLeft - 2 && mx < trackLeft + SCROLLBAR_WIDTH + 2
                && my >= trackTop && my < trackTop + trackHeight;
    }

    private void dragTo(double my) {
        double range = trackHeight - thumbHeight;
        if (range <= 0) {
            return;
        }
        double frac = (my - trackTop - grabOffset) / range;
        scroll = Math.max(0, Math.min(maxScroll, frac * maxScroll));
    }

    private void scrollBy(double amount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll + amount));
    }

    /** Click on the thumb to grab it, or on the track to jump there and keep dragging. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overTrack(mouseX, mouseY)) {
            int top = thumbTop();
            if (mouseY >= top && mouseY < top + thumbHeight) {
                grabOffset = mouseY - top;
            } else {
                grabOffset = thumbHeight / 2.0;
                dragTo(mouseY);
            }
            draggingBar = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingBar && button == 0) {
            dragTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingBar && button == 0) {
            draggingBar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Mouse wheel and trackpad. Trackpads send many small fractional deltas, so the
     * amount is applied as-is rather than in whole notches, and it works anywhere on
     * the screen since the bio is the only thing that scrolls.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll > 0) {
            scrollBy(-delta * SCROLL_STEP);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_DOWN -> scrollBy(SCROLL_STEP);
            case GLFW.GLFW_KEY_UP -> scrollBy(-SCROLL_STEP);
            case GLFW.GLFW_KEY_PAGE_DOWN -> scrollBy(viewHeight);
            case GLFW.GLFW_KEY_PAGE_UP -> scrollBy(-viewHeight);
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * A button with no art of its own: the screen draws the button textures, so this
     * only adds the label and the click area.
     */
    private static class HitButton extends Button {
        HitButton(int x, int y, int w, int h, Component label, OnPress onPress) {
            super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean hot = this.active && this.isHoveredOrFocused();
            int color = !this.active ? 0x777777 : hot ? 0xFFFFA0 : 0xFFFFFF;
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                    getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, color);
        }
    }
}
