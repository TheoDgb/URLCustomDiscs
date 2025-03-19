## 1.21 URLCustomDiscs plugin (+ server resource pack)
Last updated on March 18, 2025.

## About
This plugin, along with a required server resource pack, allows you to create and play custom music discs from YouTube URLs on your Minecraft server, with real-time updates for players.

Once installed, everything is done in-game, and there's no need to manually edit the resource pack files to add or remove discs.

The plugin supports Minecraft's spatial audio for music, but you can also play it in stereo.

Additionally, Vanilla commands such as `/playsound` and `/stopsound` work with the custom music, so you do not need to use a disc in a jukebox.

Important:
- Currently only works on Windows due to dependencies.
- Make sure to use a direct video link without parameters (such as playlist, timecode, etc.), or you might be in for a surprise. Delete everything starting from & in the URL.

Note: plugin tested on 1.21.0 Spigot server

## Commands Overview
Display the list of commands:<br>
`/customdisc help`<br><br>
Create a custom music disc from a YouTube URL:<br>
`/customdisc create <URL> <disc name> <mono/stereo>`
- mono: enables spatial audio (like played in a jukebox)
- stereo: plays the sound in the traditional way

Give a custom music disc:<br>
`/customdisc give <disc name>`<br><br>
Display custom music discs list (you can give yourself a music disc directly by clicking on its name in the chat):<br>
`/customdisc list`<br><br>
Delete a custom music disc:<br>
`/customdisc delete <disc name>`<br><br>
Display custom music disc details in hand (used for debugging):<br>
`/customdisc info`

<br>

Vanilla command to play a custom track (can be used with coordinates):<br>
`/execute positioned ~ ~ ~ run playsound minecraft:customdisc.<disc name> ambiant @a`

Vanilla command to stop a custom track:<br>
`/stopsound @a * minecraft:customdisc.<disc name>`

## Dependencies
- A **Spigot Minecraft server** (the plugin has not been tested on other server types).
- A **personal web server** hosting the resource pack, which allows:
  - the plugin to access the resource pack via an absolute path for editing;
  - players to download the resource pack and receive real-time updates for custom music discs.
- **yt-dlp** to download an MP3 audio file from a YouTube URL.
- **FFmpeg** to convert MP3 format to Ogg format.

