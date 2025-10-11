# pv-reminder
A lightweight Plasmo Voice add-on that reminds players to install the mod when they join your server — no Plasmo Voice API required. Works before or after AuthMe login, or even without AuthMe entirely.

# 📥 Installation
1. Download the latest jar from the Versions tab.
1. Place it in your server’s /plugins folder.
1. Start your server to generate the config.
1. Edit config.yml to suit your needs.
# ⚙️ Configuration

```
mode: auto          # auto | after-auth | before-auth
message: "&7Hey {player}! This server uses &aPlasmo Voice &7for proximity chat. Install it: &bmodrinth.com/plugin/plasmo-voice"

auth-delay-ticks: 20   # Delay when using after-auth (or auto + AuthMe present)
join-delay-ticks: 40   # Delay when using before-auth (or auto + AuthMe missing)

once-per-session: true # Send reminder only once per session

plasmovoice-channel-prefix: "plasmovoice:"
channels:
  - "plasmovoice:voice"
  - "plasmovoice:main"
```

# 🔌 Compatibility
Minecraft: 1.21.8+ (may work on older versions but not guaranteed) <br>
Servers: Paper, Purpur, Folia and forks
