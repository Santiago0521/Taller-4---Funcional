package taller

import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CasosAdicionalesTest extends AnyFunSuite {

  val sim = new SimulacionParticulas()
  import sim._

  val tolerancia = 1e-8

  test("punto 1: mover admite un intervalo de tiempo negativo") {
    val particula = Particula(Vector2(8.0, -3.0), Vector2(2.0, 4.0), 6.0)
    val resultado = mover(particula, -2.0)

    assert(resultado.pos == Vector2(4.0, -11.0))
    assert(resultado.vel == particula.vel)
    assert(resultado.masa == particula.masa)
  }

  test("punto 1: segmento interior respeta limites y orden") {
    val sistema = Array.tabulate(7) { i =>
      Particula(Vector2(i.toDouble, 0.0), Vector2(0.0, 1.0), i.toDouble + 1.0)
    }

    val resultado = pasoSegmento(sistema, 3.0, 2, 6)

    assert(resultado.length == 4)
    assert(resultado.map(_.pos.x).sameElements(Array(2.0, 3.0, 4.0, 5.0)))
    assert(resultado.forall(_.pos.y == 3.0))
  }

  test("punto 1: el reparto impar conserva el resultado y el orden") {
    val sistema = Array.tabulate(11) { i =>
      Particula(Vector2(i.toDouble, -i.toDouble), Vector2(0.5, 2.0), 1.0)
    }

    val secuencial = pasoSecuencial(sistema, 1.5)
    val paralelo = pasoParalelo(sistema, 1.5, 2)

    assert(paralelo.sameElements(secuencial))
  }

  test("punto 1: el reparto con umbral cero maneja mas de una particula") {
    val sistema = Array(
      Particula(Vector2(1.0, 2.0), Vector2(3.0, 4.0), 2.0),
      Particula(Vector2(-1.0, 5.0), Vector2(0.0, -2.0), 3.0),
      Particula(Vector2(7.0, 0.0), Vector2(-1.0, 1.0), 4.0))

    assert(pasoParalelo(sistema, 0.25, 0).sameElements(pasoSecuencial(sistema, 0.25)))
  }

  test("punto 1: avanzar no altera las particulas de entrada") {
    val sistema = Array(
      Particula(Vector2(2.0, 7.0), Vector2(1.0, -1.0), 5.0),
      Particula(Vector2(4.0, 8.0), Vector2(-2.0, 3.0), 6.0))
    val copia = sistema.clone()

    pasoParalelo(sistema, 4.0, 1)

    assert(sistema.sameElements(copia))
  }

  test("punto 2: energia suma aportes de velocidades en ambas coordenadas") {
    val sistema = Array(
      Particula(Vector2(0.0, 0.0), Vector2(3.0, 4.0), 2.0),
      Particula(Vector2(1.0, 1.0), Vector2(0.0, 2.0), 3.0))

    assert(math.abs(energiaSecuencial(sistema) - 31.0) < tolerancia)
  }

  test("punto 2: masa total incluye todas las particulas") {
    val sistema = Array(
      Particula(Vector2(0.0, 0.0), Vector2(0.0, 0.0), 1.25),
      Particula(Vector2(0.0, 0.0), Vector2(0.0, 0.0), 2.75),
      Particula(Vector2(0.0, 0.0), Vector2(0.0, 0.0), 0.5))

    assert(math.abs(masaTotal(sistema) - 4.5) < tolerancia)
  }

  test("punto 2: centro de masa pondera coordenadas por masa") {
    val sistema = Array(
      Particula(Vector2(2.0, 0.0), Vector2(0.0, 0.0), 4.0),
      Particula(Vector2(-1.0, 3.0), Vector2(0.0, 0.0), 1.0))
    val centro = centroDeMasaSecuencial(sistema)

    assert(math.abs(centro.x - 1.4) < tolerancia)
    assert(math.abs(centro.y - 0.6) < tolerancia)
  }

  test("punto 2: particulas sin masa no desplazan el centro") {
    val sistema = Array(
      Particula(Vector2(100.0, -20.0), Vector2(0.0, 0.0), 0.0),
      Particula(Vector2(-8.0, 90.0), Vector2(1.0, 1.0), 0.0))

    assert(centroDeMasaSecuencial(sistema) == Vector2(0.0, 0.0))
  }

  test("punto 2: masa y energia son cero en un sistema vacio") {
    assert(masaTotal(Array.empty[Particula]) == 0.0)
    assert(energiaSecuencial(Array.empty[Particula]) == 0.0)
  }

  test("punto 3: energia paralela combina tramos de tamano impar") {
    val sistema = Array.tabulate(13) { i =>
      Particula(
        Vector2(i.toDouble, 0.0),
        Vector2(i.toDouble / 3.0, (i % 4).toDouble),
        i.toDouble + 0.5
      )
    }
    val esperada = energiaSecuencial(sistema)

    assert(math.abs(energiaParalela(sistema, 3) - esperada) < tolerancia)
  }

  test("punto 3: energia paralela coincide al evitar particiones") {
    val sistema = Array(
      Particula(Vector2(2.0, 1.0), Vector2(1.0, 7.0), 3.0),
      Particula(Vector2(5.0, 4.0), Vector2(-2.0, 2.0), 0.25))

    assert(math.abs(energiaParalela(sistema, 20) - energiaSecuencial(sistema)) < tolerancia)
  }

  test("punto 3: centro de masa paralelo combina masas de mitades desiguales") {
    val sistema = Array(
      Particula(Vector2(2.0, 4.0), Vector2(0.0, 0.0), 20.0),
      Particula(Vector2(10.0, -2.0), Vector2(0.0, 0.0), 1.0),
      Particula(Vector2(30.0, 8.0), Vector2(0.0, 0.0), 1.0))
    val esperado = centroDeMasaSecuencial(sistema)
    val obtenido = centroDeMasaParalelo(sistema, 1)

    assert(math.abs(obtenido.x - esperado.x) < tolerancia)
    assert(math.abs(obtenido.y - esperado.y) < tolerancia)
  }

  test("punto 3: centro de masa paralelo retorna origen si la masa es cero") {
    val sistema = Array(
      Particula(Vector2(8.0, 2.0), Vector2(1.0, 0.0), 0.0),
      Particula(Vector2(-3.0, 7.0), Vector2(0.0, 1.0), 0.0))

    assert(centroDeMasaParalelo(sistema, 1) == Vector2(0.0, 0.0))
  }

  test("punto 3: ambas magnitudes paralelas aceptan una lista vacia") {
    val sistema = Array.empty[Particula]

    assert(energiaParalela(sistema, 1) == 0.0)
    assert(centroDeMasaParalelo(sistema, 1) == Vector2(0.0, 0.0))
  }

  test("punto 3: centro de masa paralelo tambien funciona sin dividir") {
    val sistema = Array(
      Particula(Vector2(-4.0, 6.0), Vector2(0.0, 0.0), 2.0),
      Particula(Vector2(8.0, 10.0), Vector2(0.0, 0.0), 6.0))
    val esperado = centroDeMasaSecuencial(sistema)
    val obtenido = centroDeMasaParalelo(sistema, sistema.length)

    assert(math.abs(obtenido.x - esperado.x) < tolerancia)
    assert(math.abs(obtenido.y - esperado.y) < tolerancia)
  }

  test("punto 4: trabajo es lineal en un tamano no usado en los ejemplos") {
    assert(trabajo(37) == 37)
  }

  test("punto 4: profundidad no divide si el umbral excede el tamano") {
    assert(profundidad(17, 20) == 17)
  }

  test("punto 4: profundidad contempla ambos lados de una division impar") {
    assert(profundidad(7, 2) == 4)
  }

  test("punto 4: profundidad suma divisiones hasta los tramos base") {
    assert(profundidad(15, 4) == 6)
  }

  test("punto 4: tiempo estimado agrega trabajo repartido entre procesadores") {
    assert(math.abs(tiempoEstimado(7, 2, 2) - 7.5) < tolerancia)
  }

  test("punto 4: mas procesadores reducen la parte de trabajo del modelo") {
    assert(tiempoEstimado(100, 8, 10) < tiempoEstimado(100, 2, 10))
  }

  test("punto 5: aceleracion acepta una mejora de cinco veces") {
    assert(math.abs(aceleracion(125.0, 25.0) - 5.0) < tolerancia)
  }

  test("punto 5: aceleracion menor que uno indica que paralelo tarda mas") {
    assert(math.abs(aceleracion(20.0, 80.0) - 0.25) < tolerancia)
  }

  test("punto 5: aceleracion es cero si el tiempo secuencial es cero") {
    assert(aceleracion(0.0, 12.0) == 0.0)
  }

  test("punto 5: aceleracion compara tiempos de unidades iguales") {
    assert(math.abs(aceleracion(75.0, 50.0) - 1.5) < tolerancia)
  }

  test("punto 5: umbral conveniente retorna un candidato medido") {
    val sistema = Array(
      Particula(Vector2(0.0, 0.0), Vector2(1.0, 0.0), 1.0),
      Particula(Vector2(1.0, 2.0), Vector2(0.0, 1.0), 1.0),
      Particula(Vector2(3.0, 4.0), Vector2(-1.0, 0.0), 1.0))
    val candidatos = List(1, 3)

    assert(candidatos.contains(umbralConveniente(sistema, 0.5, candidatos)))
  }
}
