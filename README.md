# BastionMCAJBoards v1.0.4 – BASTION MC

## Cambios
- `/ajboards center <kills|deaths|hours|clans|money>`: centra las letras SIN mover el holograma.
- `/ajboards fixed <tipo>`: deja de rotar hacia las cámaras; mantiene la posición y orienta el frente del texto hacia el jugador que ejecuta el comando.
- `/ajboards follow <tipo>`: restaura el comportamiento de mirar al jugador.
- Configuración individual por TOP: `boards.<tipo>.fixed`, `boards.<tipo>.facing-yaw` y `boards.<tipo>.text-alignment`.
- Migración automática de **plantillas predeterminadas originales**: `horas` → `H`, `kills` → `🗡️`, `muertes` → `💀`. Los textos personalizados se respetan. Si tenías una plantilla personalizada y quieres esos símbolos, edita `formats.<tipo>.line` en `config.yml`.
- Dinero compacto K y M, si el servidor tiene habilitada la sección `formats.money.number` (el JAR incluye valores predeterminados para config antiguos).
- TOP máximo 10 y refresco a 20 ticks.

## Instalación
1. Sube todo el contenido del ZIP a tu repositorio; reemplaza los archivos anteriores.
2. Compila con GitHub Actions, descarga el artefacto JAR y colócalo en `plugins/`.
3. **No borres `plugins/BastionMCAJBoards/config.yml`**: contiene las coordenadas.
4. Reinicia el servidor y ejecuta `/ajboards reload` si cambias el YAML.

## Nota
`fixed` fija la orientación en el momento del comando, no hace que las líneas sean invisibles desde la parte trasera. Puedes ajustar el ángulo con `boards.<tipo>.facing-yaw` (grados) y `/ajboards reload`.

Código fuente para Purpur 1.21.11 / Java 21. Se requiere verificar la compilación en GitHub Actions y probar en el servidor.
