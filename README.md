# Getting Started

### Absicht
Der Bosch Adapter liest die Produktdaten aus und mappt sie in das einheitliche Format. 
Das ist nötig, weil die Illusion Anwendung unabhängig von speziellen Datanstrukturen sein soll und selbst keine Anpassung nötig haben sollte.
Dies erlaubt eine hohe Flexibilität. Entsprechend muss der Adapter auch Bosch spezifische Konfigs für Illusion bereit stellen, da 
sie dort nicht hingehören.

Im Moment werden die Daten noch per Controller ausgegeben. Dies wird auf Dauer nicht praktikabel sein, da sonst die AKtualsierungen von 
Illusion jedesmal ein neues Mapping auslöst. Die Daten sollten in einem PRD Umfeld zwischengespeichert werden. Apache Kafka könnte auch ein Ansatz sein.
Die jetzige Lösung wurde Aufgrund der Einfachheit gewählt, die einen schnellen POC ermöglicht, ohne einen Speicher implementieren zu müssen.
