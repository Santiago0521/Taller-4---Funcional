# Informe de corrección
## Taller 4 — Simulación de partículas en paralelo

**Asignatura:** Fundamentos de Programación Funcional y Concurrente  
**Escuela de Ingeniería de Sistemas y Computación**  
**Universidad del Valle**

---

## 1. Introducción

El objetivo de este informe es argumentar la corrección de las funciones implementadas para el Taller 4, cuyo propósito es simular el movimiento de un sistema de partículas y realizar cálculos globales mediante estrategias secuenciales y paralelas.

La corrección se argumenta verificando que cada función produce el resultado establecido por su especificación. Para ello, se utilizan propiedades matemáticas, inducción estructural, inducción fuerte y propiedades algebraicas de las operaciones de combinación.

El análisis comprende los cinco puntos del taller:

1. Movimiento de las partículas.
2. Cálculo secuencial de magnitudes globales.
3. Cálculo paralelo de magnitudes globales.
4. Estimación del trabajo, profundidad y tiempo.
5. Cálculo de aceleración y selección del umbral conveniente.

## 2. Punto 1. Movimiento de partículas

### 2.1. Corrección de `mover`

La función `mover` recibe una partícula y un intervalo de tiempo `dt`. Su objetivo es calcular la nueva posición de la partícula sin modificar su velocidad ni su masa.

Sea una partícula:

\[
p=(pos,vel,masa)
\]

La especificación del movimiento es:

\[
pos'=pos+vel\cdot dt
\]

Por lo tanto, la función debe cumplir:

\[
mover(p,dt)=
(pos+vel\cdot dt,\ vel,\ masa)
\]

La implementación calcula primero la nueva posición:

\[
nuevaPos=p.pos+p.vel\cdot dt
\]

Después construye una nueva partícula con esa posición y conserva las propiedades restantes.

Así:

\[
mover(p,dt).pos=p.pos+p.vel\cdot dt
\]

\[
mover(p,dt).vel=p.vel
\]

\[
mover(p,dt).masa=p.masa
\]

Por lo tanto, `mover` cumple con su especificación, ya que actualiza correctamente la posición y conserva la velocidad y la masa originales.

### 2.2. Corrección de `pasoSegmento`

La función `pasoSegmento` recibe un sistema de partículas, un intervalo de tiempo y dos índices que determinan el segmento que se debe procesar.

El intervalo utilizado es semiabierto:

\[
[desde,hasta)
\]

Esto significa que incluye el índice `desde`, pero excluye el índice `hasta`.

Su especificación es:

\[
pasoSegmento(s,dt,desde,hasta)
=
[mover(s_i,dt)\mid desde\leq i<hasta]
\]

La función obtiene primero el segmento mediante `slice(desde,hasta)` y posteriormente aplica `mover` a cada elemento mediante `map`.

Como `mover` es correcta, cada partícula del segmento obtiene la posición esperada.

Además, `map` conserva la cantidad y el orden de los elementos.

Por lo tanto, `pasoSegmento` procesa exactamente las partículas del intervalo indicado, sin modificar el orden ni incluir elementos de otros segmentos.

### 2.3. Corrección de `pasoSecuencial`

La función `pasoSecuencial` debe mover todas las partículas del sistema, conservando su orden original.

Sea \(n=s.length\). Su especificación es:

\[
pasoSecuencial(s,dt)
=
[mover(s_i,dt)\mid 0\leq i<n]
\]

La implementación delega el procesamiento a:

\[
pasoSegmento(s,dt,0,n)
\]

El intervalo \([0,n)\) contiene todos los índices válidos del sistema.

Por la corrección de `pasoSegmento`, todas las partículas se procesan exactamente una vez y en el orden original.

En consecuencia:

\[
pasoSecuencial(s,dt)
=
[mover(s_0,dt),\ldots,mover(s_{n-1},dt)]
\]

