# Informe de correccion

Sea $S=(p_0,\ldots,p_{n-1})$ un sistema de particulas, con
$p_i=(x_i,v_i,m_i)$, y sea $[a,b)$ un tramo de indices.

## Paso de la simulacion

Para una particula, `mover` calcula

$$
x_i'=x_i+dt\,v_i,\qquad v_i'=v_i,\qquad m_i'=m_i.
$$

Por tanto, conserva velocidad y masa y aplica exactamente el desplazamiento
solicitado. `pasoSegmento(S,dt,a,b)` transforma solo los indices de $[a,b)$ y
mantiene su orden. Si $a=b$, el resultado es vacio.

En cada division, $m=a+\lfloor(b-a)/2\rfloor$. Los tramos hijos son
$[a,m)$ y $[m,b)$. Son disjuntos porque el primero termina donde empieza el
segundo, y su union es exactamente $[a,b)$. Por induccion sobre las divisiones,
ningun indice se pierde ni se repite. La concatenacion izquierda-derecha
conserva el orden original. Cuando un tramo alcanza el umbral (o tiene a lo
sumo un elemento), `pasoSegmento` realiza su calculo; de ahi se obtiene que
`pasoParalelo` produce el mismo arreglo que `pasoSecuencial`.

## Magnitudes secuenciales

La energia cinetica queda definida por

$$
E(S)=\sum_{i=0}^{n-1}\frac{1}{2}m_i\lVert v_i\rVert^2.
$$

`energiaSecuencial` suma exactamente un termino por particula. Para el arreglo
vacio la suma vale cero. `masaTotal` calcula
$M(S)=\sum_i m_i$, tambien cero para el arreglo vacio. La suma ponderada de
posiciones es $P(S)=\sum_i m_i x_i$. Si $M(S)\ne0$, el centro es
$C(S)=P(S)/M(S)$; si $M(S)=0$, la implementacion retorna el origen, como pide
el enunciado.

## Magnitudes paralelas y asociatividad

En energia, el resultado parcial de un tramo es la suma de sus terminos. Si
$S$ se divide en $L$ y $R$, entonces $E(S)=E(L)+E(R)$. La suma es asociativa
en los numeros reales: $(a+b)+c=a+(b+c)$. Por eso el arbol de reduccion puede
combinar los subtotales sin depender del orden de ejecucion.

El centro de masa no se combina promediando los centros parciales. La
informacion reducida de un tramo es el par $(P,M)$: suma ponderada y masa.
Definimos

$$
(P_1,M_1)\oplus(P_2,M_2)=(P_1+P_2,M_1+M_2).
$$

La operacion es asociativa porque la suma de vectores y la suma de masas lo
son:

$$
((P_1,M_1)\oplus(P_2,M_2))\oplus(P_3,M_3)
=(P_1+P_2+P_3,M_1+M_2+M_3)
=(P_1,M_1)\oplus((P_2,M_2)\oplus(P_3,M_3)).
$$

Ademas, si $S=L\mathbin{+\!\!+}R$, entonces
$(P(S),M(S))=(P(L),M(L))\oplus(P(R),M(R))$. La division conserva asi toda la
informacion necesaria. La division $P/M$ se hace una sola vez, despues de
combinar todos los parciales; en el caso $M=0$ se retorna el origen. Las
pruebas usan tolerancia porque la suma de `Double` no es exactamente asociativa
en la aritmetica de punto flotante.

## Trabajo, profundidad y aceleracion

Por definicion, `trabajo(n)=n`. La profundidad cumple la recurrencia

$$
D(n,u)=
\begin{cases}
n,& n\le u,\\
1+\max(D(\lfloor n/2\rfloor,u),D(\lceil n/2\rceil,u)),&n>u.
\end{cases}
$$

El maximo corresponde a que el tiempo critico espera a la mitad mas profunda;
el 1 cuenta la particion. Por ello `tiempoEstimado` suma $D(n,u)$ y $n/p$.
Finalmente, `aceleracion` implementa $t_s/t_p$, y `umbralConveniente` mide
candidatos y escoge el de menor tiempo, o $n$ cuando la lista esta vacia.
