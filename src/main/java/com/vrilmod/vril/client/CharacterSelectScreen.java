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

/** Origins-style character selection. Cannot be dismissed with ESC. */
public class CharacterSelectScreen extends Screen {
    private static final int PANEL_WIDTH = 320;

    private final List<CharacterType> characters = Characters.ALL;
    private int index = 0;

    public CharacterSelectScreen() {
        super(Component.translatable("screen.vril.select_character"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("screen.vril.choose"), b -> choose())
                .bounds(cx - 60, this.height - 36, 120, 20).build());

        if (characters.size() > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), b -> shift(-1))
                    .bounds(cx - PANEL_WIDTH / 2 - 30, this.height / 2 - 10, 20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal(">"), b -> shift(1))
                    .bounds(cx + PANEL_WIDTH / 2 + 10, this.height / 2 - 10, 20, 20).build());
        }
    }

    private void shift(int delta) {
        index = Math.floorMod(index + delta, characters.size());
    }

    private void choose() {
        ModNetwork.CHANNEL.sendToServer(new ChooseCharacterPacket(characters.get(index).id()));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        int cx = this.width / 2;
        int left = cx - PANEL_WIDTH / 2;
        int top = 34;
        int bottom = this.height - 46;

        g.drawCenteredString(this.font, this.title, cx, 14, 0xFFFFFF);

        g.fill(left, top, left + PANEL_WIDTH, bottom, 0xC0101010);
        g.renderOutline(left, top, PANEL_WIDTH, bottom - top, 0xFF555555);

        CharacterType c = characters.get(index);

        g.enableScissor(left, top, left + PANEL_WIDTH, bottom);

        // portrait, scaled up from its native size to 48x48
        int size = 48;
        g.blit(c.icon(), cx - size / 2, top + 8, size, size, 0, 0,
                c.iconSize(), c.iconSize(), c.iconSize(), c.iconSize());
        g.renderOutline(cx - size / 2 - 1, top + 7, size + 2, size + 2, 0xFF777777);

        Component name = Component.translatable("character." + "vril." + c.id()).withStyle(ChatFormatting.BOLD);
        g.drawCenteredString(this.font, name, cx, top + 62, 0xFFFFFF);

        int y = top + 80;
        for (CharacterType.Trait trait : c.traits()) {
            Component traitName = Component.translatable("trait.vril." + trait.id() + ".name")
                    .withStyle(trait.kind().color);
            g.drawString(this.font, traitName, left + 14, y, 0xFFFFFF, false);
            y += 10;

            Component desc = Component.translatable("trait.vril." + trait.id() + ".desc");
            for (FormattedCharSequence line : this.font.split(desc, PANEL_WIDTH - 28)) {
                g.drawString(this.font, line, left + 14, y, 0xAAAAAA, false);
                y += 10;
            }
            y += 6;
        }

        g.disableScissor();

        super.render(g, mouseX, mouseY, partialTick);
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
