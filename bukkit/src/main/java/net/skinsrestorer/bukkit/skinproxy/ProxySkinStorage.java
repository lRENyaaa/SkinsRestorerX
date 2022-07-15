package net.skinsrestorer.bukkit.skinproxy;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.ISkinStorage;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.utils.IOExceptionConsumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public class ProxySkinStorage implements ISkinStorage {
    private final SkinsRestorer plugin;

    @Override
    public Optional<String> getSkinOfPlayer(String playerName) {
        UUID messageId = UUID.randomUUID();

        return Optional.empty();
    }

    @Override
    public IProperty getSkinForPlayer(String playerName) throws SkinRequestException {
        UUID messageId = UUID.randomUUID();

        return null;
    }

    @Override
    public void removeSkinOfPlayer(String playerName) {
        UUID messageId = UUID.randomUUID();

    }

    @Override
    public void setSkinOfPlayer(String playerName, String skinName) {
        UUID messageId = UUID.randomUUID();


    }

    @Override
    public Optional<IProperty> getSkinData(String skinName) {
        UUID messageId = UUID.randomUUID();

        return Optional.empty();
    }

    @Override
    public Optional<IProperty> getSkinData(String skinName, boolean updateOutdated) {
        UUID messageId = UUID.randomUUID();

        return Optional.empty();
    }

    @Override
    public void setSkinData(String skinName, IProperty textures, long timestamp) {
        UUID messageId = UUID.randomUUID();

    }

    @Override
    public Map<String, IProperty> getSkins(int skinOffset) {
        throw new UnsupportedOperationException("Not supported on ProxySkinStorage.");
    }

    private void sendToMessageChannel(IOExceptionConsumer<DataOutputStream> consumer) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);

        try {
            consumer.accept(out);

            List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
            players.get(ThreadLocalRandom.current().nextInt(players.size()))
                    .sendPluginMessage(plugin, "sr:messagechannel", bytes.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
