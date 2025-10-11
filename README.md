# pv-reminder
A lightweight Plasmo Voice add-on that reminds players to install the mod when they join your server — no Plasmo Voice API required. Works before or after AuthMe login, or even without AuthMe entirely.

# 📥 Installation
1. Download the latest jar from the Versions tab.
1. Place it in your server’s /plugins folder.
1. Start your server to generate the config.
1. Edit config.yml to suit your needs.
# ⚙️ Configuration

```
# === PVCheckOnAuth Configuration ===
# Where to run the reminder check.
#   auto         -> use AuthMe if present (after-auth), otherwise run on join (before-auth)
#   after-auth   -> only run after successful AuthMe login (requires AuthMe to be installed)
#   before-auth  -> run on player join (works with or without AuthMe)
mode: auto

# Chat message shown to players who DON'T have Plasmo Voice detected.
# Use '&' color codes; {player} will be replaced with the player's name.
message: "&7Hey {player}! This server uses &aPlasmo Voice &7for proximity chat. Install it: &bmodrinth.com/mod/plasmo-voice"

# Delay after the trigger event before we check (in ticks; 20t = 1s)
auth-delay-ticks: 20     # used when mode=after-auth or mode=auto+AuthMe found
join-delay-ticks: 40     # used when mode=before-auth or mode=auto+AuthMe not found

# If true, send the reminder only once ever (persisted across restarts).
once-ever: true

# Detection settings
# If 'channels' list is non-empty, it will be used for exact matches.
# Otherwise, we fall back to the namespace prefix.
plasmovoice-channel-prefix: "plasmovoice:"
channels:
  - "plasmovoice:voice"
  - "plasmovoice:main"

```

# 🔌 Compatibility
Minecraft: 1.21.8+ (may work on older versions but not guaranteed) <br>
Servers: Paper, Purpur, Folia and forks
# 🧨 Commands
*/pvreminder* forget <player|uuid> — remove a player from the reminded list <br>
*/pvreminder* remind <player|uuid> — reminds a player and removes from the reminded list
