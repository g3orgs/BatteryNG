# BatteryNG 0.12.0

Kleine Android-App zur Live-Anzeige des **Netto-Akkustroms** und der Apps mit dem höchsten geschätzten Akkuverbrauch. Für die Rangliste sind **letzte 24 Stunden** oder **seit letzter erkannter Vollladung** wählbar; die Anzahl ist von 1 bis 10 einstellbar. Ein positiver Stromwert bedeutet Ladung, ein negativer Entladung.

## Was diese Version macht

- Liest `BatteryManager.BATTERY_PROPERTY_CURRENT_NOW` einmal pro Sekunde, solange die App im Vordergrund ist.
- Nutzt `BATTERY_PROPERTY_CURRENT_AVERAGE` als gekennzeichneten Fallback, wenn der Momentanwert nicht unterstützt wird.
- Rechnet die von Android gelieferten Mikroampere in Milliampere um. `0` ist ein gültiger Wert; `Long.MIN_VALUE` bedeutet nicht unterstützt.
- Im Zahnrad-Menü lässt sich die Strom-Einheit auf mA oder A einstellen. Hauptwert, Min/Max und schwebende Anzeige verwenden dieselbe Auswahl. mA erscheint ohne Tausendertrennzeichen (`+1318 mA`), A mit drei Dezimalstellen (`+1,318 A` bei deutscher Sprache). Die App-Verbrauchswerte bleiben in mAh, da sie eine andere Größe darstellen.
- Im Zahnrad-Menü lässt sich die Stromrichtung auf „Automatisch“, „Android-Standard“ oder „Vorzeichen umkehren“ stellen. Die automatische Erkennung vergleicht mehrere zeitlich getrennte Momentanwerte nur bei abgezogenem Stecker und Android-Status „Entlädt“. Bei Geräten mit vertauschtem Vorzeichen werden Hauptwert, Farbe, Balken, Smiley, Min/Max und Overlay gemeinsam korrigiert. Bis zur ersten sicheren Erkennung gilt der Android-Standard; „Vorzeichen umkehren“ wirkt sofort.
- Der aufgeräumte Startbildschirm zeigt Live-Wert, Richtung, dynamisches Akkusymbol mit Füllstand, Steckersymbol und optional App-Verbrauch. Messquelle und weitere Optionen liegen im Zahnrad-Menü.
- Das neue Batterie-Messinstrument-Icon ist als normales, rundes und adaptives Android-Launcher-Icon eingebunden. Die [generierte Vorlage](assets/BatteryNG-icon-master.png) und ihr [Prompt](assets/BatteryNG-icon-prompt.txt) liegen unter `assets/`.
- Die Stromkarte färbt sich bei negativem Netto-Akkustrom korallrot und bei positivem Strom smaragdgrün. Ein animierter Balken mit fünf Segmenten je Seite läuft bei Entladung nach links und bei Ladung nach rechts. Min/Max gelten für die aktuelle App-Sitzung.
- Links oben in der Stromkarte zeigt ein Smiley bei positivem Ladestrom eine grobe Einordnung: unter 400 mA rot und traurig, 400–999 mA orange und neutral, 1000–1499 mA gelb und leicht lächelnd, 1500–1999 mA hellgrün und freundlich, ab 2000 mA mintgrün und lachend. Ohne positiven Ladestrom bleibt der Smiley verborgen. Die Stufen verwenden den Netto-Akkustrom unabhängig von der gewählten Einheit mA oder A.
- Temperatur und verfügbare Akkudetails stehen in der Hauptansicht: Status, Anschluss, Füllstand, Zustand, Ladezyklen, Technologie, Kapazität, Spannung sowie Geräteangaben. Durch Tippen auf „Akku-Details“ lässt sich die Karte ein- und ausklappen; im Zahnrad-Menü kann sie vollständig ausgeblendet werden. Nicht verfügbare Werte werden als unbekannt angezeigt; die exakte maximale Akkukapazität bietet Android der App nicht verlässlich an.
- Die App-Verbrauchsliste lässt sich durch Tippen auf ihre Überschrift ein- und ausklappen oder im Zahnrad-Menü vollständig ausblenden. Die Verlaufserfassung läuft weiter, damit beim erneuten Einblenden Messpunkte vorhanden sind.
- Im Zahnrad-Menü sind Deutsch, Englisch oder Systemsprache sowie helles, dunkles oder Systemdesign wählbar.
- Im Zahnrad-Menü lässt sich eine kleine Stromanzeige über Launcher und anderen Apps starten oder stoppen. Sie liest den Wert alle zwei Sekunden und zeigt nur mA und bei Stromanschluss den Stecker.
- Solange BatteryNG geöffnet ist, lässt sich die schwebende Anzeige verschieben. Außerhalb der App bleibt sie an der gewählten Position fixiert und lässt Berührungen durch.
- Im Zahnrad-Menü lässt sich die Anzeige um 0°, 90°, 180° oder 270° drehen und auf „nur beim Aufladen zeigen“ einstellen. Ohne Stecker bleibt sie dann verborgen und erscheint beim Anschließen wieder.
- Über „Anzeige anpassen“ im Zahnrad-Menü sind Schriftgröße, Schriftfarbe, rechteckige Hintergrundfarbe und deren Deckkraft einstellbar. Bei 0 % Deckkraft ist der Hintergrund unsichtbar.
- Im unteren Bereich stehen die konfigurierbaren Top 1 bis Top 10 Apps mit Icon, Name und geschätzten mAh. Die App zeigt den gewählten Zeitraum, erfasste Dauer, Messlücken und den Stand der Auswertung an.