Por lo tanto, `pasoSecuencial` cumple con su especificación.

### 2.4. Corrección de `pasoParalelo`

La función `pasoParalelo` divide recursivamente el sistema en dos segmentos cuando su tamaño supera el umbral. Cuando el segmento es suficientemente pequeño, se procesa secuencialmente.

Se desea demostrar:

\[
pasoParalelo(s,dt,u)=pasoSecuencial(s,dt)
\]

donde \(u\) representa el umbral.

**Caso base**

Si:

\[
s.length\leq u
\]

o:

\[
s.length\leq 1
\]

la función ejecuta directamente `pasoSecuencial`.

Por lo tanto, la igualdad se cumple en este caso.

**Hipótesis de inducción**

Suponemos que para cualquier sistema con menos de \(n\) partículas:

\[
pasoParalelo(s',dt,u)=pasoSecuencial(s',dt)
\]

**Caso inductivo**

Consideremos un sistema con \(n\) partículas que no cumple la condición base.

La función calcula:

\[
mitad=\left\lfloor\frac{n}{2}\right\rfloor
\]

Y obtiene los segmentos:

\[
s_1=s.slice(0,mitad)
\]

\[
s_2=s.slice(mitad,n)
\]

Ambos segmentos tienen menos de \(n\) partículas. Por la hipótesis de inducción:

\[
pasoParalelo(s_1,dt,u)=pasoSecuencial(s_1,dt)
\]

\[
pasoParalelo(s_2,dt,u)=pasoSecuencial(s_2,dt)
\]

La función ejecuta ambos cálculos mediante `parallel` y después combina los resultados con la concatenación:

\[
izquierda++derecha
\]

Como se demostrará en la siguiente sección, los segmentos cubren todo el sistema sin superponerse y la concatenación conserva el orden.

Por lo tanto:

\[
pasoParalelo(s,dt,u)=pasoSecuencial(s,dt)
\]

**Terminación**

Cada llamada recursiva procesa segmentos de menor tamaño que el sistema original.

La división continúa hasta alcanzar la condición base, donde el procesamiento se realiza secuencialmente.

Así, la recursión termina y se concluye que `pasoParalelo` produce el mismo resultado que `pasoSecuencial`.

### 2.5. Demostración de que dividir el sistema no pierde ni repite partículas

Sea un sistema con \(n\) partículas, cuyos índices son:

\[
\{0,1,\ldots,n-1\}
\]

La división se realiza mediante:

\[
mitad=\left\lfloor\frac{n}{2}\right\rfloor
\]

Se obtienen los segmentos:

\[
S_1=[0,mitad)
\]

\[
S_2=[mitad,n)
\]

Para demostrar que la división es correcta se verifican tres propiedades.

**Los segmentos no se superponen**

\[
S_1\cap S_2=\varnothing
\]

El primer segmento excluye el índice `mitad`, mientras que el segundo comienza en ese índice. Por lo tanto, ningún elemento pertenece a ambos segmentos.

**Los segmentos cubren todo el sistema**

\[
S_1\cup S_2=[0,n)
\]

Todos los índices válidos pertenecen a alguno de los dos segmentos.

**Se conserva la cantidad de elementos**

\[
|S_1|=mitad
\]

\[
|S_2|=n-mitad
\]

Por lo tanto:

\[
|S_1|+|S_2|=n
\]

La cantidad total de partículas procesadas coincide con la cantidad original.

Estas propiedades se conservan en las divisiones recursivas porque cada segmento se divide utilizando el mismo procedimiento.

Concluimos que la división del sistema no pierde ni repite partículas.

### 2.6. Asociatividad de la concatenación

Los resultados parciales se combinan utilizando la operación `++`.

Para tres arreglos \(A\), \(B\) y \(C\):

\[
(A++B)++C=A++(B++C)
\]

Ambas expresiones producen los elementos de \(A\), seguidos por los de \(B\) y finalmente los de \(C\).

