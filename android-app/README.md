# PostItDat - Android App

Aplikasi Android untuk menerima data dari GitHub repo (via webhook web).

## Cara Build APK

### Kebutuhan
- Android Studio (download: developer.android.com/studio)
- JDK 8+
- Koneksi internet saat pertama kali build (download dependencies)

### Langkah Build

1. **Buka Android Studio**
2. **Import Project**: File → Open → pilih folder `PostItDat`
3. **Tunggu Gradle sync** (otomatis download dependencies)
4. **Build APK**: Build → Build Bundle(s)/APK(s) → Build APK(s)
5. **APK tersimpan** di: `app/build/outputs/apk/debug/app-debug.apk`
6. **Install ke HP**: Transfer APK ke HP, aktifkan "Install dari sumber tidak dikenal", install

### Atau via Command Line (jika ada Java & Android SDK)
```bash
./gradlew assembleDebug
# APK ada di: app/build/outputs/apk/debug/app-debug.apk
```

## Setup Awal Aplikasi

Saat pertama buka app, akan minta konfigurasi:

1. **GitHub PAT**: Buat di `github.com/settings/tokens`
   - Pilih "Generate new token (classic)"
   - Centang: `repo` (Full control of private repositories)
   - Copy token
   
2. **GitHub Username**: Username GitHub kamu

3. **Nama Repo**: Nama repository untuk nyimpan data (harus sudah dibuat)

## Fitur

- ✅ Polling otomatis setiap 4 detik
- ✅ Notifikasi saat ada data baru + tombol "Terima" langsung dari notif
- ✅ Lihat semua folder sesuai struktur GitHub repo
- ✅ Preview teks & gambar sebelum diterima
- ✅ Tombol "Terima": simpan ke SDCard/Dataget/[folder]/ + hapus dari GitHub
- ✅ Berjalan di background, otomatis start setelah reboot
- ✅ Tema biru langit

## Struktur File di SDCard

```
/sdcard/Dataget/
├── anos/
│   ├── pesan.txt
│   └── foto.jpg
├── project2/
│   └── video.mp4
```

## Package
- `com.postit.dat`
- Min SDK: Android 7.0 (API 24)
- Target SDK: Android 14 (API 34)
