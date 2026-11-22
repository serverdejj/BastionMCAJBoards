# BastionMCAJBoards v1.0.0

Plugin para Purpur/Paper 1.21.11 (Java 21). Cinco TOPS con hologramas nativos TextDisplay y refresco de estadísticas cada 20 ticks (1 segundo por defecto). Todos los mensajes y títulos principales se pueden configurar en YAML, con `&` y `&#RRGGBB`.

## Instalación
Sube este proyecto a GitHub, abre Actions > Compilar BastionMCAJBoards > Run workflow. Descarga el artifact JAR e instálalo en plugins. Requiere Vault + un proveedor de economía para TOP dinero; sin economía mostrará lista vacía.

## Comandos
- `/ajboards set kills` (al estar parado debajo del centro deseado; crea el holograma a 2.6 bloques sobre ti)
- `/ajboards set deaths`
- `/ajboards set hours`
- `/ajboards set clans`
- `/ajboards set money`
- `/ajboards remove <tipo>`
- `/ajboards list`, `/ajboards reload`, `/ajboards refresh`
- `/ajboards clan set <nombre> <puntos>`; `/ajboards clan remove <nombre>`

## Consideraciones
- TOP kills = jugadores asesinados, no mobs.
- TOP deaths = estadísticas de muertes de Minecraft.
- TOP hours = tiempo total de juego en horas.
- TOP money = saldo Vault (requiere proveedor); solo jugadores conocidos en este servidor.
- TOP clans = puntuaciones en `clans.yml`, **no se conecta automáticamente a BastionMCClans en esta versión**. Para enlazarlo correctamente es necesario conocer su API/datos y la métrica (kills, puntos, miembros, etc.).
- Los hologramas se recalculan cada segundo, pero si otra fuente de datos persiste valores con retraso, no puede forzar dicha fuente a actualizarse.
- Escanear miles de perfiles offline cada segundo puede causar lag: aumenta `refresh-ticks` (por ejemplo 100) si es necesario.
- Los hologramas se recrean durante los reinicios del servidor, y se eliminan al descargar el plugin.