Por lo tanto, la concatenación es asociativa.

Además, el arreglo vacío es su elemento neutro:

\[
A++[]=A
\]

\[
[]++A=A
\]

La concatenación conserva el orden de los elementos de cada segmento.

Esta propiedad garantiza que los resultados parciales pueden combinarse sin alterar el orden original de las partículas.

---

## 3. Punto 2. Cálculo secuencial de magnitudes globales

En este punto se calculan tres magnitudes del sistema: la energía cinética total, la masa total y el centro de masa.

### 3.1. Corrección de `energiaSecuencial`

La energía cinética de una partícula se define como:

\[
E_i=\frac{1}{2}m_i\|v_i\|^2
\]

Como la velocidad es un vector bidimensional:

\[
v_i=(v_{ix},v_{iy})
\]

su norma al cuadrado es:

\[
\|v_i\|^2=v_{ix}^2+v_{iy}^2
\]

Por lo tanto:

\[
E_i=\frac{1}{2}m_i(v_{ix}^2+v_{iy}^2)
\]

La energía total del sistema es:

\[
E_{total}=\sum_{i=0}^{n-1}E_i
\]

Es decir:

\[
E_{total}
=
\sum_{i=0}^{n-1}
\frac{1}{2}m_i\|v_i\|^2
\]

La función secuencial debe recorrer todas las partículas y sumar su energía cinética.

Si el acumulador inicia en cero y en cada paso agrega la energía de la partícula actual, después de recorrer las \(n\) partículas contiene:

\[
E_{acumulada}=E_0+E_1+\cdots+E_{n-1}
\]

Por lo tanto:

\[
energiaSecuencial(s)=E_{total}
\]

Para un sistema vacío, la suma es cero, lo que coincide con la especificación.

Concluimos que la función calcula correctamente la energía cinética total.

### 3.2. Corrección de `masaTotal`

La masa total corresponde a la suma de las masas de todas las partículas:

\[
M=\sum_{i=0}^{n-1}m_i
\]

La función comienza con un acumulador igual a cero y agrega la masa de cada partícula.

Después de recorrer el sistema, el acumulador contiene:

\[
M=m_0+m_1+\cdots+m_{n-1}
\]

Como cada partícula se procesa una vez, ninguna masa se pierde ni se cuenta dos veces.

Para el sistema vacío:

\[
M=0
\]

Por lo tanto:

\[
masaTotal(s)=\sum_{i=0}^{n-1}s_i.masa
\]

Concluimos que `masaTotal` cumple con su especificación.

### 3.3. Corrección de `centroDeMasaSecuencial`

El centro de masa representa la posición promedio del sistema, ponderada por la masa de cada partícula.

Su definición es:

\[
CM=
\frac{\sum_{i=0}^{n-1}m_i p_i}
{\sum_{i=0}^{n-1}m_i}
\]

La suma ponderada de posiciones es:

\[
P=\sum_{i=0}^{n-1}m_i p_i
\]

Y la masa total es:

\[
M=\sum_{i=0}^{n-1}m_i
\]

Por lo tanto:

\[
CM=\frac{P}{M}
\]

La función debe acumular la suma ponderada de las posiciones y la masa total.

Al finalizar el recorrido, ambos acumuladores contienen las contribuciones de todas las partículas.

Si la masa total es distinta de cero, se obtiene el centro de masa dividiendo la suma ponderada entre la masa total.

Si la masa total es cero, la especificación del taller establece que el centro de masa debe ser el origen:

\[
CM=(0,0)
\]

Por lo tanto, la función cumple con su especificación tanto para sistemas con masa como para sistemas sin masa.

---

## 4. Punto 3. Cálculo paralelo de magnitudes globales

En este punto se distribuyen los cálculos entre segmentos y se combinan los resultados parciales.

La corrección se demuestra comprobando que la combinación produce el mismo resultado que el cálculo secuencial.

