package net.skinsrestorer.bungee.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Collections;
import java.util.stream.Collectors;

public class BrigadierCommand extends Command implements TabExecutor {
    private final CommandDispatcher<CommandSender> dispatcher;

    public BrigadierCommand(CommandDispatcher<CommandSender> dispatcher, String name) {
        super(name);
        this.dispatcher = dispatcher;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        try {
            dispatcher.execute(getCommand(args), sender);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        try {
            ParseResults<CommandSender> results = dispatcher.parse(getCommand(args), sender);
            return dispatcher.getCompletionSuggestions(results).join().getList()
                    .stream().map(Suggestion::getText).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private String getCommand(String[] args) {
        return getName() + " " + String.join(" ", args);
    }
}
