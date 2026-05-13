
# Fishing Village   
Simple fishing game built with Java as a purely "for fun" hobby project with the help of friends. Probably insanely unbalanced.


## Features

- Basic fishing minigame (cast > wait > bite > hook > catch)
- Weather (storms and rain affect catch rates)
- Day/Night (time passes and different fish appear more regularly at night)
- Fish rarities (five tiers from Common to Legendary and also Junk because of course)
- Rod upgrades (three rod types that improve base odds)
- Bait (currently three types that reduce junk rates and boost rarity chances)
- Inventory (limited capacity for catches, upgradeable)
- Shop (sell catches, buy gear)
- Journal (track catches, personal bests and achievements)
- Basic automatic save system

Everything is very barebones and nothing is guaranteed to work properly in it's current state.
## Stack

- Java 21
- LibGDX, LWJGL3
- Gradle
## Tests & Building

To build and run the game inside the IDE, run the following commands

Run:
```bash
  Run:
  ./gradlew :desktop:run
  Compile test:
  ./gradlew :core:compileJava
```
Catch simulation: 
```md
Flags: 
use --info for console output or check core/build/reports/tests/test/index.html
to use a random seed instead of 42, append -Dsim.random=true:

Test:
./gradlew :core:test --tests "org.example.CatchSimulation"
```
## Acknowledgements

 - Background Images: https://craftpix.net/freebies/ocean-and-clouds-free-pixel-art-backgrounds/
 - Background Music: https://pixabay.com/users/danielhren-16113816/
 - Fish, Character, Rod + Other Pixel Art Assets: Mako

