# demo-recruitment-app

Reclutamiento del futuro. Estupendo, simplemente estupendo.

[Verlo en vivo](https://fir-recruitment-app.firebaseapp.com)

## Requisitos

- [Java JDK 6 o mas actual](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
- [Leiningen](https://leiningen.org/#install)

## Inicio

Desde la carpeta base del proyecto, corre este comando para iniciar un servidor de desarrollo:

    lein figwheel

Para producir una versión que se puede utilizar en producción:

    lein do clean, cljsbuild once min

Mas información en [el repositorio de Figwheel](https://github.com/bhauman/lein-figwheel).