Der Live-Wert ist **kein USB-Eingangsstrom**. Die in den Akku-Details gezeigte Spannung ist die **Akkuspannung**, nicht die Spannung am USB-Anschluss. Aus dem Netto-Akkustrom lässt sich weder die USB-Eingangsspannung noch ein aktiver Power-Delivery-Modus zuverlässig ableiten. Android stellt einer normalen App dafür keine einheitliche öffentliche Schnittstelle bereit; gerätespezifische USB-/Type-C-Systemdateien können über ADB ausgelesen werden, waren auf dem Pixel 6 Pro aber aus der App nicht zugänglich. Für die tatsächliche Eingangsleistung empfiehlt sich ein USB-C-Leistungsmesser. Grün bedeutet positiven Netto-Akkustrom; Android liefert der App keinen verlässlichen, einheitlichen Schnelllade-Status. Die Messgenauigkeit und Aktualisierungsrate hängen vom Gerät und seiner Firmware ab. Die Hauptansicht misst nur im Vordergrund; die vom Nutzer eingeschaltete schwebende Anzeige nutzt einen Vordergrunddienst. Android verlangt dafür die Freigabe **„Über anderen Apps einblenden“**. Ein echtes Launcher-Widget ist dies nicht: Die Anzeige bleibt auch über anderen Apps sichtbar.

