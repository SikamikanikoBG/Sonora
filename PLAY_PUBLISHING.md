# Publishing Sonora to Google Play

This is the end-to-end checklist. The project is already set up to produce a
signed **Android App Bundle (.aab)** — the format Play requires — via the
`Play Release (AAB)` GitHub Actions workflow. You only need to plug in a signing
key and create the store listing.

## 1. One-time accounts & money
- **Google Play Developer account** — one-time **$25** fee. Sign up at
  https://play.google.com/console with the Google account you want to own the app.
- Identity verification (Google now asks for a real name/address; for a personal
  developer account this is you). Budget a day or two for verification.

## 2. Create the upload key (do this once, then guard it with your life)
Google signs the app you ship to users ("Play App Signing"). You sign the bundle
you upload with an **upload key**. Generate it once:

```bash
keytool -genkeypair -v \
  -keystore sonora-upload.jks \
  -keyalias sonora \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass <STORE_PASSWORD> -keypass <KEY_PASSWORD> \
  -dname "CN=Arsen Apostolov, O=Sonora, C=BG"
```

- Keep `sonora-upload.jks` and both passwords backed up somewhere safe (password
  manager). If you lose the upload key you can ask Google to reset it (because
  Play App Signing holds the real signing key), but don't rely on that.
- **Never commit the .jks** — `.gitignore` already blocks it.

## 3. Wire signing into CI (so tagging a release produces an uploadable AAB)
Add four repository secrets in GitHub → Settings → Secrets and variables → Actions:

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | `base64 -w0 sonora-upload.jks` output (the whole keystore, base64) |
| `KEYSTORE_PASSWORD` | the store password |
| `KEY_ALIAS` | `sonora` |
| `KEY_PASSWORD` | the key password |

With those present, the `Play Release (AAB)` workflow signs `app-release.aab`
with your upload key on every `vX.Y.Z` tag and attaches it to the GitHub Release.
Without them it still builds, but debug-signed (not uploadable).

## 4. Create the app in Play Console
1. **Create app** → name "Sonora", app (not game), free.
2. **App content**: fill in the required declarations:
   - **Privacy policy** URL (required). Sonora collects nothing and talks only to
     the user's own server — a short policy page is enough (host it on GitHub Pages).
   - **Data safety**: declare "no data collected / no data shared". True here.
   - **Ads**: none. **Content rating**: fill the questionnaire (Everyone).
   - **Target audience**: 13+ (not directed at children).
3. **Store listing**:
   - Short description (≤80 chars) + full description.
   - **App icon** 512×512 PNG (from `branding/` — export the logo tile).
   - **Feature graphic** 1024×500 PNG.
   - **Screenshots**: at least 2 phone screenshots (Home, Now Playing).

## 5. Upload & test
1. Start with the **Internal testing** track (instant, up to 100 testers by email
   — perfect for you and family). Upload the `.aab` from the Release.
2. Add your Google account as a tester, opt in via the link, install from Play.
3. When happy, promote the same build to **Closed** → **Open** → **Production**.
   First production review can take a few days.

## 6. Shipping updates
Bump `versionCode` (must increase every upload) and `versionName` in
`app/build.gradle.kts`, tag `vX.Y.Z`, and the workflow builds the signed AAB.
Upload it to the desired track (or automate later with the Play Developer API +
`r0adkll/upload-google-play`).

---

### Notes specific to Sonora
- Because it streams from a **self-hosted** server, it needs no backend from us and
  collects no user data — which makes the Data Safety and privacy sections trivial.
- The app uses cleartext HTTP (many home servers aren't HTTPS). Play allows this;
  just be aware reviewers may note it. Using a VPN/Tailscale or HTTPS reverse proxy
  is the recommended user setup and worth mentioning in the listing.
- Consider a distinct `applicationId` if you ever want a separate "pro" build; the
  current one is `com.sikamikaniko.sonora`.
