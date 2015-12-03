/*
 * This file is part of EssentialCmds, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2015 HassanS6000
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.github.hsyyid.essentialcmds;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.github.hsyyid.essentialcmds.cmdexecutors.*;
import io.github.hsyyid.essentialcmds.listeners.*;
import io.github.hsyyid.essentialcmds.managers.config.Config;
import io.github.hsyyid.essentialcmds.utils.*;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.config.ConfigDir;
import org.spongepowered.api.service.config.DefaultConfig;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.args.GenericArguments;
import org.spongepowered.api.util.command.spec.CommandSpec;
import org.spongepowered.api.world.TeleportHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.github.hsyyid.essentialcmds.PluginInfo.*;

@Plugin(id = ID, name = NAME, version = VERSION)
public class EssentialCmds {

	private EssentialCmds() {}
	private static EssentialCmds essentialCmds = new EssentialCmds();


	public static ConfigurationNode config;
	public static ConfigurationLoader<CommentedConfigurationNode> configurationManager;
	public static TeleportHelper helper;
	public static List<PendingInvitation> pendingInvites = Lists.newArrayList();
	public static List<AFK> movementList = Lists.newArrayList();
	public static List<Player> recentlyJoined = Lists.newArrayList();
	public static List<Powertool> powertools = Lists.newArrayList();
	public static Set<UUID> socialSpies = Sets.newHashSet();
	public static List<Message> recentlyMessaged = Lists.newArrayList();
	public static Set<UUID> muteList = Sets.newHashSet();
	public static Set<UUID> frozenPlayers = Sets.newHashSet();

	@Inject
	private Logger logger;

	@Inject
	private Game game;

	@Inject
	@ConfigDir(sharedRoot = false)
	private Path configDir;

	@Inject
	@DefaultConfig(sharedRoot = false)
	private Path dConfig;

	@Inject
	@DefaultConfig(sharedRoot = false)
	private ConfigurationLoader<CommentedConfigurationNode> confManager;

	public static EssentialCmds getEssentialCmds() {
		return essentialCmds;
	}

	@Listener
	public void onPreInitialization(GamePreInitializationEvent event) {
		getLogger().info(ID + " loading...");

		// Create Config Directory for EssentialCmds
		if (!Files.exists(configDir)) {
			try {
				Files.createDirectories(configDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Create config.conf
		Config.getConfig().setup();
	}

	@Listener
	public void onServerInit(GameInitializationEvent event) {
		helper = getGame().getTeleportHelper();

		// Config File
		try
		{
			if (!Files.exists(dConfig))
			{
				Files.createFile(dConfig);
				config = confManager.load();
				config.getNode("afk", "timer").setValue(30000);
				config.getNode("afk", "kick", "use").setValue(false);
				config.getNode("afk", "kick", "timer").setValue(30000);
				config.getNode("joinmsg").setValue("&4Welcome!");
				confManager.save(config);
			}

			configurationManager = confManager;
			config = confManager.load();
		}
		catch (IOException exception)
		{
			getLogger().error("The default configuration could not be loaded or created!");
		}
		
		Utils.readMutes();
		Utils.startAFKService();

		CommandSpec homeCommandSpec =
			CommandSpec.builder().description(Texts.of("Home Command")).permission("essentialcmds.home.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("home name")))).executor(new HomeExecutor()).build();
		getGame().getCommandDispatcher().register(this, homeCommandSpec, "home");
		
		CommandSpec mobSpawnerCommandSpec =
			CommandSpec.builder().description(Texts.of("Mob Spawner Command")).permission("essentialcmds.mobspawner.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("mob name")))).executor(new MobSpawnerExecutor()).build();
		getGame().getCommandDispatcher().register(this, mobSpawnerCommandSpec, "spawner", "mobspawner");
		
		CommandSpec removeRuleCommandSpec =
			CommandSpec.builder().description(Texts.of("Home Command")).permission("essentialcmds.rules.remove")
				.arguments(GenericArguments.onlyOne(GenericArguments.integer(Texts.of("rule number")))).executor(new RemoveRuleExecutor()).build();
		getGame().getCommandDispatcher().register(this, removeRuleCommandSpec, "removerule", "delrule", "deleterule");
		
		CommandSpec addRuleCommandSpec =
			CommandSpec.builder().description(Texts.of("Add Rule Command")).permission("essentialcmds.rules.add")
				.arguments(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("rule")))).executor(new AddRuleExecutor()).build();
		getGame().getCommandDispatcher().register(this, addRuleCommandSpec, "addrule");
		
		CommandSpec deleteWorldCommandSpec =
			CommandSpec.builder().description(Texts.of("Delete World Command")).permission("essentialcmds.world.delete")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("name")))).executor(new DeleteWorldExecutor()).build();
		getGame().getCommandDispatcher().register(this, deleteWorldCommandSpec, "delworld", "deleteworld");

		CommandSpec moreCommandSpec =
			CommandSpec.builder().description(Texts.of("More Command")).permission("essentialcmds.more.use")
				.executor(new MoreExecutor()).build();
		getGame().getCommandDispatcher().register(this, moreCommandSpec, "more", "stack");
		
		CommandSpec thruCommandSpec =
			CommandSpec.builder().description(Texts.of("Thru Command")).permission("essentialcmds.thru.use")
				.executor(new ThruExecutor()).build();
		getGame().getCommandDispatcher().register(this, thruCommandSpec, "through", "thru");
		
		CommandSpec directionCommandSpec =
			CommandSpec.builder().description(Texts.of("Direction Command")).permission("essentialcmds.direction.use")
				.executor(new DirectionExecutor()).build();
		getGame().getCommandDispatcher().register(this, directionCommandSpec, "direction", "compass");
		
		CommandSpec itemInfoCommandSpec =
			CommandSpec.builder().description(Texts.of("ItemInfo Command")).permission("essentialcmds.iteminfo.use")
				.executor(new ItemInfoExecutor()).build();
		getGame().getCommandDispatcher().register(this, itemInfoCommandSpec, "iteminfo");
		
		CommandSpec blockInfoCommandSpec =
			CommandSpec.builder().description(Texts.of("BlockInfo Command")).permission("essentialcmds.blockinfo.use")
				.executor(new BlockInfoExecutor()).build();
		getGame().getCommandDispatcher().register(this, blockInfoCommandSpec, "blockinfo");
		
		CommandSpec entityInfoCommandSpec =
			CommandSpec.builder().description(Texts.of("EntityInfo Command")).permission("essentialcmds.entityinfo.use")
				.executor(new EntityInfoExecutor()).build();
		getGame().getCommandDispatcher().register(this, entityInfoCommandSpec, "entityinfo");
		
		CommandSpec rtpCommandSpec =
			CommandSpec.builder().description(Texts.of("RTP Command")).permission("essentialcmds.rtp.use")
				.executor(new RTPExecutor()).build();
		getGame().getCommandDispatcher().register(this, rtpCommandSpec, "rtp", "randomtp");

		CommandSpec butcherCommandSpec =
			CommandSpec.builder().description(Texts.of("Butcher Command")).permission("essentialcmds.butcher.use")
				.executor(new ButcherExecutor()).build();
		getGame().getCommandDispatcher().register(this, butcherCommandSpec, "butcher");
		
		CommandSpec rulesCommandSpec =
			CommandSpec.builder().description(Texts.of("Rules Command")).permission("essentialcmds.rules.use")
				.executor(new RuleExecutor()).build();
		getGame().getCommandDispatcher().register(this, rulesCommandSpec, "rules");

		CommandSpec vanishCommandSpec =
			CommandSpec.builder().description(Texts.of("Vanish Command")).permission("essentialcmds.vanish.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame())))).executor(new VanishExecutor()).build();
		getGame().getCommandDispatcher().register(this, vanishCommandSpec, "vanish");

		CommandSpec igniteCommandSpec =
			CommandSpec.builder().description(Texts.of("Ignite Command")).permission("essentialcmds.ignite.use")
				.arguments(GenericArguments.seq(
					GenericArguments.onlyOne(GenericArguments.integer(Texts.of("ticks"))),
						GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame())))))
				.executor(new IgniteExecutor()).build();
		getGame().getCommandDispatcher().register(this, igniteCommandSpec, "burn", "ignite", "fire");

		CommandSpec whoIsCommandSpec =
			CommandSpec.builder().description(Texts.of("WhoIs Command")).permission("essentialcmds.whois.use")
				.arguments(
					GenericArguments.firstParsing(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame())),
						GenericArguments.onlyOne(GenericArguments.string(Texts.of("player name")))))
				.executor(new WhoisExecutor()).build();
		getGame().getCommandDispatcher().register(this, whoIsCommandSpec, "whois", "realname", "seen");

		CommandSpec playerFreezeCommandSpec =
			CommandSpec.builder().description(Texts.of("Player Freeze Command")).permission("essentialcmds.playerfreeze.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame()))).executor(new PlayerFreezeExecutor()).build();
		getGame().getCommandDispatcher().register(this, playerFreezeCommandSpec, "playerfreeze", "freezeplayer");

		CommandSpec skullCommandSpec =
			CommandSpec.builder().description(Texts.of("Skull Command")).permission("essentialcmds.skull.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame())))).executor(new SkullExecutor()).build();
		getGame().getCommandDispatcher().register(this, skullCommandSpec, "skull", "playerskull", "head");

		CommandSpec getPosCommandSpec =
			CommandSpec.builder().description(Texts.of("GetPos Command")).permission("essentialcmds.getpos.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame()))))
				.executor(new GetPosExecutor()).build();
		getGame().getCommandDispatcher().register(this, getPosCommandSpec, "getpos");

		CommandSpec gamemodeCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Gamemode Command"))
				.permission("essentialcmds.gamemode.use")
				.arguments(
					GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.string(Texts.of("gamemode"))),
						GenericArguments.onlyOne(GenericArguments.optional(GenericArguments.player(Texts.of("player"), getGame())))))
				.executor(new GamemodeExecutor()).build();
		getGame().getCommandDispatcher().register(this, gamemodeCommandSpec, "gamemode", "gm");

		CommandSpec motdCommandSpec =
			CommandSpec.builder().description(Texts.of("MOTD Command")).permission("essentialcmds.motd.use").executor(new MotdExecutor()).build();
		getGame().getCommandDispatcher().register(this, motdCommandSpec, "motd");

		CommandSpec socialSpyCommandSpec =
			CommandSpec.builder().description(Texts.of("Allows Toggling of Seeing Other Players Private Messages")).permission("essentialcmds.socialspy.use")
				.executor(new SocialSpyExecutor()).build();
		getGame().getCommandDispatcher().register(this, socialSpyCommandSpec, "socialspy");

		CommandSpec mailListCommandSpec =
			CommandSpec.builder().description(Texts.of("List Mail Command")).permission("essentialcmds.mail.list")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.integer(Texts.of("page no")))))
				.executor(new MailListExecutor()).build();
		getGame().getCommandDispatcher().register(this, mailListCommandSpec, "listmail");

		CommandSpec mailReadCommandSpec =
			CommandSpec.builder().description(Texts.of("Read Mail Command")).permission("essentialcmds.mail.read")
				.arguments(GenericArguments.onlyOne(GenericArguments.integer(Texts.of("mail no")))).executor(new MailReadExecutor()).build();
		getGame().getCommandDispatcher().register(this, mailReadCommandSpec, "readmail");

		CommandSpec msgRespondCommandSpec =
			CommandSpec.builder().description(Texts.of("Respond to Message Command")).permission("essentialcmds.message.respond")
				.arguments(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("message"))))
				.executor(new RespondExecutor()).build();
		getGame().getCommandDispatcher().register(this, msgRespondCommandSpec, "r");

		CommandSpec timeCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Set Time Command"))
				.permission("essentialcmds.time.set")
				.arguments(
					GenericArguments.firstParsing(
						GenericArguments.string(Texts.of("time")), 
						GenericArguments.integer(Texts.of("ticks"))))
				.executor(new TimeExecutor()).build();
		getGame().getCommandDispatcher().register(this, timeCommandSpec, "time");

		CommandSpec repairCommandSpec =
			CommandSpec.builder().description(Texts.of("Repair Item in Player's Hand")).permission("essentialcmds.repair.use").executor(new RepairExecutor())
				.build();
		getGame().getCommandDispatcher().register(this, repairCommandSpec, "repair");

		CommandSpec mailCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Mail Command"))
				.permission("essentialcmds.mail.use")
				.arguments(GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.string(Texts.of("player")))),
					GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("message")))).executor(new MailExecutor())
				.build();
		getGame().getCommandDispatcher().register(this, mailCommandSpec, "mail");

		CommandSpec weatherCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Weather Command"))
				.permission("essentialcmds.weather.use")
				.arguments(GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.string(Texts.of("weather")))),
					GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.integer(Texts.of("duration")))))
				.executor(new WeatherExecutor()).build();
		getGame().getCommandDispatcher().register(this, weatherCommandSpec, "weather");

		CommandSpec mobSpawnCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Mob Spawn Command"))
				.permission("essentialcmds.mobspawn.use")
				.arguments(GenericArguments.seq(
					GenericArguments.onlyOne(GenericArguments.integer(Texts.of("amount")))),
					GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("mob name"))))
				.executor(new MobSpawnExecutor()).build();
		getGame().getCommandDispatcher().register(this, mobSpawnCommandSpec, "mobspawn", "entityspawn");

		CommandSpec enchantCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Enchant Command"))
				.permission("essentialcmds.enchant.use")
				.arguments(GenericArguments.seq(
					GenericArguments.onlyOne(GenericArguments.integer(Texts.of("level")))),
					GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("enchantment"))))
				.executor(new EnchantExecutor()).build();
		getGame().getCommandDispatcher().register(this, enchantCommandSpec, "enchant", "ench");

		CommandSpec banCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Ban Command"))
				.permission("essentialcmds.ban.use")
				.arguments(
					GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame())), GenericArguments
						.optional(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("reason"))))))
				.executor(new BanExecutor()).build();
		getGame().getCommandDispatcher().register(this, banCommandSpec, "ban");

		CommandSpec pardonCommandSpec =
			CommandSpec.builder().description(Texts.of("Unban Command")).permission("essentialcmds.unban.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("player")))).executor(new PardonExecutor()).build();
		getGame().getCommandDispatcher().register(this, pardonCommandSpec, "unban", "pardon");

		CommandSpec teleportPosCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Teleport Position Command"))
				.permission("essentialcmds.teleport.pos.use")
				.arguments(
					GenericArguments.seq(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"),
						getGame())))), GenericArguments.onlyOne(GenericArguments.integer(Texts.of("x"))),
					GenericArguments.onlyOne(GenericArguments.integer(Texts.of("y"))),
					GenericArguments.onlyOne(GenericArguments.integer(Texts.of("z")))).executor(new TeleportPosExecutor()).build();
		getGame().getCommandDispatcher().register(this, teleportPosCommandSpec, "tppos", "teleportpos", "teleportposition");
		
		CommandSpec teleportCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Teleport Command"))
				.permission("essentialcmds.teleport.use")
				.arguments(GenericArguments.seq(
					GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame())),
						GenericArguments.optional(GenericArguments.player(Texts.of("target"), getGame()))))
				.executor(new TeleportExecutor()).build();
		getGame().getCommandDispatcher().register(this, teleportCommandSpec, "tp", "teleport");

		CommandSpec kickCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Kick Command"))
				.permission("essentialcmds.kick.use")
				.arguments(GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame()))),
					GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("reason")))).executor(new KickExecutor())
				.build();
		getGame().getCommandDispatcher().register(this, kickCommandSpec, "kick");

		CommandSpec messageCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Message Command"))
				.permission("essentialcmds.message.use")
				.arguments(GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.player(Texts.of("recipient"), getGame()))),
					GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("message"))))
				.executor(new MessageExecutor()).build();
		getGame().getCommandDispatcher().register(this, messageCommandSpec, "message", "m", "msg", "tell");

		CommandSpec lightningCommandSpec =
			CommandSpec.builder().description(Texts.of("Lightning Command")).permission("essentialcmds.lightning.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame()))))
				.executor(new LightningExecutor()).build();
		getGame().getCommandDispatcher().register(this, lightningCommandSpec, "thor", "smite", "lightning");

		CommandSpec fireballCommandSpec =
			CommandSpec.builder().description(Texts.of("Fireball Command")).permission("essentialcmds.fireball.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame()))))
				.executor(new FireballExecutor()).build();
		getGame().getCommandDispatcher().register(this, fireballCommandSpec, "fireball", "ghast");
		
		CommandSpec sudoCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Sudo Command"))
				.permission("essentialcmds.sudo.use")
				.arguments(
					GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame())),
						GenericArguments.remainingJoinedStrings(Texts.of("command")))).executor(new SudoExecutor()).build();
		getGame().getCommandDispatcher().register(this, sudoCommandSpec, "sudo");

		CommandSpec createWorldCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Create World Command"))
				.permission("essentialcmds.world.create")
				.arguments(
					GenericArguments.seq(
						GenericArguments.onlyOne(GenericArguments.string(Texts.of("name"))),
						GenericArguments.onlyOne(GenericArguments.string(Texts.of("environment"))),
						GenericArguments.onlyOne(GenericArguments.string(Texts.of("gamemode"))),
						GenericArguments.onlyOne(GenericArguments.string(Texts.of("difficulty")))))
				.executor(new CreateWorldExecutor()).build();
		getGame().getCommandDispatcher().register(this, createWorldCommandSpec, "createworld");
		
		CommandSpec loadWorldCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Load World Command"))
				.permission("essentialcmds.world.load")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("name"))))
				.executor(new LoadWorldExecutor()).build();
		getGame().getCommandDispatcher().register(this, loadWorldCommandSpec, "loadworld", "importworld");

		CommandSpec afkCommandSpec =
			CommandSpec.builder().description(Texts.of("AFK Command")).permission("essentialcmds.afk.use").executor(new AFKExecutor()).build();
		getGame().getCommandDispatcher().register(this, afkCommandSpec, "afk");

		CommandSpec broadcastCommandSpec =
			CommandSpec.builder().description(Texts.of("Broadcast Command")).permission("essentialcmds.broadcast.use")
				.arguments(GenericArguments.remainingJoinedStrings(Texts.of("message"))).executor(new BroadcastExecutor()).build();
		getGame().getCommandDispatcher().register(this, broadcastCommandSpec, "broadcast");

		CommandSpec spawnCommandSpec =
			CommandSpec.builder().description(Texts.of("Spawn Command")).permission("essentialcmds.spawn.use").executor(new SpawnExecutor()).build();
		getGame().getCommandDispatcher().register(this, spawnCommandSpec, "spawn");

		CommandSpec setSpawnCommandSpec =
			CommandSpec.builder().description(Texts.of("Set Spawn Command")).permission("essentialcmds.spawn.set").executor(new SetSpawnExecutor()).build();
		getGame().getCommandDispatcher().register(this, setSpawnCommandSpec, "setspawn");

		CommandSpec tpaCommandSpec =
			CommandSpec.builder().description(Texts.of("TPA Command")).permission("essentialcmds.tpa.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame()))).executor(new TPAExecutor()).build();
		getGame().getCommandDispatcher().register(this, tpaCommandSpec, "tpa");

		CommandSpec tpaHereCommandSpec =
			CommandSpec.builder().description(Texts.of("TPA Here Command")).permission("essentialcmds.tpahere.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame()))).executor(new TPAHereExecutor())
				.build();
		getGame().getCommandDispatcher().register(this, tpaHereCommandSpec, "tpahere");

		CommandSpec tpWorldSpec =
			CommandSpec.builder().description(Texts.of("TP World Command")).permission("essentialcmds.tpworld.use")
				.arguments(GenericArguments.seq(GenericArguments.remainingJoinedStrings(Texts.of("name")),
					GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame())))))
				.executor(new TeleportWorldExecutor())
				.build();
		getGame().getCommandDispatcher().register(this, tpWorldSpec, "tpworld");

		CommandSpec tpHereCommandSpec =
			CommandSpec.builder().description(Texts.of("TP Here Command")).permission("essentialcmds.tphere.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame()))).executor(new TPHereExecutor())
				.build();
		getGame().getCommandDispatcher().register(this, tpHereCommandSpec, "tphere");

		CommandSpec tpaAcceptCommandSpec =
			CommandSpec.builder().description(Texts.of("TPA Accept Command")).permission("essentialcmds.tpa.accept").executor(new TPAAcceptExecutor()).build();
		getGame().getCommandDispatcher().register(this, tpaAcceptCommandSpec, "tpaccept");

		CommandSpec listHomeCommandSpec =
			CommandSpec.builder().description(Texts.of("List Home Command")).permission("essentialcmds.home.list")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.integer(Texts.of("page no")))))
				.executor(new ListHomeExecutor()).build();
		getGame().getCommandDispatcher().register(this, listHomeCommandSpec, "homes");

		CommandSpec listWorldsCommandSpec =
			CommandSpec.builder().description(Texts.of("List World Command")).permission("essentialcmds.worlds.list")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.integer(Texts.of("page no")))))
				.executor(new ListWorldExecutor()).build();
		getGame().getCommandDispatcher().register(this, listWorldsCommandSpec, "worlds");

		CommandSpec healCommandSpec =
			CommandSpec.builder().description(Texts.of("Heal Command")).permission("essentialcmds.heal.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame()))))
				.executor(new HealExecutor()).build();
		getGame().getCommandDispatcher().register(this, healCommandSpec, "heal");

		CommandSpec backCommandSpec =
			CommandSpec.builder().description(Texts.of("Back Command")).permission("essentialcmds.back.use").executor(new BackExecutor()).build();
		getGame().getCommandDispatcher().register(this, backCommandSpec, "back");

		CommandSpec tpaDenyCommandSpec =
			CommandSpec.builder().description(Texts.of("TPA Deny Command")).permission("essentialcmds.tpadeny.use").executor(new TPADenyExecutor()).build();
		getGame().getCommandDispatcher().register(this, tpaDenyCommandSpec, "tpadeny");

		CommandSpec hatCommandSpec =
			CommandSpec.builder().description(Texts.of("Hat Command")).permission("essentialcmds.hat.use").executor(new HatExecutor()).build();
		getGame().getCommandDispatcher().register(this, hatCommandSpec, "hat");

		CommandSpec flyCommandSpec =
			CommandSpec.builder().description(Texts.of("Fly Command")).permission("essentialcmds.fly.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("palyer"), getGame()))))
				.executor(new FlyExecutor()).build();
		getGame().getCommandDispatcher().register(this, flyCommandSpec, "fly");

		CommandSpec setHomeCommandSpec =
			CommandSpec.builder().description(Texts.of("Set Home Command")).permission("essentialcmds.home.set")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("home name")))).executor(new SetHomeExecutor()).build();
		getGame().getCommandDispatcher().register(this, setHomeCommandSpec, "sethome");

		CommandSpec deleteHomeCommandSpec =
			CommandSpec.builder().description(Texts.of("Delete Home Command")).permission("essentialcmds.home.delete")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("home name")))).executor(new DeleteHomeExecutor())
				.build();
		getGame().getCommandDispatcher().register(this, deleteHomeCommandSpec, "deletehome", "delhome");

		CommandSpec warpCommandSpec =
			CommandSpec.builder().description(Texts.of("Warp Command")).permission("essentialcmds.warp.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("warp name")))).executor(new WarpExecutor()).build();
		getGame().getCommandDispatcher().register(this, warpCommandSpec, "warp");

		CommandSpec listWarpCommandSpec =
			CommandSpec.builder().description(Texts.of("List Warps Command")).permission("essentialcmds.warps.list")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.integer(Texts.of("page no")))))
				.executor(new ListWarpExecutor()).build();
		getGame().getCommandDispatcher().register(this, listWarpCommandSpec, "warps");

		CommandSpec setWarpCommandSpec =
			CommandSpec.builder().description(Texts.of("Set Warp Command")).permission("essentialcmds.warp.set")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("warp name")))).executor(new SetWarpExecutor()).build();
		getGame().getCommandDispatcher().register(this, setWarpCommandSpec, "setwarp");

		CommandSpec deleteWarpCommandSpec =
			CommandSpec.builder().description(Texts.of("Delete Warp Command")).permission("essentialcmds.warp.delete")
				.arguments(GenericArguments.onlyOne(GenericArguments.string(Texts.of("warp name")))).executor(new DeleteWarpExecutor())
				.build();
		getGame().getCommandDispatcher().register(this, deleteWarpCommandSpec, "deletewarp", "delwarp");

		CommandSpec feedCommandSpec =
			CommandSpec.builder().description(Texts.of("Feed Command")).permission("essentialcmds.feed.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame()))))
				.executor(new FeedExecutor()).build();
		getGame().getCommandDispatcher().register(this, feedCommandSpec, "feed");

		CommandSpec unmuteCommnadSpec =
			CommandSpec.builder().description(Texts.of("Unmute Command")).permission("essentialcmds.unmute.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame()))).executor(new UnmuteExecutor())
				.build();
		getGame().getCommandDispatcher().register(this, unmuteCommnadSpec, "unmute");

		CommandSpec killCommandSpec =
			CommandSpec.builder().description(Texts.of("Kill Command")).permission("essentialcmds.kill.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame()))))
				.executor(new KillExecutor()).build();
		getGame().getCommandDispatcher().register(this, killCommandSpec, "kill");

		CommandSpec jumpCommandSpec =
			CommandSpec.builder().description(Texts.of("Jump Command")).permission("essentialcmds.jump.use").executor(new JumpExecutor()).build();
		getGame().getCommandDispatcher().register(this, jumpCommandSpec, "jump");

		CommandSpec speedCommandSpec =
			CommandSpec.builder().description(Texts.of("Speed Command")).permission("essentialcmds.speed.use")
				.arguments(GenericArguments.seq(
					GenericArguments.onlyOne(GenericArguments.integer(Texts.of("speed"))),
					GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame())))))
				.executor(new SpeedExecutor()).build();
		getGame().getCommandDispatcher().register(this, speedCommandSpec, "speed");

		CommandSpec powertoolCommandSpec =
			CommandSpec.builder().description(Texts.of("Powertool Command")).permission("essentialcmds.powertool.use")
				.arguments(GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("command")))))
				.executor(new PowertoolExecutor()).build();
		getGame().getCommandDispatcher().register(this, powertoolCommandSpec, "powertool");
		
		CommandSpec asConsoleCommandSpec =
			CommandSpec.builder().description(Texts.of("AsConsole Command")).permission("essentialcmds.asconsole.use")
				.arguments(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("command"))))
				.executor(new AsConsoleExecutor()).build();
		getGame().getCommandDispatcher().register(this, asConsoleCommandSpec, "asConsole", "asconsole");

		CommandSpec nickCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Nick Command"))
				.permission("essentialcmds.nick.use")
				.arguments(
					GenericArguments.seq(
						GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame()))),
						GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Texts.of("nick")))))
				.executor(new NickExecutor()).build();
		getGame().getCommandDispatcher().register(this, nickCommandSpec, "nick");

		CommandSpec muteCommandSpec =
			CommandSpec
				.builder()
				.description(Texts.of("Mute Command"))
				.permission("essentialcmds.mute.use")
				.arguments(
					GenericArguments.seq(GenericArguments.onlyOne(GenericArguments.player(Texts.of("player"), getGame())),
						GenericArguments.onlyOne(GenericArguments.optional(GenericArguments.integer(Texts.of("time")))),
						GenericArguments.onlyOne(GenericArguments.optional(GenericArguments.string(Texts.of("time unit"))))))
				.executor(new MuteExecutor()).build();
		getGame().getCommandDispatcher().register(this, muteCommandSpec, "mute");

		getGame().getEventManager().registerListeners(this, new SignChangeListener());
		getGame().getEventManager().registerListeners(this, new PlayerJoinListener());
		getGame().getEventManager().registerListeners(this, new MessageSinkListener());
		getGame().getEventManager().registerListeners(this, new PlayerClickListener());
		getGame().getEventManager().registerListeners(this, new PlayerInteractListener());
		getGame().getEventManager().registerListeners(this, new PlayerMoveListener());
		getGame().getEventManager().registerListeners(this, new PlayerDeathListener());
		getGame().getEventManager().registerListeners(this, new TPAListener());
		getGame().getEventManager().registerListeners(this, new MailListener());
		getGame().getEventManager().registerListeners(this, new PlayerDisconnectListener());

		getLogger().info("-----------------------------");
		getLogger().info("EssentialCmds was made by HassanS6000!");
		getLogger().info("Please post all errors on the Sponge Thread or on GitHub!");
		getLogger().info("Have fun, and enjoy! :D");
		getLogger().info("-----------------------------");
		getLogger().info("EssentialCmds loaded!");
	}

	public static ConfigurationLoader<CommentedConfigurationNode> getConfigManager()
	{
		return configurationManager;
	}

	public Path getConfigDir() {
		return configDir;
	}

	public Logger getLogger() {
		return logger;
	}

	public Game getGame() {
		return game;
	}

}
