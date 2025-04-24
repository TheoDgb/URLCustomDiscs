## 1.21+ URLCustomDiscs plugin (+ server resource pack)
Last updated on April 24, 2025.

<img src="https://github.com/TheoDgb/URLCustomDiscs/blob/main/media/URLCustomDiscs_icon.png?raw=true" alt="URLCustomDiscs Icon" style="width: 10%;">

## About
This plugin, along with a required server resource pack, allows you to create and play custom music discs from YouTube URLs on your Minecraft server, with real-time updates for players.

Once installed, everything is done in-game, and there's no need to manually edit the server resource pack files to add or remove discs.

The plugin supports Minecraft's spatial audio for music, but you can also play it in stereo.

Additionally, Vanilla commands such as `/playsound` and `/stopsound` work with the custom music, so you do not need to use a disc in a jukebox.

Important:
- Currently works on Windows and Linux due to dependencies.
- Make sure to use a direct video link without parameters (such as playlist, timecode, etc.), or you might be in for a surprise. Delete everything starting from & in the URL.

Note: plugin tested on 1.21+ Spigot, Paper and Arclight servers

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
- **yt-dlp** to download an MP3 audio file from a YouTube URL.
- **FFmpeg** to convert MP3 format to Ogg format.
- A **personal local HTTP server** hosting the server resource pack, which allows:
  - the plugin to edit the server resource pack via an absolute path for locally-hosted Minecraft server;
  - the plugin to edit the server resource pack via download and upload (POST method) for online-hosted Minecraft server;
  - players to download the server resource pack and receive real-time updates for custom music discs.

