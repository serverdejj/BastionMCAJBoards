# BastionMCAJBoards v1.0.3

Plugin para Purpur/Paper 1.21.11 (Java 21). Cinco TOPS con hologramas nativos TextDisplay y refresco de estadísticas cada 20 ticks (1 segundo por defecto). Todos los mensajes y títulos principales se pueden configurar en YAML, con `&` y `&#RRGGBB`.

## Instalación
Sube este proyecto a GitHub, abre Actions > Compilar BastionMCAJBoards > Run workflow. Descarga el artifact JAR e instálalo en plugins. Requiere Vault + un proveedor de economía para TOP dinero; sin economía mostrará lista vacía.

## Comandos
- `/ajboards set kills` (al estar parado debajo del centro deseado; crea el holograma a 2.6 bloques sobre ti)
- `/ajboards set deaths`
- `/ajboards set hours`
- `/ajboards set clans`
- `/ajboards set money`
- `/ajboards center <tipo>` (centra el TOP en tu X/Z actual y conserva su altura)
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

## v1.0.2 - Formatos configurables
Edita `plugins/BastionMCAJBoards/config.yml` para personalizar todos los titulos, lineas,
simbolos, patrones decimales y los umbrales/sufijos K/M para dinero. Ejecuta `/ajboards reload`.
Ejemplo: `20 H`, `20 🗡️`, `20 💀`, `$1.2K`, `$2.5M`.
**No sustituyas tu config.yml actual con el ejemplo si ya has colocado los hologramas**:
conserva la seccion `boards:` con sus coordenadas. Agrega/cambia `formats:` y
`hologram:` manualmente si ya tenias config.

## v1.0.3 - Centrar TOPS
`/ajboards center <kills|deaths|hours|clans|money>`: colócate en el centro horizontal que deseas para el holograma y ejecuta el comando. Solo ajusta X y Z, no la altura Y, y guarda la ubicación en `config.yml`. Debes estar en el mismo mundo. La alineación del texto se configura con `hologram.text-alignment: CENTER` (`LEFT` / `CENTER` / `RIGHT`). También puedes ajustar `center-offset.x` y `center-offset.z` en bloques.
