package net.skinsrestorer.shared.bridgadier;

import net.skinsrestorer.shared.interfaces.ISRCommandSender;

public interface PlatformWrapper<S> {
    ISRCommandSender commandSender(S sender);
}
