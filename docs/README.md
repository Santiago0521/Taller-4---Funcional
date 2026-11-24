# Informes del taller

Los informes de proceso, correccion y paralelizacion documentan la
implementacion de simulacion de particulas:

- `proceso.md` recorre las llamadas secuenciales y paralelas, incluyendo los
  tramos y la combinacion de resultados.
- `correccion.md` justifica los resultados y la asociatividad de las
  operaciones de reduccion.
- `paralelizacion.md` describe la estrategia de reparto, registra mediciones
  de `App.scala` y compara la aceleracion observada con la estimacion.

La tabla de tiempos corresponde a una maquina con ocho procesadores
disponibles. Las cifras dependen de la maquina y de la carga durante la
ejecucion.
