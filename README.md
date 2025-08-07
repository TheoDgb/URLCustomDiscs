## 1.21-1.21.8 URLCustomDiscs plugin (+ server resource pack)
Last updated on July 4, 2025.

<img src="https://github.com/TheoDgb/URLCustomDiscs/blob/main/media/URLCustomDiscs_icon.png?raw=true" alt="URLCustomDiscs Icon" style="width: 10%;">

## About
This plugin, along with a server resource pack, allows you to **create and play custom music discs from YouTube URLs or MP3 files** on your Minecraft server, with **real-time updates for players**.

Once installed, everything is done **in-game**, and there's no need to manually edit the resource pack files to add or remove discs.

The plugin supports Minecraft's spatial audio for music, but you can also play it in stereo.

Additionally, vanilla commands such as `/playsound` and `/stopsound` work with the custom music, so you do not need to use a disc in a jukebox.

**Important**: Make sure to use a direct video URL without any parameters (such as playlist, timecode, etc.), or you might get an unexpected result. Delete everything from the "&" in the URL.

## Usage
<table>
  <thead>
    <tr>
      <td colspan="2" style="text-align: center;"><strong>Create a custom disc</strong></td>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td style="width: 50%;">1. Use the "create" command and paste your URL</td>
      <td style="width: 50%;">3. All logged-in players receive the updated server resource pack containing the created custom disc</td>
    </tr>
    <tr>
      <td style="width: 50%;">2. Wait for the track to be downloaded, converted, and added to the server resource pack</td>
      <td style="width: 50%;">4. The custom disc is given to you, enjoy the music!</td>
    </tr>
    <tr>
      <td style="width: 50%; padding: 0; margin: 0; text-align: center; vertical-align: middle;"><img src="/media/create_custom_disc.gif?raw=true" alt="create_custom_disc" style="width: 100%; height: 100%; object-fit: contain; display: block;"></td>
      <td style="width: 50%; padding: 0; margin: 0; text-align: center; vertical-align: middle;"><img src="/media/play_custom_disc.gif?raw=true" alt="play_custom_disc" style="width: 100%; height: 100%; object-fit: contain; display: block;"></td>
    </tr>
  </tbody>
</table>

## Audio Download Options
### **Warning**: ***YouTube may sometimes block my API server***. This issue can be resolved by using a residential proxy, but it's costly for me. Thank you for your understanding.
In the meantime, you have **two alternative options**:
- **Use the built-in **yt-dlp** dependency (Minecraft server-side download)**  
  You can use the included **yt-dlp** dependency in the plugin. It will download the audio from the YouTube URL into the plugin folder and then send it to the API.
  > This method is **unlikely to work on shared Minecraft hosting providers**, such as Shockbyte, as they often do **not allow execution** of yt-dlp, or the IP ranges of those servers are likely **already banned by YouTube** (as my API can be at times).
- **Manually download the MP3 file (admin-only, 100% reliable)**  
  If you don't mind downloading the audio as an MP3, use a website like [noTube](https://notube.lol/fr/youtube-app-213) to download the MP3 manually. Then, place the file directly into the `audio_to_send` folder inside the `URLCustomDiscs` plugin directory.  
  Use the appropriate command to create a disc with that MP3.
  > This method only works for Minecraft server admins, as it requires access to the server’s file system.

## Commands Overview
Display the list of commands:  
`/customdisc help`

Create a custom music disc from a YouTube URL or local MP3 file:  
`/customdisc create <URL OR audio_name.mp3> <disc_name> <mono/stereo>`
- mono: enables spatial audio (as when played in a jukebox)
- stereo: plays the audio in the traditional way
> **Instructions for local MP3 files (admin-only)**:  
> - Place your MP3 file inside the `audio_to_send` folder in the plugin directory before running the create command with the `audio_name.mp3`.  
> - Rename the MP3 file to a simple name with no spaces and no special characters.
> - Don't forget to include the `.mp3` extension in the `audio_name.mp3` field.

Give yourself a custom music disc:  
`/customdisc give <disc_name>`

Show the list of custom music discs (click a disc name to autofill the give command):  
`/customdisc list`

Delete a custom music disc:  
`/customdisc delete <disc_name>`