### 4.1. Corrección de `energiaParalela`

La energía total se obtiene sumando la energía individual de cada partícula:

\[
E_{total}=\sum_{i=0}^{n-1}E_i
\]

Al dividir el sistema en dos segmentos disjuntos:

\[
S_1=[0,mitad)
\]

\[
S_2=[mitad,n)
\]

Se calcula:

\[
E_1=\sum_{i\in S_1}E_i
\]

\[
E_2=\sum_{i\in S_2}E_i
\]

Como los segmentos cubren todo el sistema y no se superponen:

\[
E_{total}=E_1+E_2
\]

La suma es asociativa:

\[
(a+b)+c=a+(b+c)
\]

Y tiene como elemento neutro el cero:

\[
a+0=0+a=a
\]

Por lo tanto, los resultados parciales pueden combinarse en cualquier agrupación sin modificar el resultado.

Concluimos que:

\[
energiaParalela(s,u)=energiaSecuencial(s)
\]

La ejecución paralela produce la misma energía total que la ejecución secuencial.

### 4.2. Corrección de `centroDeMasaParalelo`

El centro de masa no se puede calcular promediando directamente los centros de masa de dos segmentos, porque las masas de ambos segmentos pueden ser diferentes.

Para combinar correctamente los resultados, cada segmento debe aportar dos valores:

- Su masa total.
- La suma de sus posiciones ponderadas por la masa.

Representamos cada resultado parcial como:

\[
(M,P)
\]

Donde:

\[
M=\sum m_i
\]

\[
P=\sum m_i p_i
\]

La operación de combinación se define como:

\[
(M_1,P_1)\oplus(M_2,P_2)
=
(M_1+M_2,P_1+P_2)
\]

**Demostración de asociatividad**

Sean tres segmentos con resultados:

\[
(M_1,P_1),\ (M_2,P_2),\ (M_3,P_3)
\]

Al agrupar primero los dos primeros:

\[
((M_1,P_1)\oplus(M_2,P_2))\oplus(M_3,P_3)
\]

Se obtiene:

\[
(M_1+M_2+M_3,\ P_1+P_2+P_3)
\]

Al agrupar primero los dos últimos:

\[
(M_1,P_1)\oplus((M_2,P_2)\oplus(M_3,P_3))
\]

Se obtiene exactamente el mismo resultado:

\[
(M_1+M_2+M_3,\ P_1+P_2+P_3)
\]

Por lo tanto:

\[
(a\oplus b)\oplus c=a\oplus(b\oplus c)
\]

La operación es asociativa porque la suma de masas y la suma de posiciones ponderadas son asociativas.

**Obtención del centro de masa**

Una vez combinados todos los segmentos, se obtiene:

\[
M_{total}=\sum M_i
\]

\[
P_{total}=\sum P_i
\]

Si la masa total es distinta de cero:

\[
CM=\frac{P_{total}}{M_{total}}
\]

Si la masa total es cero, se devuelve el origen:

\[
CM=(0,0)
\]

Así se demuestra que la combinación paralela produce el mismo centro de masa que el cálculo secuencial.

---

## 5. Punto 4. Trabajo, profundidad y tiempo estimado

Este punto analiza el costo teórico de la simulación paralela mediante tres funciones: `trabajo`, `profundidad` y `tiempoEstimado`.

### 5.1. Corrección de `trabajo`

La función `trabajo` recibe la cantidad de partículas \(n\).

Según la especificación, existe una operación por cada partícula del sistema.

Por lo tanto:

\[
W(n)=n
\]

La función exige que:

\[
n\geq 0
\]

Y devuelve directamente \(n\).

Para un sistema vacío:

\[
W(0)=0
\]

Para un sistema con una partícula:

\[
W(1)=1
\]

En general, para cualquier cantidad válida de partículas, el trabajo es igual a su cantidad.

Por lo tanto, `trabajo` cumple con su especificación.

### 5.2. Corrección de `profundidad`

