package net.skinsrestorer.shared.commands;

import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.interfaces.ISRProxyPlayer;

public abstract class SharedGUICommand {
    public abstract int onDefault(ISRPlayer player);
}