Show details of the custom music disc you're holding (useful for debugging):  
`/customdisc info`

Update the yt-dlp dependency:  
`/customdisc update`

Vanilla command to play a custom track (can be used with coordinates):  
`/execute positioned ~ ~ ~ run playsound minecraft:customdisc.<disc_name> ambient @a`

Vanilla command to stop a custom track:  
`/stopsound @a * minecraft:customdisc.<disc_name>`

## Usage Modes
The plugin offers **two modes of use**, depending on your setup and preferences:
- **API Mode** (default):  
  Uses a dedicated remote API to manage custom discs, the resource pack, and dependencies. Ideal for **quick and easy setup**, but with some limitations on the resource pack and audio files.
- **Self-Hosted Mode**:  
  For advanced users who prefer to **host and manage everything themselves**. You can bypass the API and configure the plugin manually using your **own setup and HTTP server**.
### API Mode Resource Pack Limitations
To minimize storage space and ensure fast downloads, the resource pack has the following limitations:
- Up to 10 custom discs can be included in the resource pack at the same time.
- Each custom discs is limited to a maximum duration of 5 minutes.
- The resource pack size is limited to 80 MB (more than enough).
- If no activity (adding or deleting a custom disc) occurs for 3 months, the token and resource pack will be automatically deleted to free up space.
### Self-Hosted Mode Requirements
You must provide a **personal HTTP server to host the resource pack**. Depending on your setup (local or online Minecraft server / local or online HTTP server), the HTTP server must support the following:
- **Locals servers**: allow the plugin to edit the resource pack directly via an absolute file path.
- **Online servers**: allow the plugin to upload the updated resource pack using HTTP POST requests.
- **Players**: provide access to download the resource pack and receive real-time updates for custom music discs.
### Documentation
This documentation focuses on the default **API Mode**, which is the recommended setup for most users.

