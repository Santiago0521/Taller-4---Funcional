# Informe de paralelizacion

## Estrategia

El trabajo independiente es mover cada particula. `pasoParalelo` divide el
intervalo de indices en dos mitades y las entrega a `parallel` mientras el
tramo tenga mas elementos que el umbral. Al llegar al umbral, transforma ese
tramo con la misma funcion `pasoSegmento` que usa la version secuencial. Los
arreglos parciales se concatenan en orden. Para energia se combinan sumas; para
centro de masa se combinan pares de suma ponderada y masa, y se divide una sola
vez al final.

El umbral controla el costo de crear y coordinar tareas frente al trabajo de
cada tramo. Si es muy alto se desaprovecha paralelismo; si es muy bajo se crean
muchas tareas para poco trabajo. El umbral igual a $n$ es el control: el
programa mide la llamada a `pasoParalelo`, que en ese caso delega directamente
en `pasoSecuencial`.

## Mediciones

Se ejecuto `App.scala` en una maquina que reporto 8 procesadores disponibles,
con $dt=0.01$ y cinco mediciones calentadas por configuracion. Los tiempos se
expresan en milisegundos. La tabla reproduce una ejecucion; pueden variar en
otra corrida o maquina. La estimacion es
$n / \text{tiempoEstimado}(n,8,u)$.

| Particulas $n$ | Umbral $u$ | Secuencial (ms) | Paralelo (ms) | Aceleracion | Estimada |
|---:|---:|---:|---:|---:|---:|
| 1 000 000 | 1 024 | 27,7202 | 48,2899 | 0,5740 | 7,9373 |
| 1 000 000 | 62 500 | 27,7202 | 66,5873 | 0,4163 | 5,3332 |
| 1 000 000 | 250 000 | 27,7202 | 65,5056 | 0,4232 | 2,6667 |
| 1 000 000 | 1 000 000 (control) | 27,7202 | 22,0372 | 1,2579 | 0,8889 |
| 2 000 000 | 1 024 | 55,7135 | 121,2033 | 0,4597 | 7,9685 |
| 2 000 000 | 125 000 | 55,7135 | 119,7491 | 0,4653 | 5,3333 |
| 2 000 000 | 500 000 | 55,7135 | 118,8460 | 0,4688 | 2,6667 |
| 2 000 000 | 2 000 000 (control) | 55,7135 | 100,2021 | 0,5560 | 0,8889 |

El control tambien se midio llamando a `pasoParalelo` con umbral igual a $n$;
no se sustituyo la medicion por el tiempo secuencial. En ese caso la funcion
usa la misma ruta secuencial, asi que una diferencia medida no representa
trabajo paralelo: refleja variacion entre mediciones, calentamiento y
recoleccion de basura. El control diagnostica diferencias grandes, pero cinco
mediciones independientes no garantizan un cociente exactamente igual a 1.

## Estimacion y medicion

La estimacion conserva la tendencia ideal: profundidades menores y mas
procesadores hacen crecer la aceleracion predicha. Por ejemplo, con un millon
de particulas y umbral 1 024 predice 7,94, mientras que con el umbral 62 500
predice 5,33. En la ejecucion medida esas aceleraciones fueron 0,57 y 0,42.
Para dos millones, los umbrales repartidos dieron entre 0,46 y 0,47: en esta
maquina y corrida, repartir no compenso su costo.

El modelo solo cuenta trabajo y profundidad. No incluye el costo de crear
tareas, dividir y concatenar arreglos, asignaciones de memoria, recoleccion de
basura, competencia por memoria compartida ni otros procesos de la maquina.
Por eso sirve para razonar sobre la forma ideal del reparto, pero no predice
los milisegundos observados. La variacion grande entre umbrales cercanos
muestra que una sola ejecucion tampoco permite afirmar que la diferencia sea
estable.

## Lectura de resultados

En esta corrida, ninguno de los umbrales que realmente divide el sistema
mejoro el tiempo secuencial. El menor tiempo paralelo medido fue el control
para un millon de particulas, pero ese caso no reparte trabajo y su aceleracion
de 1,26 no es una aceleracion paralela real. Los resultados indican que, en
esta maquina, el costo de crear tareas y producir los arreglos nuevos supero
el beneficio de repartir este paso tan sencillo. No hay un umbral ganador
demostrado por estas mediciones.

La aceleracion no crece indefinidamente al agregar nucleos. Parte del tiempo
se dedica a dividir el trabajo, crear tareas, combinar arreglos y acceder a
memoria compartida; ademas, solo una cantidad finita de particulas puede
ejecutarse simultaneamente. Para los umbrales que si reparten, $S<1$, asi que
la ley de Amdahl no produce una fraccion serial fisicamente interpretable: el
modelo no incluye el sobrecosto que hizo mas lenta esta corrida. Si se usa solo
como ilustracion el mejor umbral repartido para un millon ($S=0.5740$, $p=8$),
la sustitucion da $f\approx1.85$, fuera del intervalo $[0,1]$. Esto confirma
que esta medicion no permite estimar una fraccion serial valida; se
requeririan mediciones con aceleracion mayor que uno y mas estables.

$$
f=\frac{1/S-1/p}{1-1/p}.
$$

La formula supone que el trabajo paralelo tiene costo nulo de coordinacion y
que el tiempo no paralelo se comporta como una fraccion fija del trabajo;
esas condiciones no se cumplen en esta corrida.
