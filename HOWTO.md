# How to Create and Run a BattleJar Commander

This guide explains how to write a commander using the
[battlejar-client](https://github.com/mateusz-dykszak/battlejar-client) library,
what orders you can send, how the game behaves, and what data you will receive.

**Production API** -- Connect your commander to
[https://api.battlejar.it](https://api.battlejar.it)
(HTTP for registration, WebSocket for game state and orders).

**Web UI** -- Watch fights at
[https://ui.battlejar.it](https://ui.battlejar.it).
Append `?debug=true` to the URL for extra statistics and debug info.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Project Setup](#2-project-setup)
3. [The Commander Interface](#3-the-commander-interface)
4. [Game State and Entities](#4-game-state-and-entities)
5. [Orders](#5-orders)
6. [How Orders Are Handled](#6-how-orders-are-handled)
7. [Automatic Behaviours](#7-automatic-behaviours)
8. [Spacecraft Types](#8-spacecraft-types)
9. [Entity IDs and Ownership](#9-entity-ids-and-ownership)
10. [Registration and Game Configuration](#10-registration-and-game-configuration)
11. [Strategic Tips](#11-strategic-tips)
12. [Example Commander](#12-example-commander)
13. [Running Your Commander](#13-running-your-commander)
14. [Quick Reference](#14-quick-reference)

---

## 1. Overview

**battle.jar** is a multiplayer space battle game. Each player controls a
fleet of spacecraft -- one carrier, multiple fighters, and missiles. The
last carrier standing wins.

Your commander has three responsibilities:

- **Receive game state** -- The server sends periodic snapshots containing
  positions, velocities, and status of every entity in the game.
- **Send orders** -- You tell your spacecraft to move, turn, dock, attack,
  or fire missiles.
- **React to game phases** -- The game transitions through several states;
  your commander should only send orders during the `RUNNING` phase.

---

## 2. Project Setup

Add the client library as a dependency. The library is published to GitHub
Packages. See the
[client README](https://github.com/mateusz-dykszak/battlejar-client#using-as-a-dependency-github-packages)
for full repository setup and credentials.

### Gradle (Groovy)

```groovy
dependencies {
    implementation "it.battlejar:battlejar-api:$clientVersion"
    implementation "it.battlejar:battlejar-client:$clientVersion"
    // optional -- only if you need Vector2 and other math utilities
    implementation "it.battlejar:battlejar-math:$clientVersion"
}
```

### Maven

```xml
<dependency>
    <groupId>it.battlejar</groupId>
    <artifactId>battlejar-api</artifactId>
    <version>${clientVersion}</version>
</dependency>
<dependency>
    <groupId>it.battlejar</groupId>
    <artifactId>battlejar-client</artifactId>
    <version>${clientVersion}</version>
</dependency>
<!-- optional -->
<dependency>
    <groupId>it.battlejar</groupId>
    <artifactId>battlejar-math</artifactId>
    <version>${clientVersion}</version>
</dependency>
```

> **Stability note:** Until version 1.0.0 the client should be considered
> unstable. Even minor versions may contain breaking changes.

---

## 3. The Commander Interface

Implement the `Commander` interface from the `client` module. It has three
methods, called in a specific order.

### Lifecycle Summary

| Step | Method | When |
|------|--------|------|
| 1 | `process(RegistrationResponse)` | After registration succeeds. Receive your colour and game settings. |
| 2 | `setOrdersSender(Consumer<Order>)` | Immediately after step 1. Receive the callback for sending orders. |
| 3 | `process(Entities)` | Repeatedly, once per game update cycle. Returns `true` to keep playing. |

### AbstractCommander (Recommended)

The `AbstractCommander` class handles the boilerplate of steps 1 and 2
and only calls your code during the `RUNNING` game phase. It provides:

- `myColor` -- your assigned `Color`
- `settings` -- the `GameSettings` for the current game
- `order(Order)` -- sends an order to the server

Extend it and implement the `process(Collection<Entity>)` method with
your strategy logic. Return `true` to keep playing, `false` to quit.

---

## 4. Game State and Entities

Each update delivers an `Entities` object containing a collection of
`Entity` records, the current game `state`, and a `timeStamp` (`Instant`)
indicating when the snapshot was created.

### Game States

| State | Meaning |
|-------|---------|
| `INITIALIZING` | Server is setting up |
| `PRE_REGISTRATION` | Preparing for registration |
| `REGISTRATION` | Players can register |
| `STARTING` | Game starting soon |
| `RUNNING` | **Active gameplay -- send orders now** |
| `ENDING` | Victory condition met |
| `CLEANING` | Cleanup before next game |

> **Note:** `BattleJarClient` automatically leaves the game when it
> receives `ENDING`, `CLEANING`, or `INITIALIZING` states. Your
> commander's `process` method will not be called with these states.
> If you extend `AbstractCommander`, your `process(Collection<Entity>)`
> method is only called during the `RUNNING` phase.

### Entity Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Unique entity identifier |
| `type` | `Entity.Type` | `FIGHTER`, `CARRIER`, or `MISSILE` |
| `color` | `String` | Owner colour (e.g. `"RED"`, `"BLUE"`) |
| `px`, `py` | `float` | Position |
| `vx`, `vy` | `float` | Velocity |
| `shot` | `String` | `"true"` or `"false"` when shooting; `null` or `""` when not shooting |
| `sx`, `sy` | `float` | Shot target position (when shooting) |
| `missiles` | `int` | Number of missiles carried |
| `status` | `String` | Status code (see below) |

### Entity Status Codes

| Status | Meaning |
|--------|---------|
| Numeric (e.g. `"100"`, `"10"`) | Current health |
| `"C"` | Docked to carrier |
| `"D"` | Destroyed (or missile inactive/ready) |
| `"A"` | Missile armed |
| `"E"` | Missile exploding / exploded |

### Identifying Your Units

Your entities share the colour assigned during registration. Filter by
colour to find your fleet. Entity colours are `String` values matching
the `Color` enum names (e.g. `"RED"`), so compare using
`myColor.name().equals(entity.color())`.

---

## 5. Orders

Orders are sent using the `Order` record. Each order targets one entity by
its id.

The order sender is a `Consumer<Order>` provided to your commander via
`setOrdersSender(Consumer<Order>)` during initialisation (see
[lifecycle](#lifecycle-summary)). If you implement `Commander` directly,
store this consumer and call its `accept` method to send orders. If you
extend `AbstractCommander`, the consumer is stored for you and you can
use the convenience `order(Order)` method instead.

```java
// With details
order(new Order("RED-01", OrderType.MOVE, "50|30"));

// Without details
order(new Order("RED-01", OrderType.DOCK));
```

### Order Types

| Type | Applies to | Details | Description |
|------|------------|---------|-------------|
| `MOVE` | Fighter, Carrier | `"x\|y"` -- bearing relative to own carrier | Move in a given direction |
| `TURN_XY` | Fighter, Carrier | `"x\|y"` -- direction vector | Turn toward a given direction |
| `DOCK` | Fighter | *(none)* | Dock with your carrier |
| `PATROL` | Fighter, Carrier | *(none)* | Continuous rotation patrol |
| `TARGET` | Fighter, Carrier | `"F"`, `"M"`, or `"C"` | Restrict which enemy type to target |
| `ATTACK` | Fighter, Carrier | *(optional)* enemy entity id | Attack and fire when possible |
| `FIRE_MISSILE` | Fighter, Carrier | *(optional)* `"x\|y"` direction (carriers); fighters fire along current heading | Fire a carried missile |

### MOVE

Moves the spacecraft toward a position. **The coordinates are relative to
your own carrier's position, not absolute world coordinates.** This is the
most common mistake.

MOVE is a long-lasting command -- the entity continues moving toward the
target even after temporary distractions such as collision avoidance.

```java
// Move 50 units right and 30 units up from your carrier
order(new Order("RED-01", OrderType.MOVE, "50|30"));

// Move toward carrier (relative 0, 0)
order(new Order("RED-01", OrderType.MOVE, "0|0"));
```

### TURN_XY

Turns the spacecraft to face a direction vector. This is a low-priority
command that can be overridden by automatic behaviours such as collision
avoidance or targeting. Sending TURN_XY too frequently can cause neither
your commands nor the automated ones to complete.

```java
// Turn to face right
order(new Order("RED-01", OrderType.TURN_XY, "1|0"));

// Turn toward a point -- compute direction from current position
float dx = target.px() - fighter.px();
float dy = target.py() - fighter.py();
order(new Order("RED-01", OrderType.TURN_XY, dx + "|" + dy));
```

### DOCK

Commands a fighter to return to and dock with its carrier.

```java
order(new Order("RED-01", OrderType.DOCK));
```

- Docked units have status `"C"`.
- Sending **any** order to a docked unit automatically undocks it.
  Undocking happens at a configured rate, so if many docked units
  receive orders at the same time they will be released gradually,
  not all at once.

### PATROL

Makes the spacecraft circle around its current position. Automatic
behaviours such as collision avoidance and targeting still apply, so
the entity may drift from the original point over time.

```java
order(new Order("RED-01", OrderType.PATROL));
```

### TARGET

Restricts which enemy type the entity will engage during combat.

```java
order(new Order("RED-01", OrderType.TARGET, "F")); // fighters only
order(new Order("RED-01", OrderType.TARGET, "M")); // missiles only
order(new Order("RED-01", OrderType.TARGET, "C")); // carriers only
```

### ATTACK

Engages enemies. The game also triggers attack behaviour automatically
when enemies are in range.

```java
// Attack nearest eligible enemy
order(new Order("RED-01", OrderType.ATTACK));

// Attack a specific entity
order(new Order("RED-01", OrderType.ATTACK, "BLUE-02"));
```

### FIRE_MISSILE

Fires a missile carried by the spacecraft.

```java
// Fighter -- fires along current heading
order(new Order("RED-01", OrderType.FIRE_MISSILE));

// Carrier -- optionally specify a direction
order(new Order("RED-00", OrderType.FIRE_MISSILE, "100|50"));
```

---

## 6. How Orders Are Handled

- **One order per entity at a time** -- The server processes one order
  per entity at a time. If a spacecraft receives a second order while the
  first is still being processed, the first order is discarded and
  processing restarts from scratch -- even if both orders are identical.
  In the most basic case, repeatedly sending the same MOVE order can
  cause the entity to never actually move, because each new order
  interrupts the previous one before it finishes being translated into
  detailed instructions.
- **Orders may be dropped** -- When multiple orders arrive for the same
  entity in very quick succession, the second and all subsequent orders
  may be silently ignored.
- **No fixed timing rule** -- There is no exact rule for how long to wait
  before sending a new order. Consider implementing a per-entity cooldown
  to avoid overriding orders too quickly; see the
  [example commander](#12-example-commander) for a simple approach.
- **Invalid or unknown type** -- rejected.
- **Missing or blank id or type** -- rejected.
- **Non-existent entity id** -- silently ignored.
- **Orders to destroyed units** -- no effect.

---

## 7. Automatic Behaviours

The game applies certain behaviours every tick, regardless of your orders.

| Behaviour | Description |
|-----------|-------------|
| Collision avoidance | Non-carrier, non-missile spacecraft may turn away when on a collision course. |
| Targeting and firing | Fighters and carriers pick targets in range. `TARGET` filters by enemy type. Fighters must align before firing; carriers fire automatically on enemies in range. |
| Missiles | Once fired, missiles are steered by the game and do not respond to commander orders. |
| Missile arming | Fired missiles start unarmed. After a delay they become armed (status `"A"`). |
| Missile explosion | Armed missiles can explode on contact and damage entities in a radius with falloff. |
| Docking | `DOCK` moves a fighter toward its carrier. Arriving completes the dock and clears orders. Issuing a new order undocks the fighter. |
| Border | Spacecraft do not automatically avoid the border. Leaving the play area destroys the entity -- you must send orders to keep your units inside. |
| Collision | Overlap can cause docking (ally + carrier), missile explosion, or damage. |
| Carrier factory | Carriers produce fighters and missiles over time. |

---

## 8. Spacecraft Types

### Carrier

Your main ship. If the carrier is destroyed, the player is eliminated.

- Has a turbo laser that fires automatically on enemies in range (no
  manual aiming needed).
- Produces fighters and missiles over time (factory).
- Can dock fighters and missiles (limited capacity).
- Can fire big missiles in any direction.

### Fighter

Primary combat unit. Faster and more agile than carriers.

- Must turn to aim its laser at targets.
- Carries one small missile that can be released.
- Can dock to its carrier for protection and missile reload.

### Missile

Explosive projectile that auto-targets enemy carriers once fired.

- Starts unarmed, becomes armed after a delay.
- Guided by the game -- does not accept commander orders after firing.
- Explodes on contact; damage decreases with distance from the blast
  centre.

---

## 9. Entity IDs and Ownership

### ID Format

| Type | Format | Examples |
|------|--------|----------|
| Carrier | `<COLOR>-00` | `RED-00`, `BLUE-00` |
| Fighter | `<COLOR>-<NN>` | `RED-01`, `BLUE-05` |
| Missile | `M<explosion-radius>-<COLOR>-<NN>` | `M5-RED-01`, `M10-BLUE-03` |

The number in the missile prefix indicates the explosion radius of
that missile type.

### Ownership

- **Your colour** identifies your entities. Send orders only for
  entity ids that match your colour.
- **Source of truth** -- Always take entity ids from the latest `Entities`
  snapshot. Do not invent or cache stale ids.
- **ID reuse** -- In rare conditions, fighter and missile ids may be
  reused. If you track ids across updates, make sure to clean them up
  when the entity is destroyed.

---

## 10. Registration and Game Configuration

### RegistrationResponse

After registration, you receive a `RegistrationResponse` record:

| Field | Type | Description |
|-------|------|-------------|
| `gameId` | `UUID` | Unique identifier for the current game |
| `playerId` | `UUID` | Your assigned player identifier |
| `color` | `Color` | Your assigned colour (see below) |
| `gameSettings` | `GameSettings` | Current game configuration |

### Color

`Color` is an enum with the following values: `RED`, `BLUE`, `GREEN`,
`VIOLET`, `ORANGE`, `WHITE`.

Entity colours in the `Entity` record are represented as `String` values
matching these enum names (e.g. `"RED"`, `"BLUE"`).

### Player

The `Player` record is used for registration:

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Player identifier (pass `null` for auto-assignment) |
| `color` | `Color` | Preferred colour (pass `null` for auto-assignment) |
| `username` | `String` | Display name for your commander |

### GameSettings

At registration you receive a `GameSettings` object containing
runtime-configurable values. **Always read these dynamically; do not
hardcode them.**

| Field | Description |
|-------|-------------|
| `worldWidth` | Width of the game world |
| `worldHeight` | Height of the game world |
| `fighterBody` | Fighter health |
| `fighterSize` | Fighter size (radius) |
| `carrierBody` | Carrier health |
| `carrierSize` | Carrier size (radius) |
| `missileBaseBody` | Base missile health |
| `missileSize` | Missile size (radius) |

Other gameplay properties (speeds, turn rates, weapon stats, cooldowns,
explosion properties) are **not** available through `GameSettings`. Treat
them as observable behaviour that you can learn during gameplay rather
than as fixed constants.

---

## 11. Strategic Tips

- **Protect your carrier** -- It is the victory condition. Keep fighters
  between your carrier and the enemy.
- **Stay away from edges** -- Crossing the world boundary destroys the
  entity.
- **Use MOVE coordinates correctly** -- They are relative to your own
  carrier, not absolute.
- **Don't spam orders** -- One order per entity at a time. Rapid
  re-ordering causes the entity to keep switching tasks.
- **Dock damaged fighters** -- They survive inside the carrier and can
  be re-deployed later.
- **Fire missiles wisely** -- Big carrier missiles deal heavy damage;
  small fighter missiles are best at close range.
- **Intercept enemy missiles** -- Use `TARGET "M"` on fighters near your
  carrier to shoot down incoming missiles.

---

## 12. Example Commander

A minimal commander that sends all active fighters to attack. It includes
a simple per-entity cooldown to prevent orders from being overridden
before the server can execute them. Sending orders too frequently for the
same entity causes the previous order to be replaced before it takes
effect.

```java
import it.battlejar.api.*;
import it.battlejar.client.AbstractCommander;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AttackExample extends AbstractCommander {

    private static final long ORDER_COOLDOWN_MS = 100;
    private final Map<String, Long> lastOrderTime = new HashMap<>();

    @Override
    protected boolean process(Collection<Entity> entities) {
        entities.stream()
            .filter(e -> myColor.name().equals(e.color()))
            .filter(e -> e.type() == Entity.Type.FIGHTER)
            .filter(e -> !"D".equals(e.status()) && !"C".equals(e.status()))
            .forEach(f -> rateLimitedOrder(new Order(f.id(), OrderType.ATTACK)));

        return true;
    }

    private void rateLimitedOrder(Order order) {
        long now = System.currentTimeMillis();
        if (now - lastOrderTime.getOrDefault(order.id(), 0L) < ORDER_COOLDOWN_MS) {
            return;
        }
        lastOrderTime.put(order.id(), now);
        order(order);
    }
}
```

---

## 13. Running Your Commander

### Single Game

```java
import it.battlejar.api.Player;
import it.battlejar.client.BattleJarClient;

Player player = new Player(null, null, "mycommander");

try (BattleJarClient client = new BattleJarClient("https://api.battlejar.it", new AttackCommander())) {
    client.register(player);
    client.process(); // blocks until the game ends
}
```

### Continuous (Multi-Game Loop)

```java
import it.battlejar.api.Player;
import it.battlejar.client.BattleJarContinuous;
import java.util.concurrent.Executors;

Player player = new Player(null, null, "mycommander");

try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    BattleJarContinuous continuous = new BattleJarContinuous(
        "https://api.battlejar.it",
        player,
        AttackCommander::new,
        executor
    );
    continuous.run(); // blocks, re-registers after each game
}
```

> Pass `null` for `id` and `color` in `Player` and the server will assign
> them automatically. See [Player](#player) for field details.

---

## 14. Quick Reference

### Order Cheat Sheet

| Action | Code |
|--------|------|
| Move to position | `new Order(id, OrderType.MOVE, "x\|y")` |
| Turn to direction | `new Order(id, OrderType.TURN_XY, "x\|y")` |
| Dock to carrier | `new Order(id, OrderType.DOCK)` |
| Patrol | `new Order(id, OrderType.PATROL)` |
| Target fighters | `new Order(id, OrderType.TARGET, "F")` |
| Target missiles | `new Order(id, OrderType.TARGET, "M")` |
| Target carriers | `new Order(id, OrderType.TARGET, "C")` |
| Attack | `new Order(id, OrderType.ATTACK)` |
| Fire missile | `new Order(id, OrderType.FIRE_MISSILE)` |

### Status Codes

| Status | Meaning |
|--------|---------|
| Numeric | Current health |
| `C` | Docked |
| `D` | Destroyed |
| `A` | Missile armed |
| `E` | Missile exploding |

### Utility Snippets

```java
private boolean isDocked(Entity e) {
    return "C".equals(e.status());
}

private boolean isDestroyed(Entity e) {
    return "D".equals(e.status());
}

private boolean isAlive(Entity e) {
    return !isDestroyed(e);
}

private int health(Entity e) {
    try {
        return Integer.parseInt(e.status());
    } catch (NumberFormatException ex) {
        return -1; // non-numeric status (docked, destroyed, etc.)
    }
}

private float distance(Entity a, Entity b) {
    float dx = a.px() - b.px();
    float dy = a.py() - b.py();
    return (float) Math.sqrt(dx * dx + dy * dy);
}
```

---

**See also:**
- [Client library repository](https://github.com/mateusz-dykszak/battlejar-client) -- dependency setup, building from source
- [Game web UI](https://ui.battlejar.it) -- watch your commander play in real time
- [battlejar.it](https://battlejar.it) -- project home page
