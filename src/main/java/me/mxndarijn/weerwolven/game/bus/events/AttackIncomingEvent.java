// me.mxndarijn.weerwolven.game.bus.events.AttackIncomingEvent
package me.mxndarijn.weerwolven.game.bus.events;

import me.mxndarijn.weerwolven.game.GamePlayer;
import me.mxndarijn.weerwolven.game.bus.Cancellable;
import me.mxndarijn.weerwolven.game.bus.GameBusEvent;

/**
 * Fired for each proposed kill BEFORE it's committed.
 * Listeners may:
 *  - cancel()     -> fully block the attack (e.g., Doctor/Angel save)
 *  - redirectTo() -> change the target (e.g., Bodyguard intercepts)
 *
 * Typical flow:
 *   AttackIncomingEvent evt = new AttackIncomingEvent(source, target, PlayerDiedEvent.Cause.NIGHT_KILL);
 *   bus.post(evt);
 *   if (!evt.isCancelled()) {
 *       GamePlayer finalTarget = evt.getRedirectTarget() != null ? evt.getRedirectTarget() : target;
 *       queue.propose(finalTarget, source, evt.cause());
 *   }
 */
public final class AttackIncomingEvent implements GameBusEvent, Cancellable {

    private final GamePlayer source;                 // may be null (lynch/environment)
    private final GamePlayer originalTarget;         // proposed victim
    private final PlayerEliminatedEvent.Cause cause;       // intended cause if it lands

    private boolean cancelled;
    private GamePlayer redirectTarget;               // optional: new victim (e.g., Bodyguard)

    public AttackIncomingEvent(GamePlayer source,
                               GamePlayer originalTarget,
                               PlayerEliminatedEvent.Cause cause) {
        this.source = source;
        this.originalTarget = originalTarget;
        this.cause = (cause == null ? PlayerEliminatedEvent.Cause.OTHER : cause);
    }

    // ----- inputs -----
    public GamePlayer source()          { return source; }
    public GamePlayer originalTarget()  { return originalTarget; }
    public PlayerEliminatedEvent.Cause cause(){ return cause; }

    // ----- redirect / cancel controls -----
    /** Redirect the attack to another target (e.g., Bodyguard). Null clears redirect. */
    public void redirectTo(GamePlayer newTarget) { this.redirectTarget = newTarget; }
    public GamePlayer getRedirectTarget()        { return redirectTarget; }

    /** Block the attack entirely. */
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public boolean isCancelled()                { return cancelled; }

    // Convenience helpers
    public void allow()   { this.cancelled = false; this.redirectTarget = null; }
    public void block()   { this.cancelled = true;  this.redirectTarget = null; }
}
