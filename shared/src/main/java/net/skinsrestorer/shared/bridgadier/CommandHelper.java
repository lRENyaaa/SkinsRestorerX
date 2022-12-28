package net.skinsrestorer.shared.bridgadier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommandHelper<S> {
    private final CommandDispatcher<S> dispatcher;
    private final PlatformWrapper<S> wrapper;

    public int help(CommandContext<S> c) {
        dispatcher.getSmartUsage(c.getNodes().get(c.getNodes().size() - 1).getNode(), c.getSource())
                .forEach((key, value) -> wrapper.commandSender(c.getSource()).sendMessage(key.getName() + ": " + value));

        return 1;
    }
}
