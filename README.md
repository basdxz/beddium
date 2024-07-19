<img src="src/main/resources/icon.png" width="128">

# Beddium

Beddium is a free and open-source performance mod for Minecraft clients. 

## NOTICE
This project is an (Un)Official fork of [Embeddium](https://github.com/embeddedt/embeddium) with support for Minecraft Forge (not NeoForge) on Minecraft versions 1.21+

Created with support from Embbeddedt, to allow upstream to focus on supporting the NeoForge modloaders.

Please do not report issues with Beddium to the maintainers of Embeddium or Sodium.

## Highlights

* Up-to date with upstream [Embeddium](https://github.com/embeddedt/embeddium) patches
* All performance improvements from Sodium 0.5.8 and earlier, i.e. a rewritten terrain renderer, various optimizations to the immediate-mode rendering pipeline (used by entities, GUIs, block entities, etc.), and other miscellaneous improvements
* Available for Minecraft Forge on 1.21 and newer
* Integrated Fabric Rendering API support (Indium is not required, and will not work with Beddium)
* Frequent patch updates to fix mod compatibility issues soon after being reported & reproduced
* Additional APIs for mod integration
* Optional support for translucency sorting (can be enabled in Video Settings)

## Credits

* Embeddedt for creating & maintaining the upstream
* JellySquid & the CaffeineMC team, for making Sodium in the first place, without which this project would not be possible
* Asek3, for the initial port to Forge
* XFactHD, for providing a list of gamebreaking Rubidium issues to start this work off, and for testing early builds
* Pepper, for their invaluable assistance with getting Forge lighting to work on Sodium 0.5
* @Houstonruss for the logo

## License

Beddium is licensed under the Lesser GNU General Public License version 3.

Portions of the option screen code are based on Reese's Sodium Options by FlashyReese, and are used under the terms of
the [MIT license](https://opensource.org/license/mit), located in `src/main/resources/licenses/rso.txt`. 
