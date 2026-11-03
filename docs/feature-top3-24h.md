# App-Akkuverbrauch nach Zeitraum

Status: Die Funktion ist in BatteryNG 0.12.0 enthalten. 24-Stunden-Ansicht, konfigurierbare Top-Anzahl, Zeitraumwechsel und ausblendbare Rangliste wurden auf Geräten geprüft. Ein echter Ladeübergang über 96 % und eine vollständig erfasste 24-Stunden-Spanne sind noch nicht beobachtet.

## Nutzeransicht

Im unteren Bereich der Hauptansicht erscheinen je nach Einstellung die 1 bis 10 Apps mit dem höchsten geschätzten Akkuverbrauch. Die Liste lässt sich für eine ruhigere Startseite vollständig ausblenden; die Hintergrundsammlung läuft weiter. Jede Zeile enthält Rang, Icon, Namen und mAh. Darüber stehen gewählter Zeitraum, erfasste Dauer, gegebenenfalls ein Hinweis auf Messlücken und der Zeitpunkt des letzten Messpunkts. Beim ersten Start wird zunächst ein Verlauf aufgebaut. Fehlende Berechtigungen und noch fehlende Messpunkte werden als eigener Zustand erklärt. Alle Optionen liegen im Zahnrad-Menü.

Bei „Letzte 24 Stunden“ reicht das Fenster von **jetzt minus 24 Stunden bis jetzt**. Ein kurzer Wechsel zwischen Laden und Entladen beginnt kein neues 24-Stunden-Fenster. Alternativ beginnt „Seit letzter Vollladung“ beim zuletzt erkannten Ladestand **über 96 % bei angeschlossenem Strom**. Die Rangliste beschreibt nur das, was Android einer App-UID als Verbrauch **aus dem Akku** zurechnet. Sie ist eine Diagnosehilfe und kein Beweis, dass eine App allein für hohen Akku-Drain verantwortlich ist.

## Datenquelle und Berechnung

BatteryNG verwendet Androids `dumpsys batterystats --usage --accumulated`. Der Parser liest aus „Estimated power use (mAh)“ nur die `(on battery, …)`-Komponenten der App-UIDs. Aggregate wie der Gesamtverbrauch des Geräts werden nicht als App-Verbrauch gezählt. Für UIDs anderer Android-Profile liest BatteryNG bei Bedarf die kompakte Paketzuordnung aus `dumpsys package --checkin packages`. Das sind von Android **geschätzte** mAh-Werte, keine direkte Strommessung einer einzelnen App. Die Live-Anzeige des Netto-Akkustroms stammt weiterhin aus `BatteryManager` und ist eine getrennte Messung.

BatteryNG speichert die kumulativen Werte je UID mit Zeitstempel im privaten App-Speicher (`battery_usage_history_v1.json`). Beim Öffnen der App wird ein neuer Messpunkt aufgenommen, sofern der letzte mindestens fünf Minuten zurückliegt. Ein persistierter `JobScheduler`-Job ist auf 15 Minuten eingestellt und wird nach Geräte-Neustart oder App-Update erneut geplant. Android kann Jobs insbesondere im Energiesparmodus verschieben; der genaue Abstand ist daher nicht garantiert. Die Sammlung läuft auch bei geschlossener Hauptansicht, sobald der Job eingerichtet wurde und die nötigen Rechte bestehen.

Aus aufeinanderfolgenden Messpunkten berechnet BatteryNG positive Differenzen je UID. Im 24-Stunden-Modus werden teilweise im Fenster liegende Intervalle zeitanteilig geschätzt. Im Vollladungs-Modus wird ein Intervall, das den Beginn des Fensters schneidet, vollständig ausgelassen; so gelangt kein Verbrauch von vor der Ladung in die Rangliste. Erkannte Zähler-Resets und Abstände über 45 Minuten werden aus der Rangliste ausgeschlossen und als Lücke angezeigt. Auch bei Neustart oder veralteten Messpunkten weist die Anzeige auf Lücken hin. Negative Differenzen werden nicht als negativer Verbrauch ausgegeben. Der lokale Verlauf übersteht einen App- oder Geräte-Neustart, solange die App-Daten erhalten bleiben. Seit 0.5.0 bleiben Messpunkte ab dem letzten erkannten Vollladungszeitpunkt auch über 24 Stunden erhalten.

Die Anzeige unterscheidet die erfasste Dauer von vollständigen 24 Stunden. Erst bei 24 Stunden ohne markierte Lücke wird die Abdeckung als vollständig bezeichnet. Nach der Erstinstallation, einer Datenlöschung, entzogenen Rechten oder einem nicht erfassten Zeitraum fehlen Messpunkte; vergangene UID-Werte lassen sich dann nicht verlässlich nachtragen. Die Größe eines Zeitintervalls und die lineare Aufteilung am Rand des Fensters begrenzen die Genauigkeit.

