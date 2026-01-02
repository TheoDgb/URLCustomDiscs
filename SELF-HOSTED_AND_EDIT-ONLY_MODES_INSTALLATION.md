## 1.21-1.21.8 URLCustomDiscs plugin (+ server resource pack)
Last updated on November 26, 2025.

[![Modrinth Downloads Badge](https://img.shields.io/modrinth/dt/9dkRl54Z?style=for-the-badge&logo=modrinth&color=%2300AF5C&link=https%3A%2F%2Fmodrinth.com%2Fplugin%2Furl-custom-discs)](https://modrinth.com/plugin/url-custom-discs) [![Github Downloads Badge](https://img.shields.io/github/downloads/TheoDgb/URLCustomDiscs/total?label=Downloads&style=for-the-badge&logo=github&color=blue&link=https://github.com/TheoDgb/URLCustomDiscs)](https://github.com/TheoDgb/URLCustomDiscs)

<img src="https://github.com/TheoDgb/URLCustomDiscs/blob/main/media/URLCustomDiscs_icon.png?raw=true" alt="URLCustomDiscs Icon" style="width: 10%;">

## About
This plugin, along with a server resource pack, allows you to **create and play custom music discs from YouTube URLs or MP3 files** on your Minecraft server, with **real-time updates for players**.

Once installed, everything is done **in-game**, and there's no need to manually edit the resource pack files to add or remove custom discs.

The plugin supports Minecraft's spatial audio for music, but you can also play it in stereo.

Additionally, vanilla commands such as `/playsound` and `/stopsound` work with the custom music, so you do not need to use a disc in a jukebox.

**Important**: For non-YouTube URLs, make sure to use a direct URL without any parameters (such as playlist, timecode, etc.), or you might get an unexpected result.

**Note**: Plugin tested on 1.21-1.21.8 Paper and Arclight servers and currently works on Windows and Linux due to dependencies.

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

Update Deno and yt-dlp dependencies:  
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
This documentation focuses on the **Self-Hosted Mode** and the **Edit-Only Mode**, which are intended for advanced users who prefer to manage everything themselves, without relying on the default API setup.

If you want to use the default **API Mode** instead, please refer to the dedicated documentation from the URLCustomDiscs GitHub repository: [API Mode documentation](https://github.com/TheoDgb/URLCustomDiscs/blob/main/README.md)

For more details, you may explore the API documentation, source code and architecture in the [URLCustomDiscsAPI GitHub repository](https://github.com/TheoDgb/URLCustomDiscsAPI).

## Dependencies
### Optional plugin
**ProtocolLib** [[Download](https://www.protocollib.com/)]: used to display a custom "Now Playing" toast when a custom disc is inserted into a jukebox
### External Tools
#### License & Attribution
The plugin automatically installs and uses the following tools (Deno and yt-dlp are also kept up to date):
- **Deno**, from the [Deno GitHub repository](https://github.com/denoland/deno), licensed under the [MIT License](https://github.com/denoland/deno/blob/main/LICENSE.md)
- **yt-dlp**, from the [yt-dlp GitHub repository](https://github.com/yt-dlp/yt-dlp), licensed under the [Unlicense](https://github.com/yt-dlp/yt-dlp/blob/master/LICENSE)
- **FFmpeg**, from the [FFmpeg-Builds GitHub repository (Windows)](https://github.com/BtbN/FFmpeg-Builds) and the [FFmpeg Static Builds (Linux)](https://johnvansickle.com/ffmpeg/), licensed under the [GNU General Public License version 3 (GPLv3)](https://www.gnu.org/licenses/gpl-3.0.html)
#### Tool Usage
- **Deno** is used by yt-dlp to interpret YouTube’s JavaScript and decrypt its signature cipher, which is required to extract and download audio data from YouTube.
- **yt-dlp** downloads MP3 audio files from YouTube URLs.
- **FFmpeg** converts MP3 files to Ogg Vorbis format for Minecraft compatibility.

## Download
The `URLCustomDiscs.jar` plugin and the `URLCustomDiscsPack.zip` server resource pack are available for download in the [Releases](https://github.com/TheoDgb/URLCustomDiscs/releases) section.
They are also available on [Modrinth](https://modrinth.com/plugin/url-custom-discs) ([**Versions**](https://modrinth.com/plugin/url-custom-discs/versions)).

After downloading the appropriate resource pack for your Minecraft server version, **rename it to** `URLCustomDiscsPack.zip`.

The `ProtocolLib.jar` plugin is available for download on the [official ProtocolLib website](https://www.protocollib.com/) (not mandatory).

# Self-Hosted Mode Installation
## Servers Guide
**Important**:
- For users who **don't want or can't host their Minecraft server on their own personal machine**, it's common to turn to online hosting providers.
  Keep in mind that you’ll still need a **personal HTTP server to host the resource pack** for the custom discs to be updated automatically to all players.
- If you're using a **shared Minecraft hosting provider** such as Shockbyte or a similar provider, **make sure your host allows you to run binary files** (specifically `yt-dlp` and `FFmpeg`).
  **Some hosts may have restrictions** or require special permissions. Check their documentation or contact their support if you're unsure.
- To ensure full compatibility and control, it's highly recommended to use your **own machine** or a **VPS (Virtual Private Server)** instead of shared Minecraft hosting.
  A VPS gives you full root access, allowing you to host both your Minecraft server and an HTTP server (such as Apache) on the same machine, making it ideal for directly serving the resource pack.
  > **Warning**:  
  If you intend to use `yt-dlp` to download YouTube audio directly from your server, you must ensure that the machine (including VPS) is **not part of an IP range currently blocked by YouTube**.  
  Shared Minecraft environments are often already blacklisted due to widespread abuse.  
  For greater reliability, consider using a **residential proxy with IP rotation**. This significantly reduces the risk of download failures and helps maintain consistent access to YouTube for audio retrieval.  
  In most cases, if your Minecraft server is running **locally on your personal machine**, `yt-dlp` works perfectly **without any proxy**, since residential IPs are rarely blocked by YouTube.  
  In any case, you can still create custom discs by using MP3 files.
  
- This guide does **not** cover Apache HTTP server setup on a **VPS**.
  VPS environments require manual configuration (IP binding, port forwarding, firewall rules, etc.), which vary depending on the provider. However, the steps to set up Apache remain the same.
  Just make sure to open the necessary ports and configure your firewall correctly to allow HTTP traffic.

The following shared Minecraft hosting providers have been tested or confirmed to allow the execution of binary files (yt-dlp/FFmpeg):
- Compatible:
    - **ElypseCloud** allows you to upload custom binaries via FileZilla or SSH directly to your Minecraft server's plugin folder, and also lets you grant execution permissions through its built-in file manager (tested in 2025).
- Not compatible:
    - **Shockbyte** shared hosting environments block the execution of external or custom binaries (tested in 2025).
    - **exaroton** (tested in 2025)

**Note**: If you've confirmed compatibility or incompatibility with another shared Minecraft hosting provider, feel free to let me know on the [Discord server](https://discord.gg/tdWztKWzcm) so I can update the list!

**THAT BEING SAID,**

You'll need to host the `URLCustomDiscsPack.zip` server resource pack on an HTTP server that the plugin can access and edit. Using an online file hosting service (such as [MCPacks](https://mc-packs.net/)) will not work.

Here are two tutorials for setting up a local HTTP server (Windows/Linux). These tutorials cover how to make the server resource pack accessible to your Minecraft server, either locally (via an absolute path) or online (via HTTP requests).

### Router Configuration
- Access your router's interface to:
    - configure a NAT/PAT rule for TCP port forwarding, setting both internal and external ports to 80, and using the **private IP address** of the machine running the Apache HTTP server;
    - open TCP port 80, which is the default for HTTP traffic, in your firewall to allow incoming connections to the machine running the Apache server.
<details>
<summary><b>More details and example with an Orange modem-router</b></summary>

- Access your router's interface
  <img src="/media/router_example_screenshots/1_router_interface.png" alt="router_example_screenshot_1_router_interface">
- In your router's network settings, configure a NAT/PAT rule to forward TCP port 80 (both internal and external) to the **private IPv4 address** of the machine running the Apache HTTP server.
    - On some routers, you may not be able to directly select a device by its name from a list. In that case, you'll need to manually enter the **private IPv4 address** of the machine running the Apache HTTP server. Make sure that this address is either statically assigned or reserved via DHCP on the router, so it doesn't change over time.
    - To find the **private IPv4 (or IPv6) address** of your machine:
        - on Windows, open the command prompt and type: ```ipconfig```
        - on Linux, open a terminal and type: ```ip a```

  <img src="/media/router_example_screenshots/2_access_router_network.png" alt="router_example_screenshot_2_access_router_network">
  <img src="/media/router_example_screenshots/3_get_private_ipv4.png" alt="router_example_screenshot_3_get_private_ipv4">
  <img src="/media/router_example_screenshots/4_port_forwarding.png" alt="router_example_screenshot_4_port_forwarding">
- In your router's firewall, open TCP port 80, which is the default for HTTP traffic, to allow incoming connections to the **private IPv4 (or IPv6) address** of the machine running the **Apache HTTP server**.
  <img src="/media/router_example_screenshots/5_access_router_firewall.png" alt="router_example_screenshot_5_access_router_firewall">
  <img src="/media/router_example_screenshots/6_open_port.png" alt="router_example_screenshot_6_open_port">
</details>

Router NAT Loopback Limitation (Local Access Issue):
- If you are using a setup where the Apache HTTP server and the Minecraft game are running on **two different machines on the same local network**, and everything is correctly installed and configured but **you** (the server owner) **don't receive the server resource pack while other players do**, your router may not support **NAT loopback**. This means that if a machine on the local network tries to access the **public IP address** of the router to reach another local machine, the connection may fail. This issue **only affects connections from inside your local network**.
- To work around this, you can manually download the server resource pack via the **private IPv4 address**: [http://your_private_ipv4/URLCustomDiscsPack.zip](). Then place it in your `.minecraft/resourcepacks/` folder to use it as a classic resource pack. However, you'll need to **reinstall it manually** each time a disc is created or deleted to stay up to date.

### Personal Local HTTP Server
For the following steps, you'll need the **public IPv4 address** of your network (assigned by your internet service provider). You can quickly find it on websites like [WhatIsMyIp.com](https://www.whatismyip.com/). This is the address **external users will use to reach the machine running the Apache HTTP server**.
<details>
<summary><b>Create an Apache server on Windows (local access)</b></summary>

- Download Apache from [Apache Lounge](https://www.apachelounge.com/download/) (httpd-version.zip).
- Follow the `ReadMe.txt` instructions to set up your localhost Apache server.
    - If you are using a port other than 80, modify the "Listen 80" line to "Listen your_port" in `Apache24/conf/httpd.conf`.
- Download the server resource pack in `C:/Apache24/htdocs/` and rename it `URLCustomDiscsPack.zip`.
- In `C:/Apache24/`, create a `.htaccess` file with the following content:
```
<Files "URLCustomDiscsPack.zip">
	ForceType application/octet-stream
	Header set Content-Disposition "attachment; filename=URLCustomDiscsPack.zip"
</Files>
```
- Restart Apache, then try to download the **server resource pack** with this URL:  
  [http://your_public_ip:80/URLCustomDiscsPack.zip]()

Your local HTTP server now works with a Minecraft server **hosted on the same machine**.
<details>
<summary><b>Extra steps for online access (Apache server on Windows)</b></summary>

- Download PHP ([Thread Safe version zip](https://windows.php.net/downloads/releases/php-8.4.5-Win32-vs17-x64.zip)) from [php.net](https://windows.php.net/download/).
- Extract all the files from the zip archive to `C:/php/`
- In `C:/php/`, duplicate `php.ini-development` and rename the copy to "php.ini".
    - This is the configuration file for PHP, where you can set file size limits, execution times, and more.
- In `php.ini`, change the values of the following lines to your desired limits:
    - For reference: If you plan to upload 50 music files of approximately 3 minutes (around 150MB), you should set the values between 200MB-300MB to be on the safe side.
    - Keep in mind that the more music files you add, the longer the upload process will take.
    - Note that if you add too many audio files (around 50+ depending on file size) and the pack takes too long to update, creating a new disc may fail, and removing one could crash the server (but will still function).
        - This defines the maximum size of files that can be uploaded.  
          `upload_max_filesize = 300M`
        - This value defines the maximum size for a POST request (it must be at least equal to upload_max_filesize).  
          `post_max_size = 300M`
        - This determines how long a PHP script can run before it is terminated. We increase it to 300 seconds (5 minutes) to allow more time for large uploads.  
          `max_execution_time = 300`
        - This defines the maximum time a script can spend receiving data. We set this to 300 seconds to accommodate large file uploads.  
          `max_input_time = 300`
- Open `C:/Apache24/conf/httpd.conf`
    - Locate the "LoadModule" lines and add the following line to load PHP:  
      `LoadModule php_module "C:/php/php8apache2_4.dll"`
        - Verify that the version "php8apache2_4.dll" corresponds to the one you downloaded and extracted.
    - Below this line, add the following line to specify the location of the `php.ini` file:  
      `PHPIniDir "C:/php"`
    - Locate the "AddType" lines and add the following line to associate PHP files:  
      `AddType application/x-httpd-php .php`
- In `C:/Apache24/htdocs/`, create an `info.php` file with the following content:  
  `<?php phpinfo(); ?>`
- Restart Apache, then try to access [http://your_public_ip:80/info.php]().
    - If PHP is correctly installed and configured, you should see a page with PHP configuration details, meaning PHP is working with Apache.
- In `C:/Apache24/htdocs/`, create an `upload.php` file with the following content:
```
<?php
if ($_SERVER['REQUEST_METHOD'] == 'POST' && isset($_FILES['file'])) {
    $target_dir = "C:/Apache24/htdocs/";
    $target_file = $target_dir . basename($_FILES["file"]["name"]);

    file_put_contents("C:/Apache24/htdocs/upload_log.txt", print_r($_FILES, true));

    if (move_uploaded_file($_FILES["file"]["tmp_name"], $target_file)) {
        echo "Success";
    } else {
        echo "Error uploading file";
    }
} else {
    echo "No file uploaded";
    file_put_contents("C:/Apache24/htdocs/upload_log.txt", print_r($_FILES, true));
}
?>
```
- Restart Apache to ensure the changes take effect.

Your local HTTP server now works with a Minecraft server **hosted on a different machine**.
</details>
</details>

<details>
<summary><b>Create an Apache server on Linux (local access)</b></summary>

- Install Apache: `sudo apt update && sudo apt install apache2 -y`
- check that Apache is running or start it:
  ```
  systemctl status apache2
  sudo systemctl start apache2
  ```
    - You can disable Apache at startup: `sudo systemctl disable apache2`
- Set the correct permissions for the `/var/www/html` directory:  
  `sudo chmod -R 755 /var/www/html`
    - This allows the owner to read, write, and execute, while others can only read and execute.
- Set the correct ownership to make changes in `/var/www/html`:  
  `sudo chown -R <your_user>:www-data /var/www/html`  
  Do not reset the ownership for www-data, as the Minecraft server will need to directly edit the resource pack via an absolute path in this folder.
- Download the server resource pack in `/var/www/html/` and rename it `URLCustomDiscsPack.zip`.
- Restart Apache, then try to download the **server resource pack** with this URL:  
  [http://your_public_ip:80/URLCustomDiscsPack.zip]()

Your local HTTP server now works with a Minecraft server **hosted on the same machine**.
<details>
<summary><b>Extra steps for online access (Apache server on Linux)</b></summary>

- Install PHP: `sudo apt install php libapache2-mod-php`
- `php.ini` is the configuration file for PHP, where you can set file size limits, execution times, and more. In `php.ini`, change the values of the following lines to your desired limits:  
  `sudo nano /etc/php/*/apache2/php.ini`
    - For reference: If you plan to upload 50 music files of approximately 3 minutes (around 150MB), you should set the values between 200MB-300MB to be on the safe side.
    - Keep in mind that the more music files you add, the longer the upload process will take.
    - Note that if you add too many audio files (around 50+ depending on file size) and the pack takes too long to update, creating a new disc may fail, and removing one could crash the server (but will still function).
        - This defines the maximum size of files that can be uploaded.  
          `upload_max_filesize = 300M`
        - This value defines the maximum size for a POST request (it must be at least equal to upload_max_filesize).  
          `post_max_size = 300M`
        - This determines how long a PHP script can run before it is terminated. We increase it to 300 seconds (5 minutes) to allow more time for large uploads.  
          `max_execution_time = 300`
        - This defines the maximum time a script can spend receiving data. We set this to 300 seconds to accommodate large file uploads.  
          `max_input_time = 300`
- Set the correct ownership to make changes in `/var/www/html`:  
  `sudo chown -R <your_user>:www-data /var/www/html`
- In `C:/Apache24/htdocs/`, create an `info.php` file with the following content:  
  `<?php phpinfo(); ?>`
- Restart Apache, then try to access [http://your_public_ip:80/info.php]().
    - If PHP is correctly installed and configured, you should see a page with PHP configuration details, meaning PHP is working with Apache.
- In `/var/www/html/`, create an `upload.php` file with the following content:
```
<?php
if ($_SERVER['REQUEST_METHOD'] == 'POST' && isset($_FILES['file'])) {
    $target_dir = "/var/www/html/";
    $target_file = $target_dir . basename($_FILES["file"]["name"]);

    file_put_contents("/var/www/html/upload_log.txt", print_r($_FILES, true));

    if (move_uploaded_file($_FILES["file"]["tmp_name"], $target_file)) {
        echo "Success";
    } else {
        echo "Error uploading file";
    }
} else {
    echo "No file uploaded";
    file_put_contents("upload_log.txt", print_r($_FILES, true));
}
?>
```
- Reset the ownership for www-data to ensure the HTTP server can access the files:
  `sudo chown -R www-data:www-data /var/www/html`
- Restart Apache to ensure the changes take effect.

Your local HTTP server now works with a Minecraft server **hosted on a different machine**.
</details>
</details>

## Installation
1. Download the server resource pack in the directory of your personal HTTP server, as specified in the Servers Guide, to make it available for download and editing, then rename it to `URLCustomDiscsPack.zip`.
2. Download the `URLCustomDiscs.jar` plugin into your Minecraft server's `plugins` folder.
3. Start your Minecraft server so the plugin generates the `plugins/URLCustomDiscs` folder containing a `config.yml` file.
4. Open `plugins/URLCustomDiscs/config.yml` and set the following field to `pluginUsageMode: self-hosted`.
5. In the same config file, locate the **SELF-HOSTED MODE CONFIGURATION** section.  
   Follow the instructions to configure the plugin using the public IP address of your HTTP server.
6. Restart your Minecraft server to apply the configuration changes.
7. (Optional) Download the `ProtocolLib.jar` plugin into your Minecraft server's `plugins` folder for custom "Now Playing" toasts, then restart your Minecraft server.

## Dependencies Setup Guide
The Deno, yt-dlp and FFmpeg tools are automatically installed depending on the usage mode chosen in the `config.yml`.  
If your Minecraft server is running on Linux and the automatic executable permission doesn't work, you can grant execution permission to yt-dlp using a terminal or SSH connection with the following command: `chmod +x plugins/URLCustomDiscs/bin/yt-dlp`  
If you are using a shared Minecraft hosting provider:
- Many shared Minecraft hosting providers also run on Linux. Some of them may provide access to a web-based terminal or SSH, which might allow you to run commands like `chmod`, depending on the permissions granted.
- In some cases, it may also be possible to set the executable permission using the file manager in the hosting panel.
- Refer to the list of compatible shared Minecraft hosting providers (tested by me and/or users of the plugin) in the [Servers Guide](#servers-guide) to check if your host allows this.
    - If your host isn't listed, please refer to their documentation or contact their support to know whether setting executable permissions is supported.

# Edit-Only Mode Installation
The introductory remarks from the "Important" section of the Servers Guide in the Self-Hosted Mode Installation must be taken into account for the Edit-Only mode.

The Edit-Only Mode requires a brief setup to duplicate the reference resource pack to a custom path and rename it with a custom name.
### Steps to install with the Edit-Only Mode:
1. Download the `URLCustomDiscs.jar` plugin into your Minecraft server's `plugins` folder.
2. Start your Minecraft server so the plugin generates the `plugins/URLCustomDiscs` folder containing a `config.yml` file.
3. Open `plugins/URLCustomDiscs/config.yml` and set the following field to `pluginUsageMode: edit-only`
4. In the same config file, locate the **EDIT-ONLY MODE CONFIGURATION** section.  
   Set the `duplicatedZipFilePath` field to the path relative to the `plugins` folder, including the filename, where the reference resource pack must be duplicated.
5. Restart your Minecraft server to apply the configuration changes.  
   This will create the `plugins/URLCustomDiscs/edit-only_mode_reference_resource_pack` and `plugins/URLCustomDiscs/edit-only_mode_reference_resource_pack/duplicated_resource_pack` (default path for the duplicated resource pack) folders.
6. Download the appropriate resource pack for your Minecraft server version ([1.21](https://github.com/TheoDgb/URLCustomDiscs/releases/download/v2.7.2/URLCustomDiscsPack_1.21.zip) or [1.21.4+](https://github.com/TheoDgb/URLCustomDiscs/releases/download/v2.7.2/URLCustomDiscsPack_1.21.4.zip)), rename it to `URLCustomDiscsPack.zip`, then place it in the `plugins/URLCustomDiscs/edit-only_mode_reference_resource_pack` folder.
7. You can now use the create command: `/customdisc create <URL OR audio_name.mp3> <disc_name> <mono/stereo>`
8. (Optional) Download the `ProtocolLib.jar` plugin into your Minecraft server's `plugins` folder for custom "Now Playing" toasts, then restart your Minecraft server.

## Tree Structures
<details>
<summary><b>Understanding where/how everything goes/works</b></summary>

Your Minecraft server:
```
your_mc_server_folder/                    
├── plugins/                                        # create it if not already done
│   ├── URLCustomDiscs/                             # plugin folder, automatically created when the plugin is loaded
│   │   ├── audio_to_send/                          # used to create custom discs from MP3 files
│   │   │   └── MP3 files to send...
│   │   ├── bin/                                    # auto-installed external tools
│   │   │   ├── FFmpeg/
│   │   │   │   ├── ffmpeg
│   │   │   │   └── other FFmpeg files...
│   │   │   └── yt-dlp
│   │   ├── edit-only_mode_reference_resource_pack/
│   │   │   ├── duplicated_resource_pack/           # default duplicated resource pack folder
│   │   │   │   └── DuplicatedURLCustomDiscsPack.zip
│   │   │   └── URLCustomDiscsPack.zip              # downloaded and updated resource pack used as reference to duplicate it
│   │   ├── edit_resource_pack/                     # used to edit the resource pack
│   │   │   ├── temp_audio/
│   │   │   └── temp_unpacked/
│   │   ├── config.yml                              # after creating a first custom disc, allows you to retrieve the download URL of the resource pack
│   │   └── discs.json                              # stores information about custom discs, automatically created when creating a custom disc
│   └── URLCustomDiscs-1.0.0-1.21.0.jar             # downloaded URLCustomDiscs plugin
├── server.properties                               # used to configure download URL of the resource pack
└── other Minecraft server files...
```

1.21 URLCustomDiscsPack.zip server resource pack:
```
URLCustomDiscsPack.zip/
├── assets/
│   └── minecraft/
│       ├── models/
│       │   └── item/
│       │       ├── music_disc_13.json              # "overrides" added on disc 13 to assign custom music disc models using custom_model_data
│       │       └── custom_music_disc_example.json  # custom music disc models automatically created
│       ├── sounds/
│       │   └── custom/                             # custom music disc tracks are saved there
│       ├── textures/
│       │   └── item/
│       │       └── record_custom.png               # custom music discs texture
│       └── sounds.json                             # tracks automatically associated with custom music discs
└── pack.mcmeta
```

1.21.4+ URLCustomDiscsPack.zip server resource pack:
```
URLCustomDiscsPack.zip/
├── assets/
│   └── minecraft/
│       ├── items/
│       │   └── music_disc_13.json                  # "entries" added on disc 13 to assign custom music disc models using custom_model_data
│       ├── models/
│       │   └── item/
│       │       └── custom_music_disc_example.json  # custom music disc models automatically created
│       ├── sounds/
│       │   └── custom/                             # custom music disc tracks are saved there
│       ├── textures/
│       │   └── item/
│       │       └── record_custom.png               # custom music discs texture
│       └── sounds.json                             # tracks automatically associated with custom music discs
└── pack.mcmeta
```
</details>

## Support & Community
You're using the **Self-Hosted Mode** or the **Edit-Only Mode**, which gives you full control over hosting and configuration.  
Even if you're not relying on the **API** or the **resource pack hosting**, your support still helps me maintain and improve the plugin.  
If you'd like to contribute, please consider supporting the project on [Ko-fi](https://ko-fi.com/mcallium). Thank you! ❤️

For any questions, help, or suggestions, feel free to join the [Discord server](https://discord.gg/tdWztKWzcm)!

## Disclaimer
Please note that it is the sole responsibility of each user to comply with applicable copyright laws and the terms of service of any music provider when using this plugin. The developer of this plugin does not assume any liability for unauthorized use or violations of such laws and regulations.
