# F-Droid-Einreichung

Der [Merge Request !55598](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/55598)
wurde am 3. November 2026 geschlossen, weil BatteryNG noch keine drei Monate
aktiv entwickelt wurde. Eine erneute Einreichung ist nach Erreichen dieser
Frist möglich. Die damaligen CI-Pipelines im persönlichen GitLab-Fork hatten
wegen einer zusätzlichen GitLab-Identitätsprüfung keine Jobs ausgeführt.

Der öffentliche Quellcode liegt unter https://github.com/g3orgs/BatteryNG. Der Tag
`v0.12.0` zeigt auf Commit `e497dd53b78a47018ad8593fe76c9c8d2d97d67b`.
Die damalige Einreichung bezog sich auf Version 0.12.0 mit optionaler
App-Verbrauchsliste. Diese Funktion wurde in Version 0.12.4 entfernt; die
aktuellen Fastlane-Texte beschreiben nur noch den Absprungpunkt zu Androids
Akkuverbrauchsübersicht.

Für einen Merge Request an `fdroid/fdroiddata` die Datei
[`fdroiddata/de.batteryng.mvp.yml`](fdroiddata/de.batteryng.mvp.yml) als
`metadata/de.batteryng.mvp.yml` übernehmen. F-Droid baut und signiert die APK
aus dem getaggten Quellstand. Ein lokales Debug-APK ist kein Referenz-Binary
für einen reproduzierbaren Build.

Die F-Droid-Aufnahmekriterien nennen grundsätzlich drei Monate aktive
Entwicklung. Eine Ausnahme ist für Apps mit nachweisbaren Nutzern möglich.
Für die frühere Einreichung lag kein öffentlicher Nutzernachweis vor.

Die von F-Droid signierte APK hat einen anderen Signaturschlüssel als bisherige
lokale Debug-Installationen. Android kann diese Installationen daher nicht
direkt auf die F-Droid-Version aktualisieren.