### License and Attribution
This plugin uses **yt-dlp** ([unlicense](https://github.com/yt-dlp/yt-dlp/blob/master/LICENSE)) and **FFmpeg** from the [FFmpeg.org](http://ffmpeg.org/) under the [LGPLv2.1](https://www.gnu.org/licenses/lgpl-2.1.html).<br>
yt-dlp and FFmpeg are not included in this project and must be installed separately.

## Download
The **URLCustomDiscs.jar** plugin and the **URLCustomDiscsPack.zip** server resource pack are available for download in the [**Releases**](https://github.com/TheoDgb/URLCustomDiscs/releases) section.

## Servers Guide
If you already have a Minecraft server and a personal web server that allows downloading a resource pack via URL, you can skip this section.

<details>
<summary>Tutorial for setting up a personal web server</summary>

First, you'll need a [1.21 Spigot Minecraft server](https://getbukkit.org/get/4063d239ce16b22d948c037ce7a9fb8c).
- This topic is not covered here.

Second, you need to host the URLCustomDiscsPack.zip resource pack on a personal web server that the plugin can access via an absolute path. Using an online file hosting service (such as [MCPacks](https://mc-packs.net/)) will not work.

Here's a tutorial to create an Apache server on Windows:
- Access your router's configuration interface to:
  - configure a NAT/PAT rule for TCP port forwarding, setting both internal and external ports to 80, and using the public IP address of the machine running the Apache HTTP web server (you can quickly find it on websites like [WhatIsMyIp.com](https://www.whatismyip.com/));
  - open TCP port 80, which is the default for HTTP traffic, in your firewall to allow incoming connections.
- Download Apache from [Apache Lounge](https://www.apachelounge.com/download/) (httpd-version.zip).
- Follow the ReadMe.txt instructions to set up your localhost Apache server.
  - If you are using a port other than 80, modify the "Listen 80" line to "Listen your_port" in Apache24/conf/httpd.conf.
- Download the URLCustomDiscsPack.zip resource pack and place it in Apache24/htdocs/ directory.
- In Apache24/, create a .htaccess file with the following content:
```
<Files "URLCustomDiscsPack.zip">
	ForceType application/octet-stream
	Header set Content-Disposition "attachment; filename=URLCustomDiscsPack.zip"
</Files>
```
- Restart Apache, then try to download the resource pack with this URL: <br>
  [http://your_public_ip:80/URLCustomDiscsPack.zip]()
</details>

## Installation Guide
- Download the URLCustomDiscs-1.0-1.21.0.jar plugin and place it in your Minecraft server's plugins folder (or create one if it doesn't exist).
- Download the URLCustomDiscsPack.zip resource pack and place it in your desired directory on your personal web server to make it available for download.
- In your Minecraft server's server.properties file, locate the line "resource-pack=" and update it to include your download URL:
  `resource-pack=http://your_public_ip:80/URLCustomDiscsPack.zip`
  - You can also force the resource pack to be downloaded for players by setting: <br>
    `require-resource-pack=true`
- Download the "yt-dlp.exe" executable from [yt-dlp GitHub](https://github.com/yt-dlp/yt-dlp#installation) and place it in your Minecraft server folder.
- Download FFmpeg from [FFmpeg official website](https://ffmpeg.org/download.html), extract it in your Minecraft server folder, and rename the folder to "FFmpeg":
  - Example for Windows x64 users:
    - Download "ffmpeg-master-latest-win64-gpl-shared.zip" from [FFmpeg Builds GitHub](https://github.com/BtbN/FFmpeg-Builds/releases).
    - Extract the .zip archive in your Minecraft server folder.
    - Rename the "ffmpeg-master-latest-win64-gpl-shared" folder to "FFmpeg".

## Configuration Guide
- Start your Minecraft server to allow the plugin to generate the necessary files.
- In your Minecraft server folder, open plugins/URLCustomDiscs/config.yml.
- Follow the instructions to add the absolute path and download URL for the resource pack.
- Restart your Minecraft server

## Is Everything Working Fine ?
You should automatically receive the resource pack when you join the server and should experience automatic resource pack updates when a custom music disc is created.

<details>
<summary>Troubleshooting: If something is not working</summary>

If you do not receive the resource pack, check:
- that you can download the resource pack from the URL on your personal web server;
- that you have correctly entered the download URL of the resource pack from your personal web server in server.properties;
- that you have correctly configured the absolute path and download URL for the resource pack in config.yml.

If you can't create a custom music disc, check:
- the configuration of the absolute path of the resource pack in config.yml;
- the installation of yt-dlp and FFmpeg.
</details>

## Tree Structures
<details>
<summary>Understanding where/how everything goes/works</summary>

Your Minecraft server:
```
your_server_folder/
├── FFmpeg/
│   ├── bin/...
│   ├── doc/...
│   ├── include/...
│   └── lib/...
├── plugins/                          (create it if not already done)
│   ├── URLCustomDiscs/               (automatically created when the plugin is loaded, plugin folder)
│   │   ├── music/                    (automatically created when creating a custom music disc, used to download and convert YouTube music to Ogg)
│   │   ├── discs.json                (automatically created when creating a custom music disc, stores information about custom music discs)
│   │   └── config.yml                (automatically created when the plugin is loaded, allows you to configure the resource pack server)
│   └── URLCustomDiscs-1.0-1.21.0.jar (download the URLCustomDiscs plugin)
├── yt-dlp.exe
└── other server folders and files...
```

URLCustomDiscsPack.zip resource pack:
```
URLCustomDiscsPack.zip/
├── assets/
│   └── minecraft/
│       ├── models/
│       │   └── item/
│       │       ├── music_disc_13.json ("overrides" added on disc 13 to assign custom music disc models using custom_model_data)
│       │       └── custom_music_disc_example.json (custom music disc models automatically created) 
│       ├── sounds/
│       │   └── custom/
│       │       └── (custom music disc tracks are saved here)
│       ├── textures/
│       │   └── item/
│       │       └── record_custom.png (custom music discs texture)
│       └── sounds.json (tracks automatically associated with custom music discs)
└── pack.mcmeta
```
</details>

## Disclaimer
Please note that it is the sole responsibility of each user to comply with applicable copyright laws and the terms of service of any music provider when using this plugin. The developer of this plugin do not assume any liability for unauthorized use or violations of such laws and regulations.