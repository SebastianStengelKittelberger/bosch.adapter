# Generic Adapter – Implementierungsplan

## Ziel

Ein konfigurierbarer REST-Adapter, der **beliebige JSON-REST-APIs** in das
illusion-`Product`-Schema (`List<Attribute>`) überführt und die Ergebnisse
direkt in Elasticsearch schreibt – ohne Code-Änderungen, nur durch Konfiguration.

**Demo-tauglich in 2 Schritten:**
1. REST-URL eingeben
2. SKU-Feld auswählen (+ ggf. verschachtelten SKU-Array benennen)
→ Produkte landen automatisch in Elasticsearch

---

## Kernprinzip: Konvention über Konfiguration

Feldname der Quell-API → uppercase → UKEY

```
{ "voltage": 230 }  →  Attribute { ukey: "VOLTAGE", references: { "TEXT": "230" } }
```

Kein manuelles Mapping nötig für die Demo. Feintuning (Umbenennung, Typ-Override,
Transformation) kann nachträglich in der AdapterConfig ergänzt werden.

---

## Architektur

```
REST-Quelle (beliebige API)
        ↓  HTTP GET
GenericRestFetcher          – lädt JSON, paginiert falls nötig
        ↓
JsonProductExtractor        – erkennt Produkt-/SKU-Ebene anhand Config
        ↓
AttributeMapper             – Feldname → Attribute (Default: uppercase = UKEY)
        ↓
Product (illusion-Schema)
        ↓
ElasticsearchAdapterWriter  – schreibt in "adapter-raw" Index
```

Parallel dazu:

```
AdapterConfigController     – REST-Endpoint zum Anlegen/Bearbeiten von AdapterConfigs
AdapterRunController        – REST-Endpoint zum manuellen Triggern eines Adapter-Runs
```

---

## Datenmodell: AdapterConfig

```java
public record AdapterConfig(
    String id,                    // z.B. "my-erp-adapter"
    String displayName,           // Anzeigename
    String sourceUrl,             // https://api.example.com/products
    String authType,              // NONE, BEARER, BASIC, API_KEY
    String authValue,             // Token / Credentials (verschlüsselt)

    StructureConfig structure,    // Wie ist die JSON-Struktur aufgebaut?
    List<FieldMappingConfig> fieldMappings,  // Optional: Overrides, leer = alles auto
    
    SyncConfig sync               // Wann/wie oft synchronisieren?
)

public record StructureConfig(
    String skuField,              // Pflicht: welches Feld ist die SKU/ID? z.B. "sku"
    String skuArrayPath,          // Optional: Pfad zum SKU-Array, z.B. "skus" (bei verschachteltem JSON)
    String productIdField,        // Optional: Produkt-ID wenn verschachtelt, z.B. "id"
    String paginationParam,       // Optional: z.B. "page" (Query-Parameter für Paginierung)
    Integer pageSize              // Optional: z.B. 100
)

public record FieldMappingConfig(
    String sourceField,           // Quellfeld, z.B. "weight_g"
    String ukey,                  // Ziel-UKEY, z.B. "WEIGHT_KG" (überschreibt Default)
    String referencesKey,         // "TEXT", "BOOLEAN", "NUMBER" (Default: "TEXT")
    TransformConfig transform     // Optional: Transformation
)

public record TransformConfig(
    String type,                  // NONE, DIVIDE, MULTIPLY, UPPERCASE, SPLIT
    Object value                  // z.B. 1000 bei DIVIDE
)

public record SyncConfig(
    String mode,                  // MANUAL, SCHEDULED
    String cronExpression         // Optional: "0 0 * * *" (täglich)
)
```

---

## Komponenten

### 1. GenericRestFetcher

Lädt JSON von der konfigurierten URL.

- Unterstützt Auth: NONE, Bearer Token, Basic Auth, API-Key (Header)
- Paginierung: Query-Parameter-basiert (`?page=0&size=100`), solange Response nicht leer
- Gibt `JsonNode` (Jackson) zurück – noch kein Mapping

```
GET {sourceUrl}?{paginationParam}=0&size={pageSize}
GET {sourceUrl}?{paginationParam}=1&size={pageSize}
...bis leeres Array
```

### 2. JsonProductExtractor

Erkennt anhand `StructureConfig` die Produkt- und SKU-Objekte.

**Fall A: Flaches Array (kein `skuArrayPath`)**
```json
[ { "sku": "A1", "voltage": 230 }, { "sku": "A2", "voltage": 110 } ]
```
→ Jedes Element = ein `Product` mit einer SKU (`skuField` = SKU-Code)

**Fall B: Verschachteltes Array (`skuArrayPath` gesetzt)**
```json
[ { "id": "P1", "name": "Bohrmaschine", "skus": [ {"sku": "A1"}, {"sku": "A2"} ] } ]
```
→ Root-Objekt = Produkt-Metadaten, `skus[]` = SKU-Liste

**Fall C: Einzelnes Objekt (kein Array)**
→ Wird als einzelnes Produkt behandelt

### 3. AttributeMapper

Kernstück. Wandelt `JsonNode`-Felder in `List<Attribute>` um.

**Default-Verhalten (kein FieldMappingConfig vorhanden):**
```
Feldname → .toUpperCase().replace("-", "_") = UKEY
Wert     → references.put("TEXT", value.asText())
Boolean  → references.put("BOOLEAN", value.asBoolean())
Zahl     → references.put("TEXT", value.asText())  + references.put("NUMBER", value.asDouble())
```

