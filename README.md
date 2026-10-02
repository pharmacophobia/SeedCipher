# 🔐 SeedCipher — Encrypted Messaging via BIP39 Seed Words

> **Send encrypted SMS using a shared seed word — no servers, no accounts, no metadata.**

SeedCipher is an open-source Android app that scrambles your text messages using a BIP39 seed word as the encryption key. Messages are sent as regular SMS — the recipient just needs the same app and seed word to decode them. Fully offline. No sign-up required.

---

## ✨ Features

- **🔑 BIP39 Seed-Word Encryption** — Uses the same cryptographic seed standard as Bitcoin hardware wallets
- **💬 Default SMS Handler** — Integrates directly with Android's messaging system; send and receive encrypted SMS without switching apps
- **📬 MMS Support** — Works with both SMS and MMS
- **🚫 Zero Servers** — End-to-end encryption with no relay servers; messages go carrier → carrier
- **🔒 Plausible Deniability** — Encrypted messages look like random noise to anyone without the seed
- **📴 Fully Offline** — No internet permission required for core functionality

---

## 📱 Requirements

- Android 8.0 (API 26) or higher
- Must be set as Default SMS app to send/receive encrypted messages

---

## 🚀 Build & Install

```bash
git clone https://github.com/pharmacophobia/SeedCipher.git
cd SeedCipher
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or download the APK from [Releases](../../releases) and sideload it.

> **Tip**: After installing, go to **Settings → Apps → Default apps → SMS** and set SeedCipher as your default SMS app.

---

## 🔐 How It Works

1. You and the recipient agree on a **shared seed word** (e.g. a BIP39 mnemonic phrase)
2. SeedCipher derives a deterministic encryption key from that seed
3. Your message is encrypted before being handed to the SMS stack
4. The recipient's SeedCipher decrypts it on arrival using the same seed

---

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Cryptography**: BIP39 mnemonic → PBKDF2 key derivation → AES-256 encryption
- **SMS**: SmsManager + BroadcastReceivers (HeadlessSmsService, MmsReceiver)
- **UI**: Jetpack Compose + Material 3

---

## ⚠️ Disclaimer

This app is intended for legitimate personal privacy. Always comply with local laws regarding encrypted communications. The encryption is only as strong as how secretly you share your seed word.

---

## 📄 License

MIT License — free to use, modify, and distribute.
