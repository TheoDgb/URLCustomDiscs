## 1.21-1.21.8 URLCustomDiscs plugin (+ server resource pack)
Last updated on September 8, 2025.

![Modrinth Downloads Badge](https://img.shields.io/modrinth/dt/9dkRl54Z?style=for-the-badge&logo=modrinth&color=%2300AF5C&link=https%3A%2F%2Fmodrinth.com%2Fplugin%2Furl-custom-discs) ![Github Downloads Badge](https://img.shields.io/github/downloads/TheoDgb/URLCustomDiscs/total?label=Downloads&style=for-the-badge&logo=github&color=blue&link=https://github.com/TheoDgb/URLCustomDiscs)

<img src="https://github.com/TheoDgb/URLCustomDiscs/blob/main/media/URLCustomDiscs_icon.png?raw=true" alt="URLCustomDiscs Icon" style="width: 10%;">

## About
This plugin, along with a server resource pack, allows you to **create and play custom music discs from YouTube URLs or MP3 files** on your Minecraft server, with **real-time updates for players**.

Once installed, everything is done **in-game**, and there's no need to manually edit the resource pack files to add or remove custom discs.

The plugin supports Minecraft's spatial audio for music, but you can also play it in stereo.

Additionally, vanilla commands such as `/playsound` and `/stopsound` work with the custom music, so you do not need to use a disc in a jukebox.

**Important**: Make sure to use a direct URL without any parameters (such as playlist, timecode, etc.), or you might get an unexpected result.

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
- **Use the auto-installed **yt-dlp** dependency (Minecraft server-side download)**  
  The plugin uses the **yt-dlp** dependency to download the audio from the YouTube URL into the plugin folder and then send it to the API.
  > This method is **unlikely to work on shared Minecraft hosting providers**, such as Shockbyte, as they often do **not allow execution** of yt-dlp, or the IP ranges of those servers are likely **already banned by YouTube** (as my API can be at times).
- **Manually download the MP3 file (admin-only, 100% reliable)**  
  If you don't mind downloading the audio as an MP3 file, use a website like [noTube](https://notube.lol/fr/youtube-app-213) to download it manually. Then, place the MP3 file directly into the `audio_to_send` folder inside the `URLCustomDiscs` plugin directory.  
  After that, use the appropriate command to create a custom disc with this MP3 file.
  > This method only works for Minecraft server admins, as it requires access to the server’s file system.

## Commands Overview
Display the list of commands:  
`/customdisc help`

Create a custom music disc from a YouTube URL or local MP3 file:  
`/customdisc create <URL OR audio_name.mp3> <disc_name> <mono/stereo>`
- mono: enables spatial audio (as when played in a jukebox)
- stereo: plays the audio in the traditional way
> **Instructions for local MP3 files (admin-only)**:
> - Place your MP3 file inside the `audio_to_send` folder in the plugin directory.
> - Rename the MP3 file to a simple name with no spaces and no special characters.
> - Run the create command with the `audio_name.mp3`.  
>   - Don't forget to include the `.mp3` extension in the `audio_name.mp3` field.

Give yourself a custom music disc:  
`/customdisc give <disc_name>`

Show the list of custom music discs (click a disc name to autofill the give command):  
`/customdisc list`

Delete a custom music disc:  
`/customdisc delete <disc_name>`

Show details of the custom music disc you're holding (useful for debugging):  
`/customdisc info`

Update the yt-dlp dependency:  
`/customdisc updatedep`

Vanilla command to play a custom track (can be used with coordinates):  
`/playsound minecraft:customdisc.<disc_name> ambient @a ~ ~ ~ 1 1`

Vanilla command to stop a custom track:  
`/stopsound @a * minecraft:customdisc.<disc_name>`

## Usage Modes
The plugin offers **three modes of use**, depending on your setup and preferences:
- **API Mode** (default):  
  Uses a dedicated remote API to manage custom discs, the resource pack, and dependencies. Ideal for **quick and easy setup**, but with some limitations on the resource pack and audio files.
- **Self-Hosted Mode**:  
  For advanced users who prefer to **host and manage everything themselves**. You can bypass the API and configure the plugin manually using your **own setup and HTTP server**.
- **Edit-Only Mode**:  
  Allows you to **update the resource pack locally** and duplicate it to a custom path with a custom name. Useful if you already host another resource pack and want to merge it with the `URLCustomDiscsPack.zip`.

### API Mode Resource Pack Limitations
To minimize storage space and ensure fast downloads, the resource pack has the following limitations:
- Up to 10 custom discs can be included in the resource pack at the same time.
- Each custom disc is limited to a maximum duration of 5 minutes.
- The resource pack size is limited to 80 MB (more than enough).
- If no activity (adding or deleting a custom disc) occurs for 3 months, the token and resource pack will be automatically deleted to free up space.

### Self-Hosted Mode Requirements
You must provide a **personal HTTP server to host the resource pack**. Depending on whether your Minecraft server and HTTP server are on the same machine or on separate machines, the HTTP server must support:
- **Local access**: The plugin can update the resource pack via an absolute path.
- **Online access**: The plugin can download and upload the resource pack using HTTP requests.
- **Player access**: Players must be able to download the resource pack and receive real-time updates for custom music discs.

### Documentation
This documentation focuses on the default **API Mode**, which is the recommended setup for most users.

If you want to use the **Self-Hosted Mode** or the **Edit-Only Mode** instead, please refer to the dedicated documentation from the URLCustomDiscs GitHub repository: [Self-Hosted and Edit-Only Modes documentation](https://github.com/TheoDgb/URLCustomDiscs/blob/main/SELF-HOSTED_AND_EDIT-ONLY_MODES_INSTALLATION.md)

For more details, you may explore the API documentation, source code and architecture in the [URLCustomDiscsAPI GitHub repository](https://github.com/TheoDgb/URLCustomDiscsAPI).

## Dependencies
### Optional plugin
**ProtocolLib** [[Download](https://www.protocollib.com/)]: used to display a custom "Now Playing" toast when a custom disc is inserted into a jukebox
### External Tools
#### License & Attribution
The plugin automatically installs and uses the following tools (yt-dlp is also kept up to date):
- **yt-dlp**, from the [yt-dlp GitHub repository](https://github.com/yt-dlp/yt-dlp), licensed under the [Unlicense](https://github.com/yt-dlp/yt-dlp/blob/master/LICENSE)
- **FFmpeg**, from the [FFmpeg-Builds GitHub repository (Windows)](https://github.com/BtbN/FFmpeg-Builds) and the [FFmpeg Static Builds (Linux)](https://johnvansickle.com/ffmpeg/), licensed under the [GNU General Public License version 3 (GPLv3)](https://www.gnu.org/licenses/gpl-3.0.html)
#### Tool Usage
- **yt-dlp** downloads MP3 audio files from YouTube URLs.
- **FFmpeg** converts MP3 files to Ogg Vorbis format for Minecraft compatibility.

## Download
The `URLCustomDiscs.jar` plugin is available for download in the [Releases](https://github.com/TheoDgb/URLCustomDiscs/releases) section.  
It is also available on [Modrinth](https://modrinth.com/plugin/url-custom-discs).

The `ProtocolLib.jar` plugin is available for download on the [official ProtocolLib website](https://www.protocollib.com/) (not mandatory).

## Installation
This plugin requires a brief setup to ensure players automatically download the resource pack when they join the Minecraft server.
### Steps to install with the API Mode (default mode):
1. Download the `URLCustomDiscs.jar` plugin into your Minecraft server's `plugins` folder.
2. Start your Minecraft server so the plugin generates the `plugins/URLCustomDiscs` folder containing a `config.yml` file.
3. Open `plugins/URLCustomDiscs/config.yml` and set the following field to `pluginUsageMode: api`
4. In the same config file, locate the **REMOTE API MODE CONFIGURATION** section.  
   Important fields include:
    - `apiBaseURL`: the base URL of the remote API
    - `token`: initially empty, automatically generated after creating your first custom disc
    - `apiDownloadResourcePackURL`: initially empty, automatically generated after creating your first custom disc
    - `localYtDlp`: defines whether to use the auto-installed yt-dlp tool in the plugin folder to download audio files from YouTube directly on your own server instead of relying on the external API. This field is set to false by default.
5. Create your first custom disc in-game using the `/customdisc create <URL OR audio_name.mp3> <disc_name> <mono/stereo>` command.  
   This action will generate your unique `token` and the `apiDownloadResourcePackURL`.
6. Copy the generated `apiDownloadResourcePackURL` and paste it into your Minecraft server's `server.properties` file under the `resource-pack=` filed, for example:  
   `resource-pack=https://your-generated-apiDownloadResourcePackURL.zip`
7. Restart your Minecraft server to apply the resource pack settings.
8. (Optional) To force players to download the resource pack, set `require-resource-pack=true` in `server.properties` and restart your Minecraft server.
9. Due to a potential issue with the API server being blocked by YouTube, if you **don’t want to manually download MP3 files** and would prefer to **keep using YouTube URLs**, you can configure the plugin to use the **auto-installed yt-dlp** dependency directly on your Minecraft server.  
   To do this, set the following field in the `config.yml` file: `localYtDlp: true`, then restart your Minecraft server.
   > This option likely won’t work on most **shared Minecraft hosting providers** (such as Shockbyte). For more details, refer to the [Audio Download Options](#audio-download-options)
   section.
10. (Optional) Download the `ProtocolLib.jar` plugin into your Minecraft server's `plugins` folder for custom "Now Playing" toasts, then restart your Minecraft server.

**Note**: All these steps are also explained in detail in the `config.yml` file created when you first launch the plugin.

## Support & Community
This plugin relies on a **remote API** and **resource pack hosting** maintained by me.  
If you'd like to support the project and help cover server and storage costs, please consider contributing via [Ko-fi](https://ko-fi.com/mcallium). Thank you! ❤️

For any questions, help, or suggestions, feel free to join the [Discord server](https://discord.gg/tdWztKWzcm)!

## Disclaimer
Please note that it is the sole responsibility of each user to comply with applicable copyright laws and the terms of service of any music provider when using this plugin. The developer of this plugin does not assume any liability for unauthorized use or violations of such laws and regulations.