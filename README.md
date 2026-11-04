# GK Audio App 🔊

Aapke 5 blogspot blogs ki audio app — ek hi app me saari categories,
audio playlist style me play hota hai.

| App me naam | Blog |
|---|---|
| Lucent GK | lucentaudiogk.blogspot.com |
| मांजर | manjarpart2.blogspot.com |
| विविध | miscellaneous578.blogspot.com |
| झारखंड | jharkhanddarpanaudio.blogspot.com |
| सार संग्रह | jharkhandsaarsangrahhindi.blogspot.com |

## Features
- **5 alag-alag categories** — bottom navigation me sab blogs alag-alag
- **Playlist playback** — ek post ke saare audio "भाग 1, 2, 3..." ki tarah
  ek-ek karke apne aap chalte hain
- **Screen band hone par bhi audio chalta rahega** — foreground media
  service + notification me play/pause controls
- **Auto-update** — blog me naya post daalte hi app me dikhega
  (app kholne par, tab badalne par, aur pull-to-refresh se)
- **Smooth start** — pichhla content cache me hota hai, list turant dikhti hai
- **Playback speed** 1×/1.25×/1.5×/1.75×/2×, seek bar, next/previous
- Earphone/Bluetooth hatane par auto-pause

---

## APK kaise banayein

### Tarika 1 (sabse aasan, bina kisi software ke): GitHub Actions
1. Apne GitHub account me **naya repository** banao (public).
2. Ye poora `GKAudioApp` folder us repo me **push/upload** kar do
   (GitHub Desktop app se, ya "uploading an existing file" se).
3. Repo me **Actions** tab kholo → "Build APK" workflow khud chal jayega.
4. Bug build ho jaye to us run me neeche **Artifacts** me
   `GK-Audio-Debug-APK` download ka option milega — APK download kar ke
   phone me install kar do.
5. Aage jab bhi code change karke push karoge, naya APK apne aap banega.

### Tarika 2: Android Studio (agar local build karna ho)
1. Android Studio (Hedgehog ya usse naya) install karo.
2. Project open karo. Gradle sync me dikkat ho to:
   - `File > Settings > Build, Execution, Deployment > Build Tools > Gradle`
   - "Use Gradle from" me **Specified location** chuno aur Gradle 8.9  
     (ya terminal me `gradle wrapper --gradle-version 8.9` chala do)
3. `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

> Note: wrapper files is machine me download nahi ho paye, is liye
> Tarika 1 (GitHub Actions) sabse aasan hai — usme kuch install nahi karna padta.

---

## Zaroori settings (audio smooth chalne ke liye)

1. **Battery optimization band karo is app ke liye:**
   Phone Settings → Apps → GK Audio → Battery →
   **Unrestricted / Allow background activity**.
2. App hamesha **online** chalti hai — audio blog aur GitHub par hai,
   net ke bina play nahi hoga (list offline cache se dikh sakti hai).

## Blog par audio post kaise karein (app me dikhne ka rule)

Har post ke HTML me ye tag hona chahiye (jaise abhi hai):
```html
<audio controls src="https://github.com/USER/REPO/raw/refs/heads/main/file.mp3" type="audio/mpeg"></audio>
```
- Post ka **title** app me card ka naam ban jata hai.
- Ek post me jitne bhi `<audio>` tags hain (order me), wo ek playlist
  ban jate hain — "भाग 1, भाग 2..." Khud se naam dena ho to `<figcaption>`
  me likho (jaise "Part 1").
- `<audio>` tag ke bina post publish kar do to app me us card par
  "ऑडियो जल्द ही आएगा" dikhega — baad me audio add karte hi card normal ho jayega.

## App ka naam/badge badalna

- Naam: `app/src/main/res/values/strings.xml` → `app_name`
- Package id: `app/build.gradle` → `applicationId` (warna versionCode bhi badal
  dena jab update bhejo)
