package com.girigiri.techarsenal.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.FormattedText;

import java.util.List;

/**
 * Opens the field manual as a vanilla BookViewScreen backed by
 * GuideBookContent's flattened page list. Client-only — only ever reached
 * through DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> GuideBookOpener::open)
 * from FieldManualItem, never referenced directly by any class loaded on a
 * dedicated server.
 */
public final class GuideBookOpener
{
    private GuideBookOpener()
    {
    }

    public static void open()
    {
        List<FormattedText> pages = GuideBookContent.buildPages();
        BookViewScreen.BookAccess access = new BookViewScreen.BookAccess()
        {
            @Override
            public int getPageCount()
            {
                return pages.size();
            }

            @Override
            public FormattedText getPageRaw(int page)
            {
                return pages.get(page);
            }
        };
        Minecraft.getInstance().setScreen(new BookViewScreen(access));
    }
}
