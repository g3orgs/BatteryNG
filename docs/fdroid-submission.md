# F-Droid-Einreichung

Der öffentliche Quellcode liegt unter https://github.com/g3orgs/BatteryNG. Der Tag
`v0.12.0` zeigt auf Commit `e497dd53b78a47018ad8593fe76c9c8d2d97d67b`.
Die einmalige ADB-Freigabe für die optionale App-Verbrauchsliste wird in den
deutschen und englischen Fastlane-Texten erklärt. Die Live-Messung benötigt sie
nicht.

Für einen Merge Request an `fdroid/fdroiddata` die Datei
[`fdroiddata/de.batteryng.mvp.yml`](fdroiddata/de.batteryng.mvp.yml) als
`metadata/de.batteryng.mvp.yml` übernehmen. F-Droid baut und signiert die APK
aus dem getaggten Quellstand. Ein lokales Debug-APK ist kein Referenz-Binary
für einen reproduzierbaren Build.

Die F-Droid-Aufnahmekriterien nennen grundsätzlich drei Monate aktive
Entwicklung. Eine Ausnahme ist für Apps mit nachweisbaren Nutzern möglich.
BatteryNG wird bereits von weiteren Personen genutzt; für den Merge Request
sollte dazu ein belegbarer Hinweis ergänzt werden. Die endgültige Aufnahme
entscheidet das F-Droid-Team.

Die von F-Droid signierte APK hat einen anderen Signaturschlüssel als bisherige
lokale Debug-Installationen. Android kann diese Installationen daher nicht
direkt auf die F-Droid-Version aktualisieren.
