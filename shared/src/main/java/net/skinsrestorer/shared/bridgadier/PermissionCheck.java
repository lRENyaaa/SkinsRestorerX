package net.skinsrestorer.shared.bridgadier;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.interfaces.ISRCommandSender;
import net.skinsrestorer.shared.utils.CommandReplacements;

import javax.inject.Inject;

@RequiredArgsConstructor
public class PermissionCheck<S> {
    private final SettingsManager settings;
    private final PlatformWrapper<S> wrapper;

    public boolean check(S c, String permission) {
        return wrapper.commandSender(c).hasPermission(CommandReplacements.permissions.get(permission).call(settings));
    }
}
