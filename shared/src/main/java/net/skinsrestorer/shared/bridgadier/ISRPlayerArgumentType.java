package net.skinsrestorer.shared.bridgadier;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.interfaces.ISRServerPlugin;

import javax.inject.Inject;
import java.util.Optional;

public class ISRPlayerArgumentType implements ArgumentType<ISRPlayer> {
    @Inject
    private ISRServerPlugin playerGetter;

    @Override
    public ISRPlayer parse(StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String result = reader.readString();
        Optional<ISRPlayer> optional = playerGetter.getPlayer(result);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            reader.setCursor(start);
            throw new DynamicCommandExceptionType(
                    name -> new LiteralMessage("Could not find player " + name)).createWithContext(reader, result);
        }
    }
}
