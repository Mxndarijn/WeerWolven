package me.mxndarijn.weerwolven.game.orchestration.executor;

import me.mxndarijn.weerwolven.game.action.ActionIntent;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import nl.mxndarijn.mxlib.inventory.MxItemClicked;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;
import nl.mxndarijn.mxlib.item.Pair;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

/** executes an action, for example open menu or go outside */
public abstract class AbilityExecutor {
    public abstract CompletableFuture<List<ActionIntent>> execute(Game game, GamePlayer actor, long timeoutMs);
    public abstract List<ActionIntent> defaultExecute(Game game, GamePlayer actor, long timeoutMs);

}
