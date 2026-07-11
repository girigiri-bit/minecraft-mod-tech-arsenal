package com.girigiri.techarsenal.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.util.ArrayList;
import java.util.List;

/**
 * Page structure for the in-game field manual (BookViewScreen). Each section
 * is an ordered list of translation keys — one key per rendered page. Text is
 * a condensed rewrite of docs/MANUAL_JA.md, budgeted to fit BookViewScreen's
 * page limits (114px line width, 14 rendered lines/page).
 *
 * Section order mirrors docs/MANUAL_JA.md: intro / camera / weapons / support
 * / deployables / vehicles / ammo / security / quick-ref.
 */
public final class GuideBookContent
{
    private static final String[][] SECTIONS = {
            // intro
            {"techarsenal.guide.intro.1"},
            // camera & surveillance
            {
                    "techarsenal.guide.camera.1",
                    "techarsenal.guide.camera.2",
                    "techarsenal.guide.camera.3"
            },
            // weapons
            {
                    "techarsenal.guide.weapons.1",
                    "techarsenal.guide.weapons.2",
                    "techarsenal.guide.weapons.3",
                    "techarsenal.guide.weapons.4",
                    "techarsenal.guide.weapons.5",
                    "techarsenal.guide.weapons.6"
            },
            // support gear
            {
                    "techarsenal.guide.support.1",
                    "techarsenal.guide.support.2"
            },
            // deployables / turrets / mines
            {
                    "techarsenal.guide.deployables.1",
                    "techarsenal.guide.deployables.2",
                    "techarsenal.guide.deployables.3"
            },
            // vehicles
            {
                    "techarsenal.guide.vehicles.1",
                    "techarsenal.guide.vehicles.2",
                    "techarsenal.guide.vehicles.3"
            },
            // ammo & crafting
            {
                    "techarsenal.guide.ammo.1",
                    "techarsenal.guide.ammo.2"
            },
            // security system
            {
                    "techarsenal.guide.security.1",
                    "techarsenal.guide.security.2",
                    "techarsenal.guide.security.3",
                    "techarsenal.guide.security.4",
                    "techarsenal.guide.security.5"
            },
            // quick reference
            {
                    "techarsenal.guide.quickref.1",
                    "techarsenal.guide.quickref.2"
            }
    };

    private GuideBookContent()
    {
    }

    public static List<FormattedText> buildPages()
    {
        List<FormattedText> pages = new ArrayList<>();
        for (String[] section : SECTIONS)
            for (String key : section)
                pages.add(Component.translatable(key));
        return pages;
    }
}