If you want to use the **Self-Hosted Mode** instead, please refer to the dedicated documentation from the URLCustomDiscs GitHub repository: [Self-Hosted Mode documentation](https://github.com/TheoDgb/URLCustomDiscs/blob/main/SELFHOSTEDINSTALLATION.md)

For more details, you may explore the **API** documentation, source code and architecture in the [URLCustomDiscsAPI GitHub repository](https://github.com/TheoDgb/URLCustomDiscsAPI).

## Dependencies (used by the API and/or the Minecraft server)
### License & Attribution
The **URLCustomDiscsAPI** uses the following external tools:
- **yt-dlp**, from the [yt-dlp GitHub repository](https://github.com/yt-dlp/yt-dlp), licensed under the [Unlicense](https://github.com/yt-dlp/yt-dlp/blob/master/LICENSE)
- **FFmpeg**, from the [FFmpeg Static Builds](https://johnvansickle.com/ffmpeg/), licensed under the [GNU General Public License version 3 (GPLv3)](https://www.gnu.org/licenses/gpl-3.0.html)
### Tool Usage
- **yt-dlp**: downloads MP3 audio files from YouTube URLs.  
  When local yt-dlp usage is enabled, the tool is automatically downloaded and kept up to date by the plugin at runtime.
- **FFmpeg**: converts MP3 files to Ogg Vorbis format for Minecraft compatibility.

## Download
The **URLCustomDiscs.jar** plugin is available for download in the [Releases](https://github.com/TheoDgb/URLCustomDiscs/releases) section.  
It is also available on [Modrinth](https://modrinth.com/plugin/url-custom-discs).

## Installation
This plugin requires a brief setup to ensure players automatically download the resource pack when they join the Minecraft server.
### Steps to install with the API Mode (default mode):
1. Download and place the plugin `URLCustomDiscs.jar` file into your Minecraft server's `plugins` folder.
2. Start your Minecraft server.  
   This will create a plugin folder at `plugins/URLCustomDiscs` containing a `config.yml` file.
3. Open `plugins/URLCustomDiscs/config.yml` and make sure the following field is set: `pluginUsageMode: api`
4. In the same config file, locate the **REMOTE API MODE CONFIGURATION** section.  
   Important fields include:
    - `apiBaseURL`: the base URL of the remote API
    - `token`: initially empty, automatically generated after creating your first custom disc
    - `downloadPackURL`: initially empty, automatically generated after creating your first custom disc
    - `localYtDlp`: defines whether to use the built-in yt-dlp tool included in the plugin to download audio files directly from YouTube on your own server instead of relying on the external API. This field is set to false by default.
5. Create your first custom disc in-game using the `/customdisc create <URL OR audio_name.mp3> <disc_name> <mono/stereo>` command.  
   This action will generate your unique `token` and the `downloadPackURL`.
6. Copy the generated `downloadPackURL` and paste it into your Minecraft server's `server.properties` file under the `resource-pack=` filed, for example:  
   `resource-pack=https://your-generated-downloadPackURL.zip`
7. Restart your Minecraft server to apply the resource pack settings.
8. (Optional) To force players to download the resource pack, set `require-resource-pack=true` in `server.properties` and restart your Minecraft server.
9. Due to a potential issue with the API server being blocked by YouTube, if you **don’t want to manually download MP3 files** and would prefer to keep using **YouTube URLs**, you can configure the plugin to use the **included yt-dlp** dependency directly on your Minecraft server.  
   To do this, set the following field in the `config.yml` file: `localYtDlp: true`, then restart your Minecraft server.
   > This option likely won’t work on most **shared Minecraft hosting providers** (such as Shockbyte). For more details, refer to the [Audio Download Options](#audio-download-options)
   section.

**Note**: All these steps are also explained in detail in the `config.yml` file created when you first launch the plugin.

## Tree Structures
<details>
<summary><b>Understanding where/how everything goes/works</b></summary>

Your Minecraft server:
```
your_mc_server_folder/                    
├── plugins/                            (create it if not already done)
│   ├── URLCustomDiscs/                 (automatically created when the plugin is loaded, plugin folder)
│   │   ├── config.yml                  (automatically created when the plugin is loaded, allows you to retrieve the download URL (with token) of the resource pack)
│   │   └── discs.json                  (automatically created when creating a custom music disc, stores information about custom music discs)
│   └── URLCustomDiscs-1.0.0-1.21.0.jar (download the URLCustomDiscs plugin)
├── server.properties                   (Minecraft server's file, used to configure the resource pack download URL (with token))
└── other Minecraft server folders and files... (
```

1.21 URLCustomDiscsPack.zip server resource pack:
```
URLCustomDiscsPack.zip/
├── assets/
│   └── minecraft/
│       ├── models/
│       │   └── item/
│       │       ├── music_disc_13.json             ("overrides" added on disc 13 to assign custom music disc models using custom_model_data)
│       │       └── custom_music_disc_example.json (custom music disc models automatically created) 
│       ├── sounds/
│       │   └── custom/                            (custom music disc tracks are saved there)
│       ├── textures/
│       │   └── item/
│       │       └── record_custom.png              (custom music discs texture)
│       └── sounds.json                            (tracks automatically associated with custom music discs)
└── pack.mcmeta
```

1.21.4+ URLCustomDiscsPack.zip server resource pack:
```
URLCustomDiscsPack.zip/
├── assets/
│   └── minecraft/
│       ├── items/
│       │   └── music_disc_13.json                 ("entries" added on disc 13 to assign custom music disc models using custom_model_data)
│       ├── models/
│       │   └── item/
│       │       └── custom_music_disc_example.json (custom music disc models automatically created) 
│       ├── sounds/
│       │   └── custom/                            (custom music disc tracks are saved there)
│       ├── textures/
│       │   └── item/
│       │       └── record_custom.png              (custom music discs texture)
│       └── sounds.json                            (tracks automatically associated with custom music discs)
└── pack.mcmeta
```
</details>

## Support & Community
This plugin relies on a **remote API** and **resource pack hosting** maintained by me.  
If you'd like to support the project and help cover server and storage costs, please consider contributing via [Ko-fi](https://ko-fi.com/asson). Thank you! ❤️

For any questions, help, or suggestions, feel free to join the [Discord server](https://discord.gg/tdWztKWzcm)!

## Disclaimer
Please note that it is the sole responsibility of each user to comply with applicable copyright laws and the terms of service of any music provider when using this plugin. The developer of this plugin does not assume any liability for unauthorized use or violations of such laws and regulations.
