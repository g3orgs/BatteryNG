# Datenschutz und Berechtigungen

BatteryNG hat keine Internetberechtigung. Die App enthält keine Werbung,
Analysedienste oder Tracking-SDKs und sendet keine Messwerte an einen Server.

Die Live-Anzeige liest den Netto-Akkustrom, den Anschlussstatus und verfügbare
Akkudetails aus Androids `BatteryManager` und dem Batterie-Broadcast. Dafür ist
keine besondere Freigabe nötig.

Die schwebende Anzeige ist optional. Nur wenn sie gestartet wird, benötigt die
App „Über anderen Apps einblenden“ (`SYSTEM_ALERT_WINDOW`) und einen sichtbaren
Vordergrunddienst (`FOREGROUND_SERVICE` und `FOREGROUND_SERVICE_SPECIAL_USE`).

Die optionale App-Verbrauchsliste liest Androids Akku-Statistik lokal aus.
Dafür sind Android-Nutzungszugriff und einmalige ADB-Freigaben für `DUMP` und
`PACKAGE_USAGE_STATS` nötig. `QUERY_ALL_PACKAGES` dient dazu, die von Android
gelieferten App-UIDs Namen und Icons zuzuordnen. `RECEIVE_BOOT_COMPLETED`
ermöglicht das erneute Planen der lokalen Stichproben nach einem Neustart.
BatteryNG speichert diese Stichproben ausschließlich im privaten App-Speicher.
Das Ausblenden der Liste auf der Startseite stoppt die lokale Erfassung nicht.
Wird die erforderliche Freigabe entzogen, kann BatteryNG keine neuen
Verbrauchsstichproben erfassen. „App-Daten löschen“ in den Android-Einstellungen
entfernt die gespeicherten Stichproben und Einstellungen.

Die Verbrauchswerte sind Schätzungen. Sie können wegen Messlücken, gemeinsam
genutzter UIDs oder Android-Zähler-Resets unvollständig sein.