Im Modus „Seit letzter Vollladung“ wird ein voller Akku bei **mehr als 96 % und angeschlossener Stromversorgung** einmal je Anschluss-Sitzung erkannt. BatteryNG speichert den Zeitpunkt und versucht sofort einen neuen UID-Zählerstand als Basis zu erfassen. Beim ersten Start mit bereits über 96 % beginnt diese Basis **erst beim Erkennen**, nicht rückwirkend am tatsächlichen Beginn des Ladevorgangs. Die App prüft den Zustand beim Öffnen, im laufenden Overlay und beim Hintergrundjob. Ein sehr kurzer Ladevorgang kann bei vollständig beendetem Prozess zwischen zwei verzögerten Jobs unerkannt bleiben, weil [Androids Hintergrund-Broadcast-Grenzen](https://developer.android.com/about/versions/oreo/background) die Zustellung des Stromanschluss-Broadcasts an manifestregistrierte Receiver nicht garantieren. Die App markiert fehlende Messintervalle als Lücke.

## Einmalige Einrichtung auf dem Testgerät

Die App benötigt die Android-Berechtigungen `DUMP` und `PACKAGE_USAGE_STATS` sowie den App-Op `GET_USAGE_STATS`. Nach Installation auf dem per ADB verbundenen Gerät einmal ausführen:

```bash
adb shell pm grant de.batteryng.mvp android.permission.DUMP
adb shell pm grant de.batteryng.mvp android.permission.PACKAGE_USAGE_STATS
adb shell appops set de.batteryng.mvp GET_USAGE_STATS allow
```

Die Schaltfläche **„Android-Nutzungszugriff öffnen“** im Zahnrad-Menü öffnet zusätzlich die entsprechende Systemeinstellung. Der normale Android-Berechtigungsdialog kann den für `dumpsys` benötigten Zugriff nicht selbst erteilen. Ohne Freigabe zeigt BatteryNG einen Berechtigungshinweis; die Live-mA-Messung bleibt davon unabhängig.

## Grenzen

- Androids Verbrauchsschätzung kann je nach Gerät und Firmware von der tatsächlichen Energie abweichen. Kleine Differenzen sind besonders unsicher.
- Eine UID kann mehrere Pakete teilen. BatteryNG darf deren gemeinsamen Wert nicht willkürlich als Verbrauch genau einer App ausgeben. System-UIDs ohne eindeutig zuordenbare App erscheinen nicht als gewöhnliche App.
- Die Rangliste berücksichtigt App-UIDs aus dem persönlichen und dem Arbeitsprofil. Bei geteilter UID werden mehrere App-Namen zusammen angezeigt und ein Ersatz-Icon verwendet; bei einem unbekannten Paket steht die UID statt eines App-Namens. Ein App-Icon des Arbeitsprofils erscheint, sofern Android dessen Paketmetadaten für BatteryNG bereitstellt.
- Der Verlauf ist lokal. Neuinstallation oder „App-Daten löschen“ entfernt ihn. Ein fehlendes Intervall kann nicht aus einem späteren kumulativen Wert auf einzelne Stunden verteilt werden, ohne zu schätzen.
- 24 Stunden **Kalenderzeit** sind gemeint; Ladezeiten gehören zum Zeitfenster, liefern aber keinen „on battery“-Verbrauch.

## Abnahme

1. Die Reihenfolge basiert auf geschätzten mAh für das gewählte Fenster und nicht auf Nutzungsdauer.
2. Nach kurzem Anschließen und Abziehen des Ladegeräts bleiben bereits erfasste Messpunkte erhalten.
3. App- und Geräte-Neustart erhalten den lokal gespeicherten Verlauf. Rechteverlust, Zähler-Reset und Messlücken werden sichtbar statt stillschweigend als vollständige 24 Stunden behandelt.
4. Icon und Name passen zur UID; ein gemeinsamer UID-Wert wird nicht fälschlich einer einzelnen App zugeschrieben.
5. Fehlende Berechtigungen und weniger als 24 Stunden erfasste Zeit sind in der Oberfläche erkennbar.

## Bisher geprüft

Version 0.5.0 ist gebaut und auf dem Pixel 8 installiert; alle 22 Unit-Tests bestehen. Die Freigaben `DUMP`, `PACKAGE_USAGE_STATS` und `GET_USAGE_STATS` sind erhalten. Die 24-Stunden-Ansicht zeigt drei Einträge mit Icons und mAh-Schätzung sowie den bisher erfassten Teil des Fensters. Top 5 und „Seit letzter Vollladung“ lassen sich wählen; solange noch keine Vollladung erkannt wurde, erscheint der passende Wartezustand.

Das 90°-Overlay wurde auf dem Gerät an den Bildschirmrand gezogen und auf dem Launcher geprüft. Mit kurz simuliertem USB-Anschluss erschien es bei aktivem „Nur beim Aufladen zeigen“ und verschwand nach dem Zurücksetzen des simulierten Batteriezustands wieder. Der simulierte Anschluss bei 96 % löste keinen falschen Vollladungs-Reset aus. Die Testeinstellungen und der echte Batteriezustand wurden anschließend wiederhergestellt. Eine tatsächliche Ladung über 96 % und ein voller 24-Stunden-Verlauf bleiben für einen Langzeittest offen.

In 0.6.0 wurde das Ausblenden der Rangliste auf dem Pixel 8 geprüft; die laufende Erfassung bleibt dabei eingerichtet. Der Build und 25 Unit-Tests bestehen.

Die technische Zugangsbeschränkung ist in der [Android-Berechtigungsreferenz](https://developer.android.com/reference/android/Manifest.permission#BATTERY_STATS), [AOSP-BatteryStatsManager](https://android.googlesource.com/platform/frameworks/base/+/main/core/java/android/os/BatteryStatsManager.java), [AOSP-BatteryStatsService](https://android.googlesource.com/platform/frameworks/base/+/main/services/core/java/com/android/server/am/BatteryStatsService.java) und [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager) beschrieben.
