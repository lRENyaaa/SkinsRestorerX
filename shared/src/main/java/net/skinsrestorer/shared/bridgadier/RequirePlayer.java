package net.skinsrestorer.shared.bridgadier;

import com.mojang.brigadier.context.CommandContext;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.interfaces.ISRCommandSender;
import net.skinsrestorer.shared.interfaces.ISRPlayer;

import java.util.function.Function;

@RequiredArgsConstructor
public class RequirePlayer<S> {
    private final PlatformWrapper<S> wrapper;

    public int require(CommandContext<S> c, Function<ISRPlayer, Integer> consumer) {
        ISRCommandSender sender = wrapper.commandSender(c.getSource());
        if (sender instanceof ISRPlayer) {
            return consumer.apply((ISRPlayer) sender);
        } else {
            sender.sendMessage("This command can only be used by players");
            return 0;
        }
    }
}
