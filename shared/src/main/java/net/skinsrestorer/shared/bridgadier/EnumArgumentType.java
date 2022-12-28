package net.skinsrestorer.shared.bridgadier;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EnumArgumentType<T extends Enum<T>> implements ArgumentType<T> {
    private final Class<T> enumClass;

    @Override
    public T parse(StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String result = reader.readString();
        try {
            return Enum.valueOf(enumClass, result.toUpperCase());
        } catch (IllegalArgumentException e) {
            reader.setCursor(start);
            throw new DynamicCommandExceptionType(
                    name -> new LiteralMessage("Value " + name + " does not exist")).createWithContext(reader, result);
        }
    }
}
