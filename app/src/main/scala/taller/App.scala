package taller

object App {

  def main(args: Array[String]): Unit = {
    val simulacion = new SimulacionParticulas()
    import simulacion._

    val dt = 0.01
    val repeticiones = 5
    val procesadores = Runtime.getRuntime.availableProcessors()
    val tamaños = List(1000000, 2000000)

    def medirSistema(n: Int): List[String] = {
      val sistema: Sistema = Array.tabulate(n) { i =>
        Particula(
          Vector2(i.toDouble, (i % 1000).toDouble),
          Vector2(1.0, 0.5),
          1.0
        )
      }
      val secuencial = medir(repeticiones) { pasoSecuencial(sistema, dt) }
      val umbrales = List(1024, n / 16, n / 4, n).distinct.sorted

      umbrales.map { umbral =>
        val paralelo = medir(repeticiones) {
          pasoParalelo(sistema, dt, umbral)
        }
        val aceleracionMedida = aceleracion(secuencial, paralelo)
        val aceleracionEstimada =
          trabajo(n).toDouble / tiempoEstimado(n, procesadores, umbral)
        f"$n%d | $umbral%d | $secuencial%.4f | $paralelo%.4f | " +
          f"$aceleracionMedida%.4f | $aceleracionEstimada%.4f"
      }
    }

    val filas = tamaños.flatMap(medirSistema)
    println("Simulacion de particulas")
    println(s"Nucleos disponibles: $procesadores")
    println(s"Repeticiones por medicion: $repeticiones; dt: $dt")
    println("n | umbral | secuencial (ms) | paralelo (ms) | aceleracion | estimacion")
    println(filas.mkString("\n"))
  }
}
