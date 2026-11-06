# BatteryNG

<img src="fastlane/metadata/android/en-US/images/icon.png" alt="BatteryNG app icon" width="128">

Watch the current flowing into or out of your Android battery, live. BatteryNG is open source and its battery features work offline.

[Download the APK from GitHub Releases](https://github.com/g3orgs/BatteryNG/releases) · [Privacy](PRIVACY.md) · [License](LICENSE)

## English

### What you see

- **Live battery current:** positive values mean the battery is charging; negative values mean it is discharging. The plug icon shows when external power is connected.
- **Clear visual feedback:** a left or right moving bar, battery level, temperature, and session minimum and maximum. Tap **Battery details** to expand or collapse the available Android battery information.
- **Optional floating display:** keep the current visible over the launcher and other apps. While BatteryNG is open you can drag it into place; you can also rotate it, change its text and background, adjust transparency, or show it only while plugged in.
- **Android battery usage:** open Android's own per-app battery report from Settings without ADB. Android chooses the report's time range.
- **Your preferences:** choose mA or A, English or German, light or dark theme, and automatic or manual current polarity correction for devices that report the sign backwards.
- **Manual update check:** compare the installed version with the latest GitHub release in Settings → Information, or open GitHub Releases in your browser.

<img src="fastlane/metadata/android/de-DE/images/phoneScreenshots/1.png" alt="BatteryNG main screen showing live current and battery details" width="300">

### Install and use

1. Download the current APK from [Releases](https://github.com/g3orgs/BatteryNG/releases) and install it on Android 6.0 or newer. Android may ask you to allow installation from the app you used to download it.
2. Open BatteryNG. Live current and battery details work immediately, without ADB or special access.
3. Open the **gear icon** for units, language, theme, polarity, the floating display, and a link to Android battery usage. Android's **Display over other apps** permission is required only for the floating display.

To enable the floating display, tap **Start floating display** and follow the app's prompts. On Android 13 and newer, BatteryNG opens **App info** first: choose **⋮ → Allow restricted settings**, return to BatteryNG, then select BatteryNG in Android's overlay app list and enable **Display over other apps**. This uses Android's settings screens and needs no ADB command. [Android 11 and newer open the app list rather than BatteryNG's permission page directly](https://developer.android.com/about/versions/11/privacy/permissions). [Google explains the extra confirmation for some locally installed apps here](https://support.google.com/android/answer/12623953?hl=en).

The displayed value is **net current at the battery**, not current or voltage at the USB charger. A connected cable can still coincide with a negative value if the battery is discharging. Green indicates positive battery current; it does not confirm fast charging or USB Power Delivery. Measurements depend on what your device reports to Android.

### Android battery usage

Tap **Open Android battery usage** in BatteryNG Settings to view Android's own per-app report. Android controls its time range and presentation. BatteryNG does not collect per-app usage data.

The 0.12.0 GitHub APK was **debug-signed**. Starting with 0.12.1, GitHub builds use a dedicated release key. Android cannot update a debug-signed installation with the new key: uninstall 0.12.0 first, which deletes its local settings. If BatteryNG becomes available on F-Droid, its builds will use a different key, so switching installation sources will also require reinstalling.

## Deutsch

### Das zeigt BatteryNG

- **Akkustrom live:** Positive Werte bedeuten, dass der Akku lädt; negative Werte zeigen Entladung. Das Steckersymbol zeigt eine angeschlossene Stromquelle.
- **Anschauliche Anzeige:** Ein Balken läuft bei Entladung nach links und beim Laden nach rechts. Akkustand, Temperatur sowie Minimum und Maximum der aktuellen Sitzung sind sichtbar. **Akku-Details** lassen sich durch Antippen ein- und ausklappen.
- **Optionale schwebende Anzeige:** Der Stromwert bleibt über dem Launcher und anderen Apps sichtbar. Solange BatteryNG geöffnet ist, kannst du die Anzeige verschieben. Drehung, Schrift, Hintergrund, Transparenz und „nur beim Aufladen zeigen“ sind einstellbar.
- **Android-Akkuverbrauch:** Öffne Androids eigene App-Verbrauchsübersicht aus den BatteryNG-Einstellungen, ohne ADB. Den Zeitraum der Übersicht legt Android fest.
- **Deine Einstellungen:** mA oder A, Deutsch oder Englisch, helles oder dunkles Design sowie automatische oder manuelle Korrektur des Stromvorzeichens.
- **Manuelle Updateprüfung:** Vergleiche unter Einstellungen → Information die installierte Version mit dem neuesten GitHub-Release oder öffne GitHub Releases im Browser.

### Installation und Nutzung

1. Lade die aktuelle APK unter [Releases](https://github.com/g3orgs/BatteryNG/releases) herunter und installiere sie auf Android 6.0 oder neuer. Android fragt möglicherweise nach der Erlaubnis für Installationen aus dieser Quelle.
2. Öffne BatteryNG. Die Live-Messung und Akku-Details funktionieren sofort, ohne ADB oder besondere Freigabe.
3. Über das **Zahnrad** erreichst du Einheiten, Sprache, Design, Vorzeichen, schwebende Anzeige und einen Link zu Androids Akkuverbrauchsübersicht. Nur für die schwebende Anzeige braucht Android die Freigabe **„Über anderen Apps einblenden“**.

Tippe für die Freigabe auf **Schwebende Anzeige starten** und folge den Hinweisen der App. Unter Android 13 und neuer öffnet BatteryNG zuerst die **App-Info**: Wähle dort **⋮ → Eingeschränkte Einstellungen zulassen**, kehre zu BatteryNG zurück, wähle BatteryNG in Androids App-Liste und aktiviere **„Über anderen Apps einblenden“**. Dafür ist kein ADB-Befehl nötig. [Ab Android 11 öffnet sich die App-Liste statt direkt der Freigabeseite von BatteryNG](https://developer.android.com/about/versions/11/privacy/permissions). Die [Android-Hilfe beschreibt die zusätzliche Bestätigung für manche lokal installierten Apps](https://support.google.com/android/answer/12623953?hl=de).

Der Wert ist der **Netto-Akkustrom**, nicht der Strom oder die Spannung am USB-Netzteil. Trotz angeschlossenem Kabel kann ein negativer Wert auftreten, wenn sich der Akku entlädt. Grün bedeutet positiven Akkustrom, bestätigt aber weder Schnellladen noch USB Power Delivery. Die Messwerte hängen vom Gerät und seiner Android-Implementierung ab.

### Android-Akkuverbrauch

Tippe in den BatteryNG-Einstellungen auf **Android-Akkuverbrauch öffnen**. Damit erscheint Androids eigene App-Verbrauchsübersicht. Zeitraum und Darstellung bestimmt Android. BatteryNG erfasst keine Verbrauchsdaten einzelner Apps.

Die GitHub-APK 0.12.0 war **debug-signiert**. Ab 0.12.1 verwenden GitHub-Builds einen eigenen Release-Schlüssel. Android kann die Debug-Installation mit dem neuen Schlüssel nicht aktualisieren: Deinstalliere 0.12.0 zuerst; dabei gehen ihre lokalen Einstellungen verloren. Falls BatteryNG bei F-Droid erscheint, werden dessen Builds einen anderen Schlüssel nutzen. Der Wechsel der Bezugsquelle erfordert dann ebenfalls eine Neuinstallation.

Die Akkufunktionen arbeiten offline. Nur die manuelle Updateprüfung nutzt Internetzugriff. Quellcode und App-Icon stehen unter [Apache-2.0](LICENSE); weitere Informationen findest du unter [Datenschutz](PRIVACY.md).
