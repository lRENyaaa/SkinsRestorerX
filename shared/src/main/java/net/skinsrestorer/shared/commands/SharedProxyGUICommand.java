/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.skinsrestorer.shared.commands;

import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.interfaces.ISRProxyPlayer;
import net.skinsrestorer.shared.listeners.SharedPluginMessageListener;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.log.SRLogger;

import javax.inject.Inject;

public final class SharedProxyGUICommand extends SharedGUICommand {
    @Inject
    private CooldownStorage cooldownStorage;
    @Inject
    private SkinStorage skinStorage;
    @Inject
    private SRLogger srLogger;

    public int onDefault(ISRPlayer player) {
        if (!(player instanceof ISRProxyPlayer)) {
            throw new IllegalStateException("The platform should only have proxy players");
        }

        if (!player.hasPermission("skinsrestorer.bypasscooldown") && cooldownStorage.hasCooldown(player.getUniqueId())) {
            player.sendMessage(Message.SKIN_COOLDOWN, String.valueOf(cooldownStorage.getCooldownSeconds(player.getUniqueId())));
            return;
        }
        player.sendMessage(Message.SKINSMENU_OPEN);

        SharedPluginMessageListener.sendPage(0, (ISRProxyPlayer) player, skinStorage, srLogger);
    }
}
