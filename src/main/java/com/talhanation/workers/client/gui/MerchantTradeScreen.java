package com.talhanation.workers.client.gui;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantTradeContainer;
import com.talhanation.workers.network.MessageMerchantTradeButton;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class MerchantTradeScreen extends ScreenBase<MerchantTradeContainer> {

    private static final ResourceLocation GUI_TEXTURE_3 = new ResourceLocation(Main.MOD_ID,
            "textures/gui/merchant_gui.png");

    private final MerchantEntity merchant;
    private final Inventory playerInventory;

    private final MutableComponent TRADE_TEXT = Component.translatable("gui.workers.merchant.trade");

    public MerchantTradeScreen(MerchantTradeContainer container, Inventory playerInventory, Component title) {
        super(GUI_TEXTURE_3, container, playerInventory, Component.literal(""));
        this.merchant = (MerchantEntity) container.getWorker();
        this.playerInventory = playerInventory;

        imageWidth = 176;
        imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        // CAMP POS

        addRenderableWidget(new ExtendedButton(leftPos + 110, topPos + 20 + 18 * 0, 48, 12, TRADE_TEXT, button -> {
            Main.SIMPLE_CHANNEL
                    .sendToServer(new MessageMerchantTradeButton(merchant.getUUID(), playerInventory.player.getUUID(), 0));
        }));

        addRenderableWidget(new ExtendedButton(leftPos + 110, topPos + 20 + 18 * 1, 48, 12, TRADE_TEXT, button -> {
            Main.SIMPLE_CHANNEL
                    .sendToServer(new MessageMerchantTradeButton(merchant.getUUID(), playerInventory.player.getUUID(), 1));
        }));

        addRenderableWidget(new ExtendedButton(leftPos + 110, topPos + 20 + 18 * 2, 48, 12, TRADE_TEXT, button -> {
            Main.SIMPLE_CHANNEL
                    .sendToServer(new MessageMerchantTradeButton(merchant.getUUID(), playerInventory.player.getUUID(), 2));
        }));

        addRenderableWidget(new ExtendedButton(leftPos + 110, topPos + 20 + 18 * 3, 48, 12, TRADE_TEXT, button -> {
            Main.SIMPLE_CHANNEL
                    .sendToServer(new MessageMerchantTradeButton(merchant.getUUID(), playerInventory.player.getUUID(), 3));
        }));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        guiGraphics.drawString(font, merchant.getDisplayName().getVisualOrderText(), 8, 6, FONT_COLOR);
        guiGraphics.pose().popPose();

        guiGraphics.drawString(font, playerInventory.getDisplayName().getVisualOrderText(), 8, imageHeight - 152 + 25,
                FONT_COLOR);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
    }
}
