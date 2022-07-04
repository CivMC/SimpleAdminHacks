package com.programmerdan.minecraft.simpleadminhacks.configs;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHackConfig;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.chat.ChatUtils;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;
import vg.civcraft.mc.civmodcore.utilities.CivLogger;

public final class DogFactsConfig extends SimpleHackConfig {

	private static final CivLogger LOGGER = CivLogger.getLogger(DogFactsConfig.class);

	private List<Component> announcements;
	private long intervalTime;

	public DogFactsConfig(
			final @NotNull SimpleAdminHacks plugin,
			final @NotNull ConfigurationSection base
	) {
		super(plugin, base);
	}

	@Override
	protected void wireup(
			final @NotNull ConfigurationSection config
	) {
		// Announcements
		this.announcements = ConfigHelper.getStringList(config, "announcements")
				.parallelStream()
				.map((final String raw) -> {
					if (StringUtils.isBlank(raw)) {
						LOGGER.warning("Empty DogFact at [" + config.getCurrentPath() + "]");
						return null;
					}
					final Component component;
					try {
						component = MiniMessage.get().parse(raw);
					}
					catch (final Throwable thrown) {
						LOGGER.warning("Could not parse DogFact at [" + config.getCurrentPath() + "]");
						return null;
					}
					//noinspection deprecation # The deprecation is a LIE! Accidental leftover from move to Kyori
					if (ChatUtils.isNullOrEmpty(component)) {
						LOGGER.warning("Wordless DogFact at [" + config.getCurrentPath() + "]");
						return null;
					}
					return component;
				})
				.filter(Objects::nonNull)
				.toList();
		// Interval Time
		this.intervalTime = ConfigHelper.parseTimeAsTicks(Objects.requireNonNullElse(
				config.getString("intervalTime"), "30m"
		));
	}

	public @NotNull List<Component> getAnnouncements() {
		return this.announcements;
	}

	public long getIntervalInTicks() {
		return this.intervalTime;
	}

}
