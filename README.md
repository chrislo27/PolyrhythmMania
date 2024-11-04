# Polyrhythm Mania
A game that faithfully recreates the *Polyrhythm* minigame from Rhythm Tengoku (リズム天国), with a 
built-in level editor and side modes.

![Polyrhythm Mania thumbnail](https://user-images.githubusercontent.com/6299069/144956042-654ff2b3-aeba-4486-810e-f26aa1b33bff.png)
![Polyrhythm: Assemble side mode thumbnail](https://user-images.githubusercontent.com/6299069/140859874-0552bb9a-c6dc-460b-a4a2-e35f99648ea9.png)

[![Downloads](https://img.shields.io/github/downloads/chrislo27/PolyrhythmMania/total.svg)](https://github.com/chrislo27/PolyrhythmMania/releases/latest)
[![GitHub license](https://img.shields.io/github/license/chrislo27/PolyrhythmMania.svg)](https://github.com/chrislo27/PolyrhythmMania/blob/dev/LICENSE.txt)
[![Donate to the project maintainer](https://img.shields.io/badge/Donate-PayPal-blue.svg?logo=paypal)](https://www.paypal.com/donate?hosted_button_id=9JLGHKZNWLLQ8)

[Original Soundtrack on YouTube by GENERIC](https://www.youtube.com/playlist?list=PLt_3dgnFrUPwcA6SdTfi0RapEBdQV64v_)

## Features
* Full re-creation of the Polyrhythm minigame from Rhythm Tengoku, *and more!*
* A full *Story Mode* with over 40 levels
* Sort and organize your custom levels using the Library feature
* Create custom levels with a built-in and intuitive level editor
* Level effects such as changing the colour palette, adding text boxes, putting Skill Stars, and more
* Switch between classic GBA, modern HD, and arcade textures, or add your own custom texture pack
* Practice modes to improve your Polyrhythm skills with levels from the original GBA game
* Endless Mode keeps you on your toes with never-ending procedurally generated patterns
* Daily Challenge mode which has a series of patterns not seen in regular Endless Mode, with online leaderboards
* Extra side modes like _Polyrhythm: Dunk_, _Polyrhythm: Assemble_, and _Built to Scale: Solitaire_ add more Polyrhythm fun

[Check out the v1.2 update trailer on YouTube!](https://www.youtube.com/watch?v=I4BXu7sNj-M)  
[Check out the announcement trailer on YouTube!](https://www.youtube.com/watch?v=A3ZUBIy_MAQ)  
[Check out the August 2021 features trailer!](https://www.youtube.com/watch?v=k9PtPI1-tDo)  


## System requirements to play
**Officially supported operating systems:** Windows 10 or newer, Linux (x86-64 and ARM). 

> macOS is not officially supported (i.e. we don't accept bug reports from macOS) but you can attempt to run it per [issue #29](https://github.com/chrislo27/PolyrhythmMania/issues/29).

**System Specifications:** [Similar requirements as *Minecraft: Java Edition*](https://help.minecraft.net/hc/en-us/articles/4409225939853-Minecraft-Java-Edition-Installation-Issues-FAQ#h_01FFJMSQWJH31CH16H63GX4YKE)
are used, since the same underlying graphics library (GLFW with OpenGL 4.4) is used.

### Minimum requirements
* CPU: Intel Core i3-3210 3.2 GHz / AMD A8-7600 APU 3.1 GHz or equivalent 
* RAM: 4GB 
* GPU (Integrated): Intel HD Graphics 4000 (Ivy Bridge) or AMD Radeon R5 series (Kaveri line) with OpenGL 4.4
* GPU (Discrete): NVIDIA GeForce 400 Series or AMD Radeon HD 7000 series with OpenGL 4.4 
* HDD: At least 1GB for game and levels 
* OS: Windows 10 and up, Linux 64-bit distributions from 2018 onwards
* Display: 1280x720 resolution

### Recommended requirements
* CPU: Intel Core i5-4690 3.5GHz / AMD A10-7800 APU 3.5 GHz or equivalent 
* RAM: 8GB 
* GPU: NVIDIA GeForce 700 Series or AMD Radeon Rx 200 Series (excluding integrated chipsets) with OpenGL 4.5 
* HDD: 2GB (SSD is recommended) 
* OS: Windows 10 and newer (64-bit), Linux 64-bit distributions from 2020 onwards (e.g. Ubuntu 20.04) 
* Display: 1920x1080 resolution

## Installation instructions
These instructions are the same if you're downloading the game for the first time or upgrading to a new version.
Make sure that you meet the system requirements above.

__Windows (64-bit only, no Java installation required):__
1. Navigate to the [latest release here](https://github.com/chrislo27/PolyrhythmMania/releases/latest) in a new tab or window.
2. Download the correct version of the game in the Assets section named `PolyrhythmMania_VERSION_win64.zip`, where VERSION is the release version name. **Note the "win64" in the file name.** Don't download the "Source code".
3. Find the downloaded zip file in File Explorer. Extract the zip file: right click the downloaded zip file, click "Extract All...", and extract the contents to a folder.
4. Open the newly extracted folder and go into the `PolyrhythmMania_win64` folder.
5. Double-click the `LaunchPolyrhythmMania.exe` executable to start the game! (You don't need the Java Runtime Environment already installed, the game comes with a copy.)
6. If Windows Defender says "Windows Defender SmartScreen prevented an unrecognized app from starting", you can safely ignore it by clicking "More info" and then "Run anyway".

__Other platforms (or for advanced users who already have Java installed)__
1. **(Pre-requisite)** Ensure you already have the Java Runtime Environment (JRE 17 recommended, JRE 11 minimum) installed and it is accessible in your path as the `java` command.
2. Navigate to the [latest release here](https://github.com/chrislo27/PolyrhythmMania/releases/latest) in a new tab or window.
3. Download the correct version of the game in the Assets section named `PolyrhythmMania_VERSION.zip`, where VERSION is the release version name. Don't download the "Source code".
4. Extract the zip file to a known location.
5. Open the newly extracted directory and go into the `PolyrhythmMania_platform_agnostic` directory.
6. Run the appropriate launch script: On Windows, double click `play_windows.bat`. On Linux, run the `play_linux.sh` file (you may have to `chmod +x play_linux.sh` first).
7. If you prefer not to use a launch script, you can run `java -jar bin/PolyrhythmMania.jar` with your preferred settings.

## Compilation instructions
These instructions are for people interested in editing the source code of the game.

1. Ensure JDK 17 or newer is installed.
2. `chmod +x gradlew`
3. `./gradlew :desktop:run`

## Other information
Rhythm Heaven is the intellectual property of Nintendo.
This program is **NOT** endorsed nor sponsored in any way by Nintendo.
All used properties of Nintendo (such as names, audio, graphics, etc.) in this software are not intended to maliciously infringe trademark rights.
All other trademarks and assets are property of their respective owners.
This is a community project and this is available for others to use
according to the [GPL-3.0 license](LICENSE), without charge.