La función `profundidad` recibe el tamaño del sistema \(n\) y el umbral \(u\).

Su objetivo es calcular la profundidad de la división recursiva, incluyendo el trabajo del segmento que finalmente se procesa sin dividir.

La función auxiliar utiliza la siguiente definición:

\[
D(n,u)=
\begin{cases}
n, & n\leq u\\
1+\max(D(\lfloor n/2\rfloor,u),D(n-\lfloor n/2\rfloor,u)), & n>u
\end{cases}
\]

**Caso base**

Cuando:

\[
n\leq u
\]

la función devuelve \(n\).

Esto representa el tamaño del segmento que ya no se divide y se procesa secuencialmente.

**Caso recursivo**

Cuando:

\[
n>u
\]

se divide el sistema en dos partes:

\[
n_1=\left\lfloor\frac{n}{2}\right\rfloor
\]

\[
n_2=n-n_1
\]

La profundidad se calcula como:

\[
D(n,u)=1+\max(D(n_1,u),D(n_2,u))
\]

El uno representa la división actual. Se utiliza el máximo porque la profundidad corresponde al camino más largo de dependencias entre las dos ramas.

**Ejemplo**

Para:

\[
n=1024,\quad u=512
\]

Se obtiene:

\[
D(1024,512)=1+\max(D(512,512),D(512,512))
\]

Como los dos segmentos alcanzan el umbral:

\[
D(512,512)=512
\]

Entonces:

\[
D(1024,512)=1+512=513
\]

Esto coincide con el comportamiento esperado de la función.

**Terminación**

En cada llamada recursiva, el tamaño de los segmentos es menor que el tamaño original.

Por lo tanto, después de suficientes divisiones se alcanza la condición:

\[
n\leq u
\]

En ese momento se devuelve el tamaño del segmento y termina la recursión.

Concluimos que `profundidad` calcula correctamente la cantidad de divisiones y el trabajo restante del tramo final.

### 5.3. Corrección de `tiempoEstimado`

La función `tiempoEstimado` recibe:

- \(n\): cantidad de partículas.
- \(p\): cantidad de procesadores.
- \(u\): umbral.

Su especificación es:

\[
T(n,p,u)=D(n,u)+\frac{W(n)}{p}
\]

Como:

\[
W(n)=n
\]

Se obtiene:

\[
T(n,p,u)=D(n,u)+\frac{n}{p}
\]

La función exige:

\[
p\geq1
\]

Esto evita dividir entre cero o utilizar una cantidad inválida de procesadores.

Por la corrección de `trabajo` y `profundidad`, ambos términos de la expresión corresponden a sus respectivas especificaciones.

Por lo tanto, `tiempoEstimado` calcula correctamente el tiempo teórico definido para el taller.

Es importante aclarar que este valor es una estimación matemática y no una medición real del tiempo de ejecución.

---

## 6. Punto 5. Aceleración y selección del umbral conveniente

Este punto permite comparar el rendimiento secuencial y paralelo mediante mediciones y determinar qué umbral ofrece el mejor tiempo entre los candidatos.

### 6.1. Corrección de `aceleracion`

La aceleración se define como:

\[
A=\frac{T_s}{T_p}
\]

Donde:

- \(T_s\) es el tiempo de ejecución secuencial.
- \(T_p\) es el tiempo de ejecución paralelo.

Si:

\[
A>1
\]

la ejecución paralela es más rápida.

Si:

\[
A=1
\]

ambas ejecuciones tardan lo mismo.

Si:

\[
A<1
\]

la ejecución paralela es más lenta.

La función devuelve directamente el cociente entre ambos tiempos.

Por lo tanto, cumple con la definición de aceleración, siempre que el tiempo paralelo sea distinto de cero.

### 6.2. Corrección de la medición

La función `medir` realiza varias mediciones de una operación utilizando ScalaMeter.