### License And Attribution
This plugin uses **yt-dlp** ([unlicense](https://github.com/yt-dlp/yt-dlp/blob/master/LICENSE)) and **FFmpeg** from the [FFmpeg.org](http://ffmpeg.org/) under the [LGPLv2.1](https://www.gnu.org/licenses/lgpl-2.1.html).<br>
yt-dlp and FFmpeg are not included in this project and must be installed separately.

## Download
The **URLCustomDiscs.jar** plugin and the **URLCustomDiscsPack.zip** server resource pack are available for download in the [**Releases**](https://github.com/TheoDgb/URLCustomDiscs/releases) section. <br>
It is also available on [Modrinth](https://modrinth.com/plugin/url-custom-discs) ([**Versions**](https://modrinth.com/plugin/url-custom-discs/versions)).

## Servers Guide
Important:
- For users who don't want or can't host their Minecraft server on their own personal machine, it's common to turn to online hosting providers.
  Keep in mind that you’ll still need a personal HTTP server to host the resource pack for the custom music discs to be added and updated automatically.
- If you're using shared hosting like Shockbyte or a similar provider, **make sure your host allows you to run binary files** (for yt-dlp and FFmpeg, in our case).
  Some hosts may have restrictions or require special permissions. Check their documentation or contact their support if you're not sure.
- For full control and compatibility, it's highly recommended to use a VPS (Virtual Private Server), which gives you full root access, instead of shared Minecraft hosting.
  With a VPS, you can host both your Minecraft server and an (Apache) HTTP server on the same machine, making it ideal for serving the resource pack directly.
  The Apache server setup on a VPS is not covered in this guide (locally only). Configuring the IP, ports, firewall, and other related settings is the user's responsibility, as these can vary depending on the VPS provider. However, the steps to set up an Apache server remain the same on a VPS.
  Just make sure to configure your firewall and open the necessary ports, as VPS environments usually require manual network setup.

You'll need to host the **URLCustomDiscsPack.zip** server resource pack on a **personal local HTTP server** that the plugin can access and edit. Using an online file hosting service (such as [MCPacks](https://mc-packs.net/)) will not work.

Here are two tutorials for setting up a **personal local HTTP server** (Windows / Linux). These tutorials cover how to make your **personal local HTTP server** work with both local and online Minecraft servers.

### Router Configuration
- Access your router's configuration interface to:
  - configure a NAT/PAT rule for TCP port forwarding, setting both internal and external ports to 80, and using the public IP address of the machine running the Apache HTTP server (you can quickly find it on websites like [WhatIsMyIp.com](https://www.whatismyip.com/));
  - open TCP port 80, which is the default for HTTP traffic, in your firewall to allow incoming connections.

### Personal Local HTTP Server
<details>
<summary><b>Create an Apache server on Windows</b></summary>

- Download Apache from [Apache Lounge](https://www.apachelounge.com/download/) (httpd-version.zip).
- Follow the ReadMe.txt instructions to set up your localhost Apache server.
  - If you are using a port other than 80, modify the "Listen 80" line to "Listen your_port" in Apache24/conf/httpd.conf.
- Download the URLCustomDiscsPack.zip server resource pack and place it in C:/Apache24/htdocs/
- In C:/Apache24/, create a .htaccess file with the following content:
```
<Files "URLCustomDiscsPack.zip">
	ForceType application/octet-stream
	Header set Content-Disposition "attachment; filename=URLCustomDiscsPack.zip"
</Files>
```
- Restart Apache, then try to download the server resource pack with this URL: <br>
  [http://your_public_ip:80/URLCustomDiscsPack.zip]()

Your **personal local HTTP server** now works with a locally-hosted Minecraft server.
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
- Restart Apache, then try to access http://your_public_ip:80/info.php.
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
- Download the URLCustomDiscsPack.zip server resource pack and place it in the /var/www/html/ directory.
- Reset the ownership for www-data to ensure the HTTP server can access the files:
  `sudo chown -R www-data:www-data /var/www/html`
- Restart Apache, then try to download the server resource pack with this URL: <br>
  [http://your_public_ip:80/URLCustomDiscsPack.zip]()

Your **personal local HTTP server** now works with a locally-hosted Minecraft server.
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
- Restart Apache, then try to access http://your_public_ip:80/info.php.
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
- Download the **URLCustomDiscsPack.zip** server resource pack and place it in the directory of your **personal local HTTP server**, as specified in the Servers Guide, to make it available for download and editing.
- Start your Minecraft server to allow the plugin to generate the necessary files.
- In your Minecraft server folder, open plugins/URLCustomDiscs/config.yml and follow the instructions to properly configure the file.
- In your Minecraft server's server.properties file, locate the line "resource-pack=" and update it to include your download URL:
  `resource-pack=http://your_public_ip:80/URLCustomDiscsPack.zip`
  - You can also force the server resource pack to be downloaded for players by setting: <br>
    `require-resource-pack=true`
- Restart your Minecraft server

## Dependencies Installation Guide
- Download the **yt-dlp** executable from [yt-dlp GitHub](https://github.com/yt-dlp/yt-dlp#installation) ([**Windows**](https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe), [**Linux (requires Python to be installed)**](https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp), [**Linux (standalone but larger in size and must be renamed to "yt-dlp")**](https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux)) and place it in your_mc_server_folder/plugins/URLCustomDiscs/
  - If your Minecraft server is on Linux, grant execution permissions to yt-dlp with: `chmod +x yt-dlp`
- Download **FFmpeg** from [FFmpeg GitHub](https://github.com/BtbN/FFmpeg-Builds/releases) ([**Windows**](https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl-shared.zip), [**Linux**](https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-linux64-gpl-shared.tar.xz)), extract it in your_mc_server_folder/plugins/URLCustomDiscs/, and rename the folder to "FFmpeg":
  - Example for Windows users:
    - Download "ffmpeg-master-latest-win64-gpl-shared.zip".
    - Extract the .zip archive in your_mc_server_folder/plugins/URLCustomDiscs/
    - Rename the "ffmpeg-master-latest-win64-gpl-shared" folder to "FFmpeg".

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

## Disclaimer
Please note that it is the sole responsibility of each user to comply with applicable copyright laws and the terms of service of any music provider when using this plugin. The developer of this plugin does not assume any liability for unauthorized use or violations of such laws and regulations.