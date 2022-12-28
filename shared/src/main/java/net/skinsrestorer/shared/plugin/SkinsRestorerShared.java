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
package net.skinsrestorer.shared.plugin;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import co.aikar.locales.LocaleManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import lombok.Getter;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.interfaces.IPropertyFactory;
import net.skinsrestorer.api.interfaces.ISkinApplier;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.bridgadier.*;
import net.skinsrestorer.shared.commands.SharedGUICommand;
import net.skinsrestorer.shared.commands.SharedSRCommand;
import net.skinsrestorer.shared.commands.SharedSkinCommand;
import net.skinsrestorer.shared.config.Config;
import net.skinsrestorer.shared.config.DatabaseConfig;
import net.skinsrestorer.shared.config.MineSkinConfig;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.ISRForeign;
import net.skinsrestorer.shared.interfaces.ISRLogger;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.MySQL;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.storage.adapter.FileAdapter;
import net.skinsrestorer.shared.storage.adapter.MySQLAdapter;
import net.skinsrestorer.shared.update.UpdateChecker;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.connections.MineSkinAPI;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bstats.MetricsBase;
import org.bstats.charts.SingleLineChart;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public abstract class SkinsRestorerShared implements ISRPlugin {
    protected final boolean unitTest = System.getProperty("sr.unit.test") != null;
    protected final SRLogger logger;
    protected final UpdateChecker updateChecker;
    @Getter
    protected final Path dataFolder;
    @Getter
    protected final String version;
    protected final Injector injector;
    @Getter
    private boolean outdated = false;

    protected SkinsRestorerShared(ISRLogger isrLogger, boolean loggerColor,
                                  String version, String updateCheckerAgent, Path dataFolder,
                                  IWrapperFactory wrapperFactory, IPropertyFactory propertyFactory) {
        this.injector = new InjectorBuilder().addDefaultHandlers("net.skinsrestorer").create();

        injector.register(ISRPlugin.class, this);

        injector.register(MetricsCounter.class, new MetricsCounter());
        injector.register(CooldownStorage.class, new CooldownStorage());
        injector.register(SRLogger.class, (logger = new SRLogger(isrLogger, loggerColor)));

        injector.register(IWrapperFactory.class, wrapperFactory);
        injector.register(IPropertyFactory.class, propertyFactory);

        this.version = version;
        this.updateChecker = new UpdateCheckerGitHub(2124, version, logger, updateCheckerAgent);
        this.dataFolder = dataFolder;
    }

    protected <S> CommandDispatcher<S> createDispatcher(PlatformWrapper<S> wrapper, Class<? extends SharedGUICommand> guiCommandClass) {
        CommandDispatcher<S> dispatcher = new CommandDispatcher<>();

        dispatcher.register(createSkinCommand(wrapper));
        dispatcher.register(createSRCommand(wrapper));

        return dispatcher;
    }

    protected <S> LiteralArgumentBuilder<S> createSRCommand(PlatformWrapper<S> wrapper) {
        ISRPlayerArgumentType playerArgumentType = injector.newInstance(ISRPlayerArgumentType.class);
        SharedSRCommand srCommand = injector.newInstance(SharedSRCommand.class);
        CommandHelper<S> commandHelper = new CommandHelper<>(null, wrapper); // TODO
        EnumArgumentType<SharedSRCommand.PlayerOrSkin> enumArgumentType = new EnumArgumentType<>(SharedSRCommand.PlayerOrSkin.class);

        PermissionCheck<S> permissionCheck = new PermissionCheck<>(injector.getSingleton(SettingsManager.class), wrapper);

        CommandNode<S> dropArgument = LiteralArgumentBuilder.<S>literal("drop")
                .requires(c -> permissionCheck.check(c, "srDrop"))
                .then(
                        RequiredArgumentBuilder.<S, SharedSRCommand.PlayerOrSkin>argument("type", enumArgumentType)
                                .then(
                                        RequiredArgumentBuilder.<S, String>argument("target", StringArgumentType.string())
                                                .executes(c -> srCommand.onDrop(wrapper.commandSender(c.getSource()), c.getArgument("player", SharedSRCommand.PlayerOrSkin.class), StringArgumentType.getString(c, "player")))
                                )
                ).build();
        return LiteralArgumentBuilder.<S>literal("sr")
                .requires(c -> permissionCheck.check(c, "sr"))
                .then(
                        LiteralArgumentBuilder.<S>literal("reload")
                                .requires(c -> permissionCheck.check(c, "srReload"))
                                .executes(c -> srCommand.onReload(wrapper.commandSender(c.getSource()))
                                )
                )
                .then(
                        LiteralArgumentBuilder.<S>literal("status")
                                .requires(c -> permissionCheck.check(c, "srStatus"))
                                .executes(c -> srCommand.onStatus(wrapper.commandSender(c.getSource())))
                )
                .then(dropArgument)
                .then(
                        LiteralArgumentBuilder.<S>literal("remove")
                                .redirect(dropArgument)
                )
                .then(
                        LiteralArgumentBuilder.<S>literal("applyskin")
                                .requires(c -> permissionCheck.check(c, "srApplySkin"))
                                .then(
                                        RequiredArgumentBuilder.<S, ISRPlayer>argument("target", playerArgumentType)
                                                .executes(c -> srCommand.onApplySkin(wrapper.commandSender(c.getSource()), c.getArgument("target", ISRPlayer.class)))
                                )
                )
                .executes(commandHelper::help);
    }

    public <S> LiteralArgumentBuilder<S> createSkinCommand(PlatformWrapper<S> wrapper) {
        ISRPlayerArgumentType playerArgumentType = injector.newInstance(ISRPlayerArgumentType.class);
        EnumArgumentType<SkinVariant> enumArgumentType = new EnumArgumentType<>(SkinVariant.class);
        SharedSkinCommand sharedSkinCommand = injector.getSingleton(SharedSkinCommand.class);

        CommandHelper<S> commandHelper = new CommandHelper<>(null, wrapper); // TODO

        PermissionCheck<S> permissionCheck = new PermissionCheck<>(injector.getSingleton(SettingsManager.class), wrapper);
        RequirePlayer<S> requirePlayer = new RequirePlayer<>(wrapper);

        return LiteralArgumentBuilder.<S>literal("skin")
                .requires(c -> permissionCheck.check(c, "skin"))
                .then(
                        LiteralArgumentBuilder.<S>literal("clear")
                                .requires(c -> permissionCheck.check(c, "skinClear"))
                                .then(
                                        RequiredArgumentBuilder.<S, ISRPlayer>argument("player", playerArgumentType)
                                                .requires(c -> permissionCheck.check(c, "skinClearOther"))
                                                .executes(c -> sharedSkinCommand.onSkinClearOther(
                                                        wrapper.commandSender(c.getSource()), c.getArgument("player", ISRPlayer.class)))
                                )
                                .executes(c -> requirePlayer.require(c, sharedSkinCommand::onSkinClear))
                )
                .then(
                        LiteralArgumentBuilder.<S>literal("search")
                                .requires(c -> permissionCheck.check(c, "skinSearch"))
                                .then(
                                        RequiredArgumentBuilder.<S, String>argument("name", StringArgumentType.string())
                                                .executes(c -> sharedSkinCommand.onSkinSearch(wrapper.commandSender(c.getSource()), c.getArgument("name", String.class)))
                                )
                                .executes(commandHelper::help)
                )
                .then(
                        LiteralArgumentBuilder.<S>literal("update")
                                .requires(c -> permissionCheck.check(c, "skinUpdate"))
                                .then(
                                        RequiredArgumentBuilder.<S, ISRPlayer>argument("player", playerArgumentType)
                                                .requires(c -> permissionCheck.check(c, "skinUpdateOther"))
                                                .executes(c -> sharedSkinCommand.onSkinUpdateOther(wrapper.commandSender(c.getSource()), c.getArgument("player", ISRPlayer.class)))
                                )
                                .executes(c -> requirePlayer.require(c, sharedSkinCommand::onSkinUpdate))
                )
                .then(
                        LiteralArgumentBuilder.<S>literal("set")
                                .requires(c -> permissionCheck.check(c, "skinSet"))
                                .then(
                                        RequiredArgumentBuilder.<S, ISRPlayer>argument("player", playerArgumentType)
                                                .requires(c -> permissionCheck.check(c, "skinSetOther"))
                                                .then(
                                                        RequiredArgumentBuilder.<S, String>argument("skin", StringArgumentType.string())
                                                                .then(
                                                                        RequiredArgumentBuilder.<S, SkinVariant>argument("variant", enumArgumentType)
                                                                                .executes(c -> sharedSkinCommand.onSkinSetOther(
                                                                                        wrapper.commandSender(c.getSource()), c.getArgument("player", ISRPlayer.class),
                                                                                        c.getArgument("skin", String.class), c.getArgument("variant", SkinVariant.class)))
                                                                )
                                                                .executes(c -> sharedSkinCommand.onSkinSetOther(
                                                                        wrapper.commandSender(c.getSource()), c.getArgument("player", ISRPlayer.class),
                                                                        c.getArgument("skin", String.class), null))
                                                )
                                )
                                .then(
                                        RequiredArgumentBuilder.<S, String>argument("skin", StringArgumentType.string())
                                                .executes(c -> requirePlayer.require(c, p -> sharedSkinCommand.onSkinSet(p, c.getArgument("skin", String.class))))
                                )
                                .executes(commandHelper::help)
                )
                .then(
                        LiteralArgumentBuilder.<S>literal("url")
                                .requires(c -> permissionCheck.check(c, "skinUrl"))
                                .then(
                                        RequiredArgumentBuilder.<S, SkinVariant>argument("variant", enumArgumentType)
                                                .executes(c -> requirePlayer.require(c, p -> sharedSkinCommand.onSkinUrl(
                                                        p, c.getArgument("url", String.class),
                                                        c.getArgument("variant", SkinVariant.class))))
                                )
                                .then(
                                        RequiredArgumentBuilder.<S, String>argument("url", StringArgumentType.string())
                                                .executes(c -> requirePlayer.require(c, p -> sharedSkinCommand.onSkinUrl(p, c.getArgument("url", String.class), null)))
                                )
                                .executes(commandHelper::help)
                )
                .then(
                        RequiredArgumentBuilder.<S, String>argument("skin", StringArgumentType.string())
                                .executes(c -> requirePlayer.require(c, p -> sharedSkinCommand.onSkinSet(p, c.getArgument("skin", String.class))))
                );
    }


    public <S> LiteralArgumentBuilder<S> createSkinsCommand(SharedGUICommand sharedGUICommand, PlatformWrapper<S> wrapper) {
        PermissionCheck<S> permissionCheck = new PermissionCheck<>(injector.getSingleton(SettingsManager.class), wrapper);
        RequirePlayer<S> requirePlayer = new RequirePlayer<>(wrapper);

        return LiteralArgumentBuilder.<S>literal("skins")
                .requires(c -> permissionCheck.check(c, "skins"))
                .executes(c -> requirePlayer.require(c, sharedGUICommand::onDefault));
    }

    protected abstract boolean isProxyMode();

    public void checkUpdate(boolean showUpToDate) {
        runAsync(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                outdated = true;
                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, version, isProxyMode())
                        .forEach(logger::info);
            }

            @Override
            public void upToDate() {
                if (!showUpToDate)
                    return;

                updateChecker.getUpToDateMessages(version, isProxyMode()).forEach(logger::info);
            }
        }));
    }

    public void loadConfig() {
        SettingsManager settings = injector.getIfAvailable(SettingsManager.class);
        if (settings == null) {
            settings = SettingsManagerBuilder
                    .withYamlFile(dataFolder.resolve("config.yml"))
                    .configurationData(Config.class, DatabaseConfig.class, StorageConfig.class, MineSkinConfig.class)
                    .useDefaultMigrationService()
                    .create();
            injector.register(SettingsManager.class, settings);
        } else {
            settings.reload();
        }

        //__Default__Skins
        if (settings.getProperty(StorageConfig.DEFAULT_SKINS_ENABLED) && settings.getProperty(StorageConfig.DEFAULT_SKINS).isEmpty()) {
            logger.warning("[Config] no DefaultSkins found! Disabling DefaultSkins.");
            settings.setProperty(StorageConfig.DEFAULT_SKINS_ENABLED, false);
        }

        //__Disabled__Skins
        if (settings.getProperty(Config.DISABLED_SKINS_ENABLED) && settings.getProperty(Config.DISABLED_SKINS).isEmpty()) {
            logger.warning("[Config] no DisabledSkins found! Disabling DisabledSkins.");
            settings.setProperty(Config.DISABLED_SKINS_ENABLED, false);
        }

        if (settings.getProperty(Config.RESTRICT_SKIN_URLS_ENABLED) && settings.getProperty(Config.RESTRICT_SKIN_URLS_LIST).isEmpty()) {
            logger.warning("[Config] no RestrictSkinUrls found! Disabling RestrictSkinUrls.");
            settings.setProperty(Config.RESTRICT_SKIN_URLS_ENABLED, false);
        }

        if (!settings.getProperty(StorageConfig.CUSTOM_GUI_ENABLED))
            settings.setProperty(StorageConfig.CUSTOM_GUI_ONLY, false);

        if (!settings.getProperty(Config.DISMOUNT_PLAYER_ON_UPDATE)) {
            settings.setProperty(Config.REMOUNT_PLAYER_ON_UPDATE, false);
        }

        if (settings.getProperty(MineSkinConfig.MINESKIN_API_KEY).equals("key")) {
            settings.setProperty(MineSkinConfig.MINESKIN_API_KEY, "");
        }

        logger.setDebug(settings.getProperty(Config.DEBUG));
        SkinsRestorerLocale locale = injector.getIfAvailable(SkinsRestorerLocale.class);
        if (locale != null) {
            locale.setDefaultLocale(settings.getProperty(Config.LANGUAGE));
        }
    }

    public void loadLocales() {
        LocaleManager<ISRForeign> localeManager = LocaleManager.create(ISRForeign::getLocale, Locale.ENGLISH);
        injector.register(LocaleManager.class, localeManager);
        Message.load(localeManager, dataFolder, this);
        injector.getSingleton(SkinsRestorerLocale.class);
    }

    protected void initStorage() throws InitializeException {
        // Initialise SkinStorage
        SkinStorage skinStorage = injector.getSingleton(SkinStorage.class);
        SettingsManager settings = injector.getSingleton(SettingsManager.class);
        try {
            if (settings.getProperty(DatabaseConfig.MYSQL_ENABLED)) {
                MySQL mysql = new MySQL(
                        logger,
                        settings.getProperty(DatabaseConfig.MYSQL_HOST),
                        settings.getProperty(DatabaseConfig.MYSQL_PORT),
                        settings.getProperty(DatabaseConfig.MYSQL_DATABASE),
                        settings.getProperty(DatabaseConfig.MYSQL_USERNAME),
                        settings.getProperty(DatabaseConfig.MYSQL_PASSWORD),
                        settings.getProperty(DatabaseConfig.MYSQL_MAX_POOL_SIZE),
                        settings.getProperty(DatabaseConfig.MYSQL_CONNECTION_OPTIONS)
                );

                mysql.connectPool();
                mysql.createTable(settings);

                logger.info("Connected to MySQL!");
                skinStorage.setStorageAdapter(new MySQLAdapter(mysql, settings));
            } else {
                skinStorage.setStorageAdapter(new FileAdapter(dataFolder, settings));
            }

            // Preload default skins
            runAsync(skinStorage::preloadDefaultSkins);
        } catch (SQLException e) {
            logger.severe("§cCan't connect to MySQL! Disabling SkinsRestorer.", e);
            throw new InitializeException(e);
        } catch (IOException e) {
            logger.severe("§cCan't create data folders! Disabling SkinsRestorer.", e);
            throw new InitializeException(e);
        }
    }

    public void checkUpdateInit(Runnable check) {
        Path updaterDisabled = dataFolder.resolve("noupdate.txt");
        if (Files.exists(updaterDisabled)) {
            logger.info("Updater Disabled");
        } else {
            check.run();
        }
    }

    protected void setOutdated() {
        outdated = true;
    }

    protected void initMineSkinAPI() {
        injector.getSingleton(MineSkinAPI.class);
    }

    protected void registerAPI(ISkinApplier skinApplier) {
        injector.register(SkinsRestorerAPI.class, new SkinsRestorerAPI(
                injector.getSingleton(MojangAPI.class),
                injector.getSingleton(MineSkinAPI.class),
                injector.getSingleton(SkinStorage.class),
                injector.getSingleton(IWrapperFactory.class),
                injector.getSingleton(IPropertyFactory.class), skinApplier));
    }

    protected void registerMetrics(Object metricsParent) {
        MetricsCounter metricsCounter = injector.getSingleton(MetricsCounter.class);
        try {
            Field field = metricsParent.getClass().getDeclaredField("metricsBase");
            field.setAccessible(true);
            MetricsBase metrics = (MetricsBase) field.get(metricsParent);

            metrics.addCustomChart(new SingleLineChart("mineskin_calls", () -> metricsCounter.collect(MetricsCounter.Service.MINE_SKIN)));
            metrics.addCustomChart(new SingleLineChart("minetools_calls", () -> metricsCounter.collect(MetricsCounter.Service.MINE_TOOLS)));
            metrics.addCustomChart(new SingleLineChart("mojang_calls", () -> metricsCounter.collect(MetricsCounter.Service.MOJANG)));
            metrics.addCustomChart(new SingleLineChart("ashcon_calls", () -> metricsCounter.collect(MetricsCounter.Service.ASHCON)));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    protected abstract void pluginStartup() throws InitializeException;

    protected abstract Object createMetricsInstance();

    protected void startupStart() {
        logger.load(dataFolder);

        if (!unitTest) {
            registerMetrics(createMetricsInstance());
        }

        runRepeatAsync(injector.getSingleton(CooldownStorage.class)::cleanup, 60, 60, TimeUnit.SECONDS);
    }

    protected void updateCheck() {
        checkUpdateInit(() -> {
            checkUpdate(true);

            int delayInt = 60 + ThreadLocalRandom.current().nextInt(240 - 60 + 1);
            runRepeatAsync(this::checkUpdate, delayInt, delayInt, TimeUnit.MINUTES);
        });
    }
}