Se obtienen varias muestras y se ordenan de menor a mayor.

Después se selecciona el elemento central:

\[
T_{medido}=muestras[\lfloor r/2\rfloor]
\]

donde \(r\) es la cantidad de repeticiones.

En el taller se utilizan cinco repeticiones:

\[
r=5
\]

Por lo tanto, se selecciona la muestra central de las cinco mediciones ordenadas, que corresponde a la mediana.

Además, se utiliza un calentamiento de la máquina virtual antes de cada medición, reduciendo el efecto de la primera ejecución.

El uso de la mediana permite disminuir la influencia de mediciones excepcionalmente altas o bajas.

### 6.3. Corrección de `umbralConveniente`

La función `umbralConveniente` recibe un sistema, un intervalo de tiempo y una lista de umbrales candidatos.

Su objetivo es seleccionar el umbral que produce el menor tiempo de ejecución medido.

**Caso base**

Si la lista de candidatos está vacía:

\[
umbrales=[]
\]

La función devuelve:

\[
s.length
\]

Este valor equivale a no dividir el sistema, porque el tamaño del sistema es menor o igual al umbral.

**Caso recursivo o general**

Cuando existen candidatos, la función mide la ejecución paralela para cada umbral utilizando cinco repeticiones.

Para cada candidato \(u_i\), obtiene:

\[
T_i=medir(5)\{pasoParalelo(s,dt,u_i)\}
\]

Después selecciona el candidato cuyo tiempo medido es menor:

\[
u_{mejor}=\arg\min_{u_i}T_i
\]

Por lo tanto, la función devuelve uno de los umbrales proporcionados y selecciona el que presenta el menor tiempo mediano.

Concluimos que `umbralConveniente` cumple con su especificación.

### 6.4. Interpretación de la aceleración y del umbral

El rendimiento de la ejecución paralela depende del tamaño del sistema y del umbral utilizado.

Un umbral demasiado pequeño puede producir muchas tareas y aumentar los costos de coordinación.

Un umbral demasiado grande puede reducir el paralelismo disponible.

Por lo tanto, el mejor umbral no necesariamente es el menor ni el mayor, sino aquel que presenta el menor tiempo medido para el sistema y el equipo utilizados.

Cuando:

\[
u=n
\]

la condición de la función paralela se cumple desde el comienzo y el sistema se procesa secuencialmente.

Este caso sirve como control para comparar la ejecución paralela sin división con la ejecución secuencial.

La aceleración y los tiempos obtenidos dependen de las características de la máquina y de las condiciones de ejecución. Por eso deben analizarse con las mediciones reales del experimento.

---

## 7. Conclusiones

A partir de las demostraciones realizadas, se establece que la corrección de la simulación depende de que cada función respete su especificación y de que las operaciones utilizadas para combinar resultados parciales conserven las propiedades necesarias.

En el Punto 1 se demuestra que el movimiento de cada partícula se calcula correctamente y que la división del sistema en segmentos no pierde ni repite partículas. Además, la concatenación de los resultados es asociativa y conserva el orden.

En el Punto 2 se justifica que los cálculos secuenciales de energía, masa total y centro de masa corresponden a sus definiciones matemáticas.

En el Punto 3 se demuestra que los cálculos paralelos pueden combinarse correctamente. La energía se combina mediante suma y el centro de masa mediante la suma de masas y posiciones ponderadas.

En el Punto 4 se demuestra que el trabajo corresponde a la cantidad de partículas, que la profundidad se calcula a partir de la división recursiva y que el tiempo estimado combina la profundidad con el trabajo distribuido entre los procesadores.

Finalmente, en el Punto 5 se justifica el cálculo de aceleración y la selección del umbral mediante mediciones repetidas.

En conclusión, el uso de paralelismo permite distribuir el trabajo sin cambiar los resultados matemáticos de la simulación, siempre que la división de los datos y la combinación de los resultados parciales respeten las propiedades demostradas.