Androids Soll-Konvention lautet: positiver Akkustrom beim Laden, negativer beim Entladen. Einzelne Geräte können ein vertauschtes Vorzeichen liefern; ein [dokumentierter Xiaomi-Mi-8-Fall](https://github.com/MatteCarra/AccA/issues/27) zeigt einen negativen Rohwert bei „Charging“. Der Steckerstatus kommt separat vom Strommesswert. Ein negativer Wert **mit** angeschlossenem Kabel beweist allein keine falsche Polarität, weil der Akku bei schwacher Stromversorgung oder hoher Last tatsächlich entladen werden kann. Die automatische Korrektur nutzt deshalb nur stabile Werte **ohne** Kabel. Androids [BatteryManager-Referenz](https://developer.android.com/reference/android/os/BatteryManager) und [Health-HAL-Anforderungen](https://source.android.com/docs/core/perf/health/implementation-2-1) beschreiben die Vorzeichenkonvention.

Die Rangliste beruht auf von Android geschätzten **Akkuverbrauchswerten pro UID**, nicht auf Bildschirmzeit. BatteryNG speichert Messpunkte lokal im privaten App-Speicher und benötigt kein Netzwerk. Es misst beim Öffnen der App und plant einen regelmäßigen Hintergrundjob im Abstand von 15 Minuten; Android kann dessen Ausführung verschieben. Die Liste entsteht nach mindestens zwei Messpunkten. Nach Datenlücken, einem Zähler-Reset, einer Neuinstallation oder dem Löschen der App-Daten lässt sich die Vergangenheit nicht vollständig rekonstruieren. Die Rangliste berücksichtigt auch Apps im Arbeitsprofil. Werte für gemeinsam genutzte UIDs können nicht sicher einer einzelnen App zugeschrieben werden. Einzelheiten stehen in [docs/feature-top3-24h.md](docs/feature-top3-24h.md).

Für „seit letzter Vollladung“ gilt ein angeschlossener Akku mit **mehr als 96 %** als voll. BatteryNG speichert den erkannten Zeitpunkt und erfasst den Verbrauch ab dort; beim ersten Start mit bereits vollem Akku beginnt die Erfassung erst ab diesem Start. Ein kurzer Ladevorgang kann unerkannt bleiben, wenn die App samt Overlay beendet ist und Android den Hintergrundjob erst später ausführt. Die 24-Stunden-Ansicht bleibt von diesem Lade-Marker unabhängig.

## Einmalige Freigabe für die App-Auswertung

Für die App-Auswertung braucht BatteryNG auf dem Testgerät erweiterten Zugriff auf Androids Akku-Statistik und den Nutzungszugriff. Nach der Installation und bei verbundenem Gerät einmal am Computer ausführen:

```bash
adb shell pm grant de.batteryng.mvp android.permission.DUMP
adb shell pm grant de.batteryng.mvp android.permission.PACKAGE_USAGE_STATS
adb shell appops set de.batteryng.mvp GET_USAGE_STATS allow
```

Der Button **„Android-Nutzungszugriff öffnen“** im Zahnrad-Menü führt zur passenden Android-Einstellung. Nach einer Neuinstallation oder wenn Android eine Freigabe entzieht, die Schritte gegebenenfalls wiederholen. Die Live-Anzeige des Nettostroms funktioniert ohne diese drei Befehle.

Für die schwebende Anzeige ist zusätzlich **„Über anderen Apps einblenden“** nötig. Auf dem Pixel 6 Pro blockierte Android diese Freigabe für die per APK installierte App mit „App wurde Zugriff verweigert“. Bei ausdrücklich gewünschter schwebender Anzeige und verbundener ADB-Sitzung ließ sich die Freigabe so setzen:

```bash
adb shell appops set de.batteryng.mvp SYSTEM_ALERT_WINDOW allow
```

Danach BatteryNG erneut öffnen und die schwebende Anzeige im Zahnrad-Menü starten. Wenn **„Nur beim Aufladen zeigen“** aktiv ist, verschwindet sie ohne Stromanschluss wie eingestellt.

## Bauen und testen

Benötigt werden JDK 17 und ein Android SDK mit Plattform 34 und Build Tools 35.0.0. Das Gradle-Wrapper-Skript wird auf diesem NAS-Mount über `bash` gestartet:

```bash
ANDROID_HOME=/pfad/zum/Android/Sdk bash gradlew :app:testDebugUnitTest :app:exportDebugApk
```

Die installierbare Debug-APK liegt nach dem Build unter `dist/BatteryNG.apk`. Sie ist nur für Tests signiert. Installation auf einem per ADB verbundenen Gerät:

```bash
adb install -r dist/BatteryNG.apk
```

Stand des Tests: Version 0.12.0 wurde auf einem Xiaomi 2201123G mit Android 15/HyperOS 3 getestet. Ohne Kabel zeigte die automatische Vorzeichenkorrektur Entladung; nach erneutem Anschließen zeigte die App bei Android-Status „Lädt“ einen positiven Wert. 38 Unit-Tests bestehen. Ein [Screenshot der Hauptansicht](fastlane/metadata/android/de-DE/images/phoneScreenshots/1.png) liegt bei den öffentlichen App-Metadaten.

## Lizenz, Datenschutz und Veröffentlichung

BatteryNG, einschließlich des selbst erstellten Launcher-Icons, steht unter [Apache-2.0](LICENSE). Die Herkunft der Grafik ist unter [assets/README.md](assets/README.md) beschrieben. Der [Quellcode](https://github.com/g3orgs/BatteryNG) ist für eine öffentliche Veröffentlichung vorgesehen. Die [Datenschutzhinweise](PRIVACY.md) erläutern die optionalen Berechtigungen und lokalen Verlaufsdaten.

`dist/` enthält lokal gebaute Debug-APKs und `outputs/` interne Test- und Referenzdateien. Beide Ordner sind vom öffentlichen Quellstand ausgeschlossen. F-Droid erstellt und signiert die veröffentlichte APK aus einem getaggten Quellstand; die lokale Debug-APK ist nur zum Testen.
