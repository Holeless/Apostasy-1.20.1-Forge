package com.vrilmod.vril.client;

import com.vrilmod.vril.character.CharacterType;
import com.vrilmod.vril.character.Characters;
import com.vrilmod.vril.network.ChooseCharacterPacket;
import com.vrilmod.vril.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Character selection. Cannot be dismissed with ESC.
 *
 * Layout: Back button top-left, Forward button top-right, the character's
 * portrait top-centre, a scrollable bio area in the open middle, and a wide
 * Choose button along the bottom. Back/Forward cycle through
 * {@link Characters#ALL}; the screen opens on character 1.
 */
public class CharacterSelectScreen extends Screen {
    private static final int MARGIN = 10;
    private static final int NAV_WIDTH = 70;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PORTRAIT_SIZE = 40;
    private static final int PAD = 8;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLL_STEP = 12;

    private final List<CharacterType> characters = Characters.ALL;
    private int index = 0; // opens on character 1

    private Button backButton;
    private Button forwardButton;

    private double scroll = 0;
    private int maxScroll = 0;

    public CharacterSelectScreen() {
        super(Component.translatable("screen.vril.select_character"));
    }

    @Override
    protected void init() {
        this.backButton = this.addRenderableWidget(
                Button.builder(Component.translatable("screen.vril.back"), b -> shift(-1))
                        .bounds(MARGIN, MARGIN, NAV_WIDTH, BUTTON_HEIGHT).build());
        this.forwardButton = this.addRenderableWidget(
                Button.builder(Component.translatable("screen.vril.forward"), b -> shift(1))
                        .bounds(this.width - MARGIN - NAV_WIDTH, MARGIN, NAV_WIDTH, BUTTON_HEIGHT).build());

        // Nothing to click through yet; they light up once a second character exists.
        boolean multiple = characters.size() > 1;
        this.backButton.active = multiple;
        this.forwardButton.active = multiple;

        int chooseWidth = Math.min(this.width - MARGIN * 2, 220);
        this.addRenderableWidget(Button.builder(Component.translatable("screen.vril.choose"), b -> choose())
                .bounds((this.width - chooseWidth) / 2, this.height - MARGIN - BUTTON_HEIGHT,
                        chooseWidth, BUTTON_HEIGHT).build());
    }

    private void shift(int delta) {
        index = Math.floorMod(index + delta, characters.size());
        scroll = 0;
    }

    private void choose() {
        ModNetwork.CHANNEL.sendToServer(new ChooseCharacterPacket(characters.get(index).id()));
        this.onClose();
    }

    // Open area geometry, shared by render() and mouseScrolled().
    private int areaLeft() {
        return MARGIN;
    }

    private int areaRight() {
        return this.width - MARGIN;
    }

    private int areaTop() {
        return MARGIN + PORTRAIT_SIZE + 4 + this.font.lineHeight + 8;
    }

    private int areaBottom() {
        return this.height - MARGIN - BUTTON_HEIGHT - 8;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        int cx = this.width / 2;
        CharacterType c = characters.get(index);

        // portrait, top centre, scaled up from its native size
        int px = cx - PORTRAIT_SIZE / 2;
        g.blit(c.icon(), px, MARGIN, PORTRAIT_SIZE, PORTRAIT_SIZE, 0, 0,
                c.iconSize(), c.iconSize(), c.iconSize(), c.iconSize());
        g.renderOutline(px - 1, MARGIN - 1, PORTRAIT_SIZE + 2, PORTRAIT_SIZE + 2, 0xFF777777);

        Component name = Component.translatable("character.vril." + c.id()).withStyle(ChatFormatting.BOLD);
        g.drawCenteredString(this.font, name, cx, MARGIN + PORTRAIT_SIZE + 4, 0xFFFFFF);

        // open area
        int left = areaLeft();
        int right = areaRight();
        int top = areaTop();
        int bottom = areaBottom();
        g.fill(left, top, right, bottom, 0xC0101010);
        g.renderOutline(left, top, right - left, bottom - top, 0xFF555555);

        int textLeft = left + PAD;
        int textWidth = right - left - PAD * 2 - SCROLLBAR_WIDTH - 2;
        int viewHeight = bottom - top - PAD * 2;

        // Lay out the content (bio, then traits) to measure it, drawing only
        // what falls inside the open area.
        g.enableScissor(left + 1, top + 1, right - 1, bottom - 1);
        int y = top + PAD - (int) scroll;
        int startY = y;

        Component bio = Component.translatable("character.vril." + c.id() + ".bio");
        for (FormattedCharSequence line : this.font.split(bio, textWidth)) {
            g.drawString(this.font, line, textLeft, y, 0xDDDDDD, false);
            y += 10;
        }
        y += 8;

        for (CharacterType.Trait trait : c.traits()) {
            Component traitName = Component.translatable("trait.vril." + trait.id() + ".name")
                    .withStyle(trait.kind().color);
            g.drawString(this.font, traitName, textLeft, y, 0xFFFFFF, false);
            y += 10;

            Component desc = Component.translatable("trait.vril." + trait.id() + ".desc");
            for (FormattedCharSequence line : this.font.split(desc, textWidth)) {
                g.drawString(this.font, line, textLeft, y, 0xAAAAAA, false);
                y += 10;
            }
            y += 6;
        }
        g.disableScissor();

        int contentHeight = y - startY;
        maxScroll = Math.max(0, contentHeight - viewHeight);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        // scrollbar, only when there is something to scroll
        if (maxScroll > 0) {
            int trackLeft = right - SCROLLBAR_WIDTH - 3;
            int trackTop = top + 3;
            int trackHeight = bottom - top - 6;
            g.fill(trackLeft, trackTop, trackLeft + SCROLLBAR_WIDTH, trackTop + trackHeight, 0xFF2A2A2A);
            int thumbHeight = Math.max(12, trackHeight * viewHeight / contentHeight);
            int thumbTop = trackTop + (int) ((trackHeight - thumbHeight) * (scroll / maxScroll));
            g.fill(trackLeft, thumbTop, trackLeft + SCROLLBAR_WIDTH, thumbTop + thumbHeight, 0xFF888888);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Mouse wheel scrolls the bio when the cursor is over the open area. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= areaLeft() && mouseX <= areaRight() && mouseY >= areaTop() && mouseY <= areaBottom()) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - delta * SCROLL_STEP));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
