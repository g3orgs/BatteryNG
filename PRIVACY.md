# Datenschutz und Berechtigungen

BatteryNG benötigt Internetzugriff ausschließlich für die manuelle Updateprüfung
unter „Einstellungen → Information“. Erst nach einem Tipp auf „Nach Updates
suchen“ ruft die App die Versionsnummer des neuesten veröffentlichten Releases
von `api.github.com` ab. Der Link „GitHub Releases öffnen“ startet den Browser.
BatteryNG führt keine automatische Updateprüfung aus. Die App enthält keine
Werbung, Analysedienste oder Tracking-SDKs und sendet keine Messwerte an einen
Server.

Die Live-Anzeige liest den Netto-Akkustrom, den Anschlussstatus und verfügbare
Akkudetails aus Androids `BatteryManager` und dem Batterie-Broadcast. Dafür ist
keine besondere Freigabe nötig.

Die schwebende Anzeige ist optional. Nur wenn sie gestartet wird, benötigt die
App „Über anderen Apps einblenden“ (`SYSTEM_ALERT_WINDOW`) und einen sichtbaren
Vordergrunddienst (`FOREGROUND_SERVICE` und `FOREGROUND_SERVICE_SPECIAL_USE`).

BatteryNG erfasst keine Verbrauchsdaten einzelner Apps. Der Button „Android-Akkuverbrauch öffnen“ startet lediglich Androids eigene Übersicht. Von älteren BatteryNG-Versionen gespeicherte Stichproben werden nicht mehr fortgeführt; sie bleiben bis zum Löschen der App-Daten im privaten App-Speicher.
