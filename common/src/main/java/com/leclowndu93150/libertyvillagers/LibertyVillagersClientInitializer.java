package com.leclowndu93150.libertyvillagers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import java.util.LinkedList;
import java.util.List;

public class LibertyVillagersClientInitializer {

    public static void init() {
        LibertyVillagersMod.setIsClient(true);
    }

    public static void openBookScreen(ItemStack bookStack) {
        BookViewScreen screen = new BookViewScreen(BookViewScreen.BookAccess.fromItem(bookStack));

        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(screen));
    }

    public static List<String> wrapText(String string) {
        StringSplitter textHandler = Minecraft.getInstance().font.getSplitter();
        List<String> lines = new LinkedList<>();
        textHandler.splitLines(string, 114, Style.EMPTY, true, (style, ix, jx) -> {
            String substring = string.substring(ix, jx);
            lines.add(substring);
        });
        return lines;
    }

}
