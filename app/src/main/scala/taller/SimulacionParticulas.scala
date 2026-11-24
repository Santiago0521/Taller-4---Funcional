package taller

import common._
import org.scalameter._

/**
 * Taller 4 — simulación de partículas repartida entre varios núcleos.
 *
 * Complete cada función. Las firmas y los tipos no se modifican.
 *
 * Las abstracciones `parallel` y `task` vienen en el paquete `common`, que ya
 * está en el proyecto.
 */
class SimulacionParticulas {

  type Sistema = Array[Particula]

  // Punto 1: un paso de la simulación -----------------------------------------

  /** La posición nueva es la anterior más la velocidad por dt. */
  def mover(p: Particula, dt: Double): Particula = {
    val nuevaPos = p.pos + (p.vel * dt)
    Particula(nuevaPos, p.vel, p.masa)
  }

  /** Avanza todo el sistema un paso de tiempo dt. */
  def pasoSecuencial(s: Sistema, dt: Double): Sistema = {
    pasoSegmento(s, dt, 0, s.length)
  }

  /** Avanza solo el tramo entre `desde` incluido y `hasta` excluido. */
  def pasoSegmento(s: Sistema, dt: Double, desde: Int, hasta: Int): Sistema = {
    s.slice(desde, hasta).map(p => mover(p, dt))
  }

  /**
   * Parte el sistema por la mitad con `parallel` mientras el tramo tenga más
   * elementos que el umbral, y por debajo de esa cifra resuelve en secuencial.
   */
  def pasoParalelo(s: Sistema, dt: Double, umbral: Int): Sistema = {
    def avanzar(desde: Int, hasta: Int): Sistema = {
      val tamaño = hasta - desde
      if (tamaño <= umbral || tamaño <= 1) {
        pasoSegmento(s, dt, desde, hasta)
      } else {
        val mitad = desde + tamaño / 2
        val (izquierda, derecha) = parallel(
          avanzar(desde, mitad),
          avanzar(mitad, hasta)
        )
        izquierda ++ derecha
      }
    }
    avanzar(0, s.length)
  }

  // Punto 2: magnitudes globales ----------------------------------------------

  /** La suma de las energías cinéticas, media masa por velocidad al cuadrado. */
  def energiaSecuencial(s: Sistema): Double = {
    s.foldLeft(0.0) { (total, particula) =>
      total + 0.5 * particula.masa * particula.vel.norma * particula.vel.norma
    }
  }

  /** La suma de las masas. */
  def masaTotal(s: Sistema): Double = {
    s.foldLeft(0.0) { (total, particula) => total + particula.masa }
  }

  /**
   * La suma de las posiciones ponderadas por la masa, dividida entre la masa
   * total. Sin masa, el origen.
   */
  def centroDeMasaSecuencial(s: Sistema): Vector2 = {
    val (posicionPonderada, masa) = s.foldLeft((Vector2(0.0, 0.0), 0.0)) {
      case ((suma, masaAcumulada), particula) =>
        (suma + particula.pos * particula.masa, masaAcumulada + particula.masa)
    }
    if (masa == 0.0) Vector2(0.0, 0.0)
    else posicionPonderada * (1.0 / masa)
  }

  // Punto 3: las mismas magnitudes en paralelo --------------------------------

  /** La energía del sistema, repartida con el mismo umbral del punto 1. */
  def energiaParalela(s: Sistema, umbral: Int): Double = {
    def acumular(desde: Int, hasta: Int): Double = {
      val tamaño = hasta - desde
      if (tamaño <= umbral || tamaño <= 1) {
        s.slice(desde, hasta).foldLeft(0.0) { (total, particula) =>
          total + 0.5 * particula.masa * particula.vel.norma * particula.vel.norma
        }
      } else {
        val mitad = desde + tamaño / 2
        val (izquierda, derecha) = parallel(
          acumular(desde, mitad),
          acumular(mitad, hasta)
        )
        izquierda + derecha
      }
    }
    acumular(0, s.length)
  }

  /**
   * El centro de masa, repartido. Promediar los promedios de cada mitad da
   * otra cosa cuando las dos mitades no pesan igual.
   */
  def centroDeMasaParalelo(s: Sistema, umbral: Int): Vector2 = {
    def acumular(desde: Int, hasta: Int): (Vector2, Double) = {
      val tamaño = hasta - desde
      if (tamaño <= umbral || tamaño <= 1) {
        s.slice(desde, hasta).foldLeft((Vector2(0.0, 0.0), 0.0)) {
          case ((suma, masaAcumulada), particula) =>
            (suma + particula.pos * particula.masa, masaAcumulada + particula.masa)
        }
      } else {
        val mitad = desde + tamaño / 2
        val (izquierda, derecha) = parallel(
          acumular(desde, mitad),
          acumular(mitad, hasta)
        )
        (izquierda._1 + derecha._1, izquierda._2 + derecha._2)
      }
    }

    val (posicionPonderada, masa) = acumular(0, s.length)
    if (masa == 0.0) Vector2(0.0, 0.0)
    else posicionPonderada * (1.0 / masa)
  }

  // Punto 4: trabajo y profundidad --------------------------------------------

  /** El número total de operaciones, una por partícula. */
  def trabajo(n: Int): Int = {
    require(n >= 0, s"n debe ser positivo")
    n
  }

  /** Cuántas veces se parte a la mitad, más el tramo que queda sin partir. */
  def profundidad(n: Int, umbral: Int): Int = {
    require(n >= 0, "n debe ser positivo")
    require(umbral >= 1, "umbral debe ser al menos 1")

    def calculo(tamaño: Int): Int = {
      if (tamaño <= umbral) {
        tamaño
      } else {
        val mitad = tamaño / 2
        val otraMitad = tamaño - mitad
        1 + math.max(calculo(mitad), calculo(otraMitad))
      }
    }
    calculo(n)
  }

  /** La profundidad más el trabajo entre p procesadores. */
  def tiempoEstimado(n: Int, p: Int, umbral: Int): Double = {
    require(p >= 1, "Debe existir al menos un procesador")

    profundidad(n, umbral).toDouble + trabajo(n).toDouble / p
  }

  // Punto 5: medir la aceleración ---------------------------------------------

  /** Mediana de varias mediciones de ScalaMeter, en milisegundos. */
  def medir[A](repeticiones: Int)(cuerpo: => A): Double = {
    val muestras = Vector.fill(repeticiones) {
      (withWarmer(new Warmer.Default) measure { cuerpo }).value
    }
    muestras.sorted.apply(muestras.length / 2)
  }

  /** El cociente entre el tiempo secuencial y el paralelo. */
  def aceleracion(tSecuencial: Double, tParalelo: Double): Double = {
    tSecuencial / tParalelo
  }

  /**
   * El umbral que menos tarda de una lista de candidatos. Con la lista vacía,
   * el tamaño del sistema, que equivale a no repartir.
   */
  def umbralConveniente(s: Sistema, dt: Double, umbrales: List[Int]): Int = {
    if (umbrales.isEmpty) {
      s.length
    } else {
      umbrales.map(umbral => (umbral, medir(5) {pasoParalelo(s, dt, umbral)})).minBy(_._2)._1
    }
  }
}