**Mit FieldMappingConfig (Override):**
- Anderer UKEY-Name
- Anderer referencesKey
- Transformation (z.B. DIVIDE 1000)

Verschachtelte Objekte und Arrays werden **flach ignoriert** (zu komplex für Demo-Phase),
können aber als JSON-String in `references.put("TEXT", ...)` gespeichert werden.

### 4. ElasticsearchAdapterWriter

Schreibt fertige `Product`-Objekte in einen dedizierten ES-Index: `adapter-{adapterId}-raw`.

- Index-Name enthält Adapter-ID → mehrere Adapter koexistieren
- Bulk-Insert für Performance
- Upsert (nicht Insert) anhand SKU-Feld → idempotent, wiederholbar

```json
// Elasticsearch Dokument
{
  "_id": "{skuCode}",
  "productId": "P1",
  "productMetaData": { "name": "...", "id": 1 },
  "skuMetaData": { "sku": "A1", "name": "..." },
  "productAttributes": [ { "ukey": "NAME", "references": { "TEXT": "Bohrmaschine" } } ],
  "skuAttributes":     [ { "ukey": "VOLTAGE", "references": { "TEXT": "230", "NUMBER": 230.0 } } ]
}
```

### 5. AdapterConfigController

```
POST /generic-adapter/configs          – neue AdapterConfig anlegen
GET  /generic-adapter/configs          – alle Configs auflisten
GET  /generic-adapter/configs/{id}     – einzelne Config laden
PUT  /generic-adapter/configs/{id}     – Config aktualisieren

POST /generic-adapter/configs/{id}/test  – Verbindung testen + 3 Beispieldatensätze zurückgeben
POST /generic-adapter/configs/{id}/run   – Adapter manuell starten (async)
GET  /generic-adapter/configs/{id}/status – letzter Run-Status (Anzahl Produkte, Fehler, Dauer)
```

**`/test`-Endpoint** ist der Wizard-Kern: gibt strukturierte Preview zurück:
```json
{
  "status": "OK",
  "detectedFields": ["voltage", "weight", "name", "sku"],
  "sampleProducts": [
    {
      "sku": "A1",
      "attributes": [
        { "ukey": "VOLTAGE", "value": "230" },
        { "ukey": "WEIGHT",  "value": "1.8" }
      ]
    }
  ]
}
```

---

## Implementierungsschritte

### Stufe 1 – Kern (Demo-ready)

- [ ] `AdapterConfig` Record + JSON-Serialisierung
- [ ] `AdapterConfigRepository` – in-memory (für Demo) oder JSON-Datei
- [ ] `GenericRestFetcher` – GET ohne Auth, kein Paging
- [ ] `JsonProductExtractor` – Fall A (flaches Array)
- [ ] `AttributeMapper` – Default-Mechanismus (Feldname → UKEY uppercase)
- [ ] `ElasticsearchAdapterWriter` – Bulk Upsert in `adapter-{id}-raw`
- [ ] `AdapterConfigController` – POST config, POST run, POST test
- [ ] Integrationstest: öffentliche Demo-API → ES

### Stufe 2 – Robustheit

- [ ] Auth-Typen (Bearer, Basic, API-Key)
- [ ] Paginierung
- [ ] `JsonProductExtractor` – Fall B (verschachtelter SKU-Array)
- [ ] `FieldMappingConfig` – manuelle UKEYs und Typ-Overrides
- [ ] Transformationen (DIVIDE, MULTIPLY, UPPERCASE)
- [ ] `AdapterConfigRepository` – persistente Speicherung (DB oder ES)
- [ ] Run-Status und Fehler-Logging

### Stufe 3 – Produktionsreife

- [ ] Scheduled Runs (Cron)
- [ ] Delta-Load (nur geänderte Datensätze)
- [ ] JDBC-Adapter (gleiche Config-Struktur, anderer Fetcher)
- [ ] CSV/Excel-Adapter
- [ ] UI-Wizard im Frontend

---

## Abgrenzung zu illusion

| Adapter (dieser Plan) | illusion (MapConfig) |
|-----------------------|----------------------|
| Normalisiert Quellstruktur → `Attribute` | Transformiert `Attribute` → Output-Felder |
| Feldname → UKEY (automatisch) | UKEY → Ziel-Template-Slot (konfiguriert) |
| Technische Übersetzung | Fachliche Transformation |
| Schreibt in `adapter-raw` ES-Index | Liest aus `adapter-raw`, schreibt in `output` ES-Index |
| Einmal konfigurieren, läuft automatisch | Redakteur pflegt Mappings |

---

## Offene Fragen

1. **Config-Persistenz Stufe 1**: JSON-Datei auf Disk vs. direkt in Elasticsearch?
   → Empfehlung: JSON-Datei in `data/adapter-configs/` für Demo einfacher.

2. **Elasticsearch-Verbindung**: Gleiche ES-Instanz wie illusion oder eigene?
   → Empfehlung: Gleiche Instanz, getrennter Index.

3. **Auth-Secrets**: Wie werden Tokens/Passwörter gespeichert?
   → Für Demo: Plaintext in Config. Produktiv: Vault / Environment Variables.
