## 1.21.0-1 URLCustomDiscs plugin (+ server resource pack)
Last updated on August 7, 2025.

<img src="https://github.com/TheoDgb/URLCustomDiscs/blob/main/media/URLCustomDiscs_icon.png?raw=true" alt="URLCustomDiscs Icon" style="width: 10%;">

## About
This plugin, along with a server resource pack, allows you to **create and play custom music discs from YouTube URLs or MP3 files** on your Minecraft server, with **real-time updates for players**.

Once installed, everything is done **in-game**, and there's no need to manually edit the resource pack files to add or remove discs.

The plugin supports Minecraft's spatial audio for music, but you can also play it in stereo.

Additionally, vanilla commands such as `/playsound` and `/stopsound` work with the custom music, so you do not need to use a disc in a jukebox.

**Important**: Make sure to use a direct video URL without any parameters (such as playlist, timecode, etc.), or you might get an unexpected result. Delete everything from the "&" in the URL.

**Note**:
- Plugin tested on 1.21.0-1 Spigot, Paper and Arclight servers  
- Currently works on Windows and Linux due to dependencies.  
- Partially works in 1.21.4+: discs can be created, deleted, and played using vanilla commands, but don't play as intended in a jukebox.

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
### **Warning**: ***YouTube blocked my API server***. This issue can be resolved by using a residential proxy, but it's costly for me. Thank you for your understanding.
In the meantime, you have **two alternative options**:
- **Use the built-in **yt-dlp** dependency (Minecraft server-side download)**  
  You can use the included **yt-dlp** dependency in the plugin. It will download the audio from the YouTube URL into the plugin folder and then send it to the API.
  > This method is **unlikely to work on shared Minecraft hosting providers**, such as Shockbyte, as they often do **not allow execution** of yt-dlp, or the IP ranges of those servers are likely **already banned by YouTube** (just like my API is at the moment).
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
This documentation focuses on the **Self-Hosted Mode**, which is intended for advanced users who prefer to manage everything manually, without relying on the default API setup.

