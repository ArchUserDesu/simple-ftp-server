# Simple FTP Server

A tiny, ad-free FTP server for Android. Start or stop file sharing with one tap and access shared storage from another device on the same local network.

## Features

- Dark, single-screen interface
- Password-protected and anonymous modes
- Password visibility control
- Upload, download, rename, delete, and directory operations
- Active, passive, and extended passive FTP modes
- Foreground service with a persistent notification
- No ads, analytics, tracking, accounts, or third-party runtime libraries

## Install

Download the APK from [Releases](../../releases/latest), allow installation from your browser or file manager, and install it. Android will ask for notification and all-files access; both are used only to keep the user-started server visible and to serve shared storage.

The defaults are:

- Address: shown in the app after starting
- Port: `2121`
- Username: `ftp`
- Password: `ftp`
- Root directory: `/storage/emulated/0`

Change the default credentials before using the server. Anonymous mode allows anyone on the reachable network to read and modify shared files.

## Security

FTP sends credentials and files without encryption. Use this app only on a trusted local network, stop the server when finished, and never forward its port to the internet. For encrypted remote access, use SFTP instead.

Path canonicalization prevents FTP clients from navigating above the configured shared-storage root. Active-mode data connections are restricted to the control connection's peer address and non-privileged ports.

## Build

Requirements:

- JDK 17 or newer
- Android SDK Platform 35
- Gradle 8.9

```sh
gradle assembleDebug
```

The debug APK is written under `app/build/outputs/apk/debug/`. Release APKs must be signed with your own private key. Keystores and build output are intentionally excluded from Git.

Run the dependency-free protocol smoke tests with:

```sh
sh scripts/smoke-test.sh
```

## License

[MIT](LICENSE)
