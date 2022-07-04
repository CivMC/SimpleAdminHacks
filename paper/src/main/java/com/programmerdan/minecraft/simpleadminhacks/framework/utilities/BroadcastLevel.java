package com.programmerdan.minecraft.simpleadminhacks.framework.utilities;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Various Broadcast targets
 *
 * @author ProgrammerDan
 */
public enum BroadcastLevel {

	OP {
		@Override
		public int sendMessage(final @NotNull Component message) {
			return (int) Bukkit.getOperators()
					.stream()
					.map(OfflinePlayer::getPlayer)
					.filter(Objects::nonNull)
					.peek((final Player player) -> player.sendMessage(message))
					.count();
		}
	},

	PERM {
		@Override
		public int sendMessage(final @NotNull Component message) {
			return Bukkit.broadcast(message, SimpleAdminHacks.instance().config().getBroadcastPermission());
		}
	},

	CONSOLE {
		@Override
		public int sendMessage(final @NotNull Component message) {
			Bukkit.getConsoleSender().sendMessage(message);
			return 1;
		}
	},

	ALL {
		@Override
		public int sendMessage(final @NotNull Component message) {
			return Bukkit.broadcast(message);
		}
	};

	/**
	 * Sends a message to the intended targets.
	 *
	 * @param message The message to send.
	 * @return Returns how many recipients received the message.
	 */
	public abstract int sendMessage(final @NotNull Component message);

	/**
	 * Sends a message to the intended targets.
	 *
	 * @param message The message to send.
	 * @return Returns how many recipients received the message.
	 *
	 * @deprecated Use {@link #sendMessage(Component)} instead.
	 */
	@Deprecated
	public final int sendMessage(final @NotNull String message) {
		return sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
	}

}