If you want to use the default **API Mode** instead, please refer to the dedicated documentation from the URLCustomDiscs GitHub repository: [API Mode documentation](https://github.com/TheoDgb/URLCustomDiscs/blob/main/README.md)

For more details, you may explore the **API** documentation, source code and architecture in the [URLCustomDiscsAPI GitHub repository](https://github.com/TheoDgb/URLCustomDiscsAPI).

## Dependencies
### License & Attribution
The **URLCustomDiscsAPI** uses the following external tools:
- **yt-dlp**, from the [yt-dlp GitHub repository](https://github.com/yt-dlp/yt-dlp), licensed under the [Unlicense](https://github.com/yt-dlp/yt-dlp/blob/master/LICENSE)
- **FFmpeg**, from the [FFmpeg-Builds GitHub repository (Windows)](https://github.com/BtbN/FFmpeg-Builds) and the [FFmpeg Static Builds (Linux)](https://johnvansickle.com/ffmpeg/), licensed under the [GNU General Public License version 3 (GPLv3)](https://www.gnu.org/licenses/gpl-3.0.html)

yt-dlp and FFmpeg are not included in this project and must be installed separately.
### Tool Usage
- **yt-dlp**: downloads MP3 audio files from YouTube URLs.
- **FFmpeg**: converts MP3 files to Ogg Vorbis format for Minecraft compatibility.

## Download
The **URLCustomDiscs.jar** plugin and the **URLCustomDiscsPack.zip** server resource pack are available for download in the [Releases](https://github.com/TheoDgb/URLCustomDiscs/releases) section. <br>
They are also available on [Modrinth](https://modrinth.com/plugin/url-custom-discs) ([**Versions**](https://modrinth.com/plugin/url-custom-discs/versions)).

## Servers Guide
**Important**:
- For users who **don't want or can't host their Minecraft server on their own personal machine**, it's common to turn to online hosting providers.
  Keep in mind that you’ll still need a **personal HTTP server to host the resource pack** for the custom music discs to be added and updated automatically.
- If you're using **shared Minecraft hosting** such as Shockbyte or a similar provider, **make sure your host allows you to run binary files** (specifically `yt-dlp` and `FFmpeg`, in our case).
  **Some hosts may have restrictions** or require special permissions. Check their documentation or contact their support if you're not sure.
- To ensure full compatibility and control, it's highly recommended to use a **VPS (Virtual Private Server)** instead of shared Minecraft hosting.
  A VPS gives you full root access, allowing you to host both your Minecraft server and an HTTP server (such as Apache) on the same machine, making it ideal for directly serving the resource pack.
  > **Warning**:  
  If you intend to use `yt-dlp` to download YouTube audio directly from your server, you must ensure that the machine (including VPS) is **not part of an IP range currently blocked by YouTube**.  
  Shared Minecraft environments are often already blacklisted due to widespread abuse.  
  For greater reliability, consider using a **residential proxy with IP rotation**. This significantly reduces the risk of download failures and helps maintain consistent access to YouTube for audio retrieval.  
  In most cases, if your Minecraft server is running **locally on your personal machine**, `yt-dlp` works perfectly **without any proxy**, since residential IPs are rarely blocked by YouTube.
- This guide does **not** cover Apache HTTP server setup on a **VPS**.
  VPS environments require manual configuration (IP binding, port forwarding, firewall rules, etc.), which vary depending on the provider. However, the steps to set up Apache remain the same.
  Just make sure to open the necessary ports and configure your firewall correctly to allow HTTP traffic.

The following shared Minecraft hosting providers have been tested or confirmed to allow the execution of binary files (yt-dlp / FFmpeg):
- Compatible:
    - **ElypseCloud** allows you to upload custom binaries via FileZilla or SSH directly to your Minecraft server's plugin folder, and also lets you grant execution permissions through its built-in file manager (tested in 2025).
- Not compatible:
    - **Shockbyte** shared hosting environments block the execution of external or custom binaries (tested in 2025).
    - **exaroton** (tested in 2025)

**Note**: If you've confirmed compatibility or incompatibility with another shared Minecraft hosting provider, feel free to let me know on the [UrlCustomDiscs Discord](https://discord.gg/tdWztKWzcm) so I can update the list!

**THAT BEING SAID,**

You'll need to host the **URLCustomDiscsPack.zip** server resource pack on a personal HTTP server that the plugin can access and edit. Using an online file hosting service (such as [MCPacks](https://mc-packs.net/)) will not work.

Here are two tutorials for setting up a personal HTTP server (Windows / Linux). These tutorials cover how to make your personal local HTTP server work with both local and online Minecraft servers.

### Router Configuration
- Access your router's interface to:
    - configure a NAT/PAT rule for TCP port forwarding, setting both internal and external ports to 80, and using the **private IP address** of the machine running the Apache HTTP server;
    - open TCP port 80, which is the default for HTTP traffic, in your firewall to allow incoming connections to the machine running the Apache server.
<details>
<summary><b>More details and example with an Orange modem-router</b></summary>

- Access your router's interface
  <img src="/media/router_example_1_router_interface.png" alt="router_example_1_router_interface">
- In your router's network settings, configure a NAT/PAT rule to forward TCP port 80 (both internal and external) to the **private IPv4 address** of the machine running the Apache HTTP server.
    - On some routers, you may not be able to directly select a device by its name from a list. In that case, you'll need to manually enter the **private IPv4 address** of the machine running the Apache HTTP server. Make sure that this address is either statically assigned or reserved via DHCP on the router, so it doesn't change over time.
    - To find the **private IPv4 (or IPv6) address** of your machine:
        - on Windows, open the command prompt and type: ```ipconfig```
        - on Linux, open a terminal and type: ```ip a```

  <img src="/media/router_example_2_access_router_network.png" alt="router_example_2_access_router_network">
  <img src="/media/router_example_3_get_private_ipv4.png" alt="router_example_3_get_private_ipv4">
  <img src="/media/router_example_4_port_forwarding.png" alt="router_example_4_port_forwarding">
- In your router's firewall, open TCP port 80, which is the default for HTTP traffic, to allow incoming connections to the **private IPv4 (or IPv6) address** of the machine running the **Apache HTTP server**.
  <img src="/media/router_example_5_access_router_firewall.png" alt="router_example_5_access_router_firewall">
  <img src="/media/router_example_6_open_port.png" alt="router_example_6_open_port">
</details>

Router NAT Loopback Limitation (Local Access Issue): <br>
- If you are using a setup where the Apache HTTP server and the Minecraft game are running on **two different machines on the same local network**, and everything is correctly installed and configured but **you** (the server owner) **don't receive the server resource pack while other players do**, your router may not support **NAT loopback**. This means that if a machine on the local network tries to access the **public IP address** of the router to reach another local machine, the connection may fail. This issue **only affects connections from inside your local network**. <br>
- To work around this, you can manually download the server resource pack via the **private IPv4 address**: [http://your_private_ipv4/URLCustomDiscsPack.zip](). Then place it in your .minecraft/resourcepacks/ folder to use it as a classic resource pack. However, you'll need to **reinstall it manually** each time a disc is created or deleted in order to stay up to date.

### Personal Local HTTP Server
For the following steps, you'll need the **public IPv4 address** of your network (assigned by your ISP). You can quickly find it on websites like [WhatIsMyIp.com](https://www.whatismyip.com/). This is the address **external users will use to reach the machine running the Apache HTTP server**.
<details>
<summary><b>Create an Apache server on Windows</b></summary>

- Download Apache from [Apache Lounge](https://www.apachelounge.com/download/) (httpd-version.zip).
- Follow the ReadMe.txt instructions to set up your localhost Apache server.
    - If you are using a port other than 80, modify the "Listen 80" line to "Listen your_port" in Apache24/conf/httpd.conf.
- Download the **URLCustomDiscsPack.zip** server resource pack and place it in C:/Apache24/htdocs/
- In C:/Apache24/, create a .htaccess file with the following content:
```
<Files "URLCustomDiscsPack.zip">
	ForceType application/octet-stream
	Header set Content-Disposition "attachment; filename=URLCustomDiscsPack.zip"
</Files>
```
- Restart Apache, then try to download the **server resource pack** with this URL: <br>
  [http://your_public_ip:80/URLCustomDiscsPack.zip]()

Your **personal local HTTP server** now works with a **locally-hosted Minecraft server**.
<details>
<summary><b>Extra steps for an online-hosted Minecraft server (Apache server on Windows)</b></summary>

- Download PHP ([Thread Safe version zip](https://windows.php.net/downloads/releases/php-8.4.5-Win32-vs17-x64.zip)) from [php.net](https://windows.php.net/download/).
- Extract all the files from the zip archive to C:/php/
- In C:/php/, duplicate php.ini-development and rename the copy to "php.ini".
    - This is the configuration files for PHP, where you can set file size limits, execution times, and more.
- In php.ini, change the values of the following lines to your desired limits:
    - For reference: If you plan to upload 50 music files of approximately 3 minutes (around 150MB), you should set the values between 200MB-300MB to be on the safe side. Keep in mind that the more music files you add, the longer the upload process will take.
        - upload_max_filesize: This defines the maximum size of files that can be uploaded. <br>
          `upload_max_filesize = 300M`
        - post_max_size: This value defines the maximum size for a POST request (it must be at least equal to upload_max_filesize). <br>
          `post_max_size = 300M`
        - max_execution_time: This determines how long a PHP script can run before it is terminated. We increase it to 300 seconds (5 minutes) to allow more time for large uploads. <br>
          `max_execution_time = 300`
        - max_input_time: This defines the maximum time a script can spend receiving data. We set this to 300 seconds to accommodate large file uploads. <br>
          `max_input_time = 300`
- Open C:/Apache24/conf/httpd.conf
    - Locate the "LoadModule" lines and add the following line to load PHP: <br>
      `LoadModule php_module "C:/php/php8apache2_4.dll"`
        - Verify that the version "php8apache2_4.dll" corresponds to the one you downloaded and extracted.
    - Below this line, add the following line to specify the location of the php.ini file: <br>
      `PHPIniDir "C:/php"`
    - Locate the "AddType" lines and add the following line to associate PHP files: <br>
      `AddType application/x-httpd-php .php`
- In C:/Apache24/htdocs/, create an info.php file with the following content: <br>
  `<?php phpinfo(); ?>`
- Restart Apache, then try to access [http://your_public_ip:80/info.php]().
    - If PHP is correctly installed and configured, you should see a page with PHP configuration details, meaning PHP is working with Apache.
- In C:/Apache24/htdocs/, create an upload.php file with the following content:
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
</details>
</details>
<br>
<details>
<summary><b>Create an Apache server on Linux</b></summary>

- Install Apache: `sudo apt update && sudo apt install apache2 -y`
- check that Apache is running or start it:
  ```
  systemctl status apache2
  sudo systemctl start apache2
  ```
    - You can disable Apache at startup: `sudo systemctl disable apache2`
- Set the correct permissions for the /var/www/html directory: <br>
  `sudo chmod -R 755 /var/www/html`
    - This allows the owner to read, write, and execute, while others can only read and execute.
- Set the correct ownership to make changes in /var/www/html/: <br>
  `sudo chown -R <your_user>:<your_user> /var/www/html`
- Download the **URLCustomDiscsPack.zip** server resource pack and place it in the /var/www/html/ directory.
- Reset the ownership for www-data to ensure the HTTP server can access the files:
  `sudo chown -R www-data:www-data /var/www/html`
- Restart Apache, then try to download the **server resource pack** with this URL: <br>
  [http://your_public_ip:80/URLCustomDiscsPack.zip]()

Your **personal local HTTP server** now works with a locally-hosted **Minecraft server**.
<details>
<summary><b>Extra steps for an online-hosted Minecraft server (Apache server on Linux)</b></summary>

- Install PHP : `sudo apt install php libapache2-mod-php`
- php.ini is the configuration files for PHP, where you can set file size limits, execution times, and more. In php.ini, change the values of the following lines to your desired limits: <br>
  `sudo nano /etc/php/*/apache2/php.ini`
    - For reference: If you plan to upload 50 music files of approximately 3 minutes (around 150MB), you should set the values between 200MB-300MB to be on the safe side. Keep in mind that the more music files you add, the longer the upload process will take.
        - upload_max_filesize: This defines the maximum size of files that can be uploaded. <br>
          `upload_max_filesize = 300M`
        - post_max_size: This value defines the maximum size for a POST request (it must be at least equal to upload_max_filesize). <br>
          `post_max_size = 300M`
        - max_execution_time: This determines how long a PHP script can run before it is terminated. We increase it to 300 seconds (5 minutes) to allow more time for large uploads. <br>
          `max_execution_time = 300`
        - max_input_time: This defines the maximum time a script can spend receiving data. We set this to 300 seconds to accommodate large file uploads. <br>
          `max_input_time = 300`
- Set the correct ownership to make changes in /var/www/html/: <br>
  `sudo chown -R <your_user>:<your_user> /var/www/html`
- In C:/Apache24/htdocs/, create an info.php file with the following content: <br>
  `<?php phpinfo(); ?>`
- Restart Apache, then try to access [http://your_public_ip:80/info.php]().
    - If PHP is correctly installed and configured, you should see a page with PHP configuration details, meaning PHP is working with Apache.
- In /var/www/html/, create an upload.php file with the following content:
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
</details>
</details>

## Configuration Guide
- Download the **URLCustomDiscs.jar** plugin and place it in your Minecraft server's plugins folder.
- Download the **URLCustomDiscsPack.zip** server resource pack and place it in the directory of your personal local HTTP server, as specified in the Servers Guide, to make it available for download and editing.
- Start your Minecraft server to allow the plugin to generate the necessary files.
- In your Minecraft server folder, open plugins/URLCustomDiscs/config.yml and follow the instructions to properly configure the file.
- In your Minecraft server's `server.properties` file, locate the line `resource-pack=` and update it to include your download URL:
  `resource-pack=http://your_public_ip:80/URLCustomDiscsPack.zip`
    - You can also force the server resource pack to be downloaded for players by setting: <br>
      `require-resource-pack=true`
- Restart your Minecraft server.

## Dependencies Installation Guide
### Download **yt-dlp**
- Get the **yt-dlp** executable from [yt-dlp GitHub](https://github.com/yt-dlp/yt-dlp#installation):
    - [**Windows**](https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe)
    - [**Linux** (requires Python to run)](https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp)
    - [**Linux** (standalone but larger in size)](https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux)
        - Rename the standalone version to "yt-dlp".
- Place it in your_mc_server_folder/plugins/URLCustomDiscs/
- If your Minecraft server is on Linux, grant execution permissions to yt-dlp using a terminal or SSH connection with the following command: `chmod +x plugins/URLCustomDiscs/yt-dlp`
- If you are using a shared Minecraft hosting provider:
    - Many shared Minecraft hosting providers also run on Linux. Some of them may provide access to a web-based terminal or SSH, which might allow you to run commands like `chmod`, depending on the permissions granted.
    - In some cases, it may also be possible to set the executable permission using the file manager in the hosting panel.
    - Refer to the list of compatible shared Minecraft hosting providers (tested by me and/or users of the plugin) in the [Servers Guide](#servers-guide), to check if your host allows this.
        - If your host isn't listed, please refer to their documentation or contact their support to know whether setting executable permissions is supported.

### Download **FFmpeg**
- Get **FFmpeg**:
    - [**Windows** (Shared build)](https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl-shared.zip) from [FFmpeg-Builds GitHub repository](https://github.com/BtbN/FFmpeg-Builds/releases)
    - [**Linux** (Static build)](https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz) from [FFmpeg Static Builds](https://johnvansickle.com/ffmpeg/)
- Extract it in your_mc_server_folder/plugins/URLCustomDiscs/
- Rename the folder to "FFmpeg".
- If you downloaded the Linux static build, you must perform this extra step for compatibility with the plugin:
    - Create a new "bin" directory inside the "FFmpeg" folder.
    - Move all files and folders from FFmpeg/ into FFmpeg/bin/
- If your Minecraft server is on Linux, grant execution permissions to FFmpeg using a terminal or SSH connection with the following command: `chmod +x plugins/URLCustomDiscs/FFmpeg/bin/ffmpeg`
- If you are using a shared Minecraft hosting provider, the same notes apply as above for yt-dlp, regarding Linux-based hosting environments, terminal access, and executable permission setup.

## Tree Structures
<details>
<summary><b>Understanding where/how everything goes/works</b></summary>

Your Minecraft server:
```
your_mc_server_folder/                    
├── plugins/                            (create it if not already done)
│   ├── URLCustomDiscs/                 (automatically created when the plugin is loaded, plugin folder)
│   │   ├── editResourcePack/           (automatically created when the plugin is loaded, used to download, edit and upload the server resource pack)
│   │   ├── FFmpeg/                     (download FFmpeg)
│   │   │   ├── bin/...
│   │   │   ├── doc/...
│   │   │   ├── include/...
│   │   │   └── lib/...
│   │   ├── music/                      (automatically created when the plugin is loaded, used to download and convert YouTube music to Ogg)
│   │   ├── config.yml                  (automatically created when the plugin is loaded, allows you to configure the server resource pack)
│   │   ├── discs.json                  (automatically created when creating a custom music disc, stores information about custom music discs)
│   │   └── yt-dlp.exe                  (download yt-dlp)
│   └── URLCustomDiscs-1.0.0-1.21.0.jar (download the URLCustomDiscs plugin)
├── server.properties                   (Minecraft server's file, used to configure the resource pack download URL)
└── other Minecraft server folders and files...
```

URLCustomDiscsPack.zip server resource pack:
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
</details>

## Support & Community
You're using the **Self-Hosted Mode**, which gives you full control over hosting and configuration.  
Even if you're not relying on the **API** or the **resource pack hosting**, your support still helps me maintain and improve the plugin.  
If you'd like to contribute, please consider supporting the project on [Ko-fi](https://ko-fi.com/asson). Thank you! ❤️

For any questions, help, or suggestions, feel free to join the [Discord server](https://discord.gg/tdWztKWzcm)!

## Disclaimer
Please note that it is the sole responsibility of each user to comply with applicable copyright laws and the terms of service of any music provider when using this plugin. The developer of this plugin does not assume any liability for unauthorized use or violations of such laws and regulations.
