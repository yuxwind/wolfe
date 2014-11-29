package ml.wolfe.apps.factorization.io

import cc.factorie.la.DenseTensor1
import ml.wolfe.FactorieVector
import ml.wolfe.apps.factorization.TensorDB

/**
 * @author rockt
 */
object VectorInspector extends App {
  def calculateLengthsAndAngle(v1: FactorieVector, v2: FactorieVector): (Double, Double, Double) = {
    val length1 = math.sqrt(v1 dot v1)
    val length2 = math.sqrt(v2 dot v2)

    val angle = math.acos((v1 dot v2) / (length1 * length2)) * (180 / math.Pi)

    (length1, length2, angle)
  }

  val pathToDB = args.lift(0).getOrElse("./wolfe-apps/data/out/F")

  val db = new TensorDB(100)
  println("Deserializing DB...")
  db.deserialize(pathToDB + "/serialized/")

  println("Analyzing vectors...")
  //val pathToAnnotatedFormulae = args.lift(1).getOrElse("./wolfe-apps/data/formulae/curated.txt")
  //val pathToAllFormulae = args.lift(2).getOrElse("./wolfe-apps/data/formulae/generated.txt")
  val pathToAnnotatedFormulae = args.lift(1).getOrElse("./wolfe-apps/data/formulae/latest.txt")
  val pathToAllFormulae = args.lift(2).getOrElse("./wolfe-apps/data/formulae/latest.txt")

  def printStats(premise: String, consequent: String): (Double, Double, Double) = {
    val premiseVector = db.vector1(premise).get
    val consequentVector = db.vector1(consequent).get

    val (premiseLength, consequentLength, angle) = calculateLengthsAndAngle(premiseVector, consequentVector)

    val correctLength = if (premiseLength < consequentLength) "true" else "false"

    //println("%4.2f°".format(angle) + s"\t$premiseLength\t$consequentLength\t$correctLength")
    println("%4.2f°".format(angle) + s"\t$premiseLength\t$consequentLength")

    (premiseLength, consequentLength, angle)
  }

  val debug = false
  val dropFormulae = 0
  val numSamples = 10


  val pairsOfRelations =
    if (debug) Seq("path#poss|<-poss<-executive->appos->|appos:INV" -> "REL$/business/person/company")
    else {
      val annotatedFormulae = io.Source.fromFile(pathToAnnotatedFormulae).getLines().toList.drop(dropFormulae * 3)
        .filterNot(l => l.startsWith("//") || l.isEmpty)
        .map(_.split(" => ")).map(a => (a(0), a(1))).filterNot(_._2.startsWith("!"))

      val allFormulae = io.Source.fromFile(pathToAllFormulae).getLines().toList
        .filterNot(l => l.startsWith("//") || l.isEmpty)
        .map(_.split(" => ")).map(a => (a(0), a(1))).filterNot(_._2.startsWith("!"))

      annotatedFormulae.take(numSamples) ++ allFormulae.takeRight(numSamples)
    }

  println(pairsOfRelations.take(numSamples).mkString("\n"))
  println()

  println(s"Top $numSamples implications")
  val top10Stats = pairsOfRelations.take(numSamples).map(t => printStats(t._1, t._2))
  val top10AvgLengthDiff = top10Stats.map(t => t._2 - t._1).sum / numSamples.toDouble
  val top10AvgAngle = top10Stats.map(_._3).sum / numSamples.toDouble
  println("Average length difference: " + top10AvgLengthDiff)
  println("Average angle: " + top10AvgAngle)
  println()
  println(s"Bottom $numSamples implications")
  val least10Stats = pairsOfRelations.takeRight(numSamples).map(t => printStats(t._1, t._2))
  val least10AvgLengthDiff = least10Stats.map(t => t._2 - t._1).sum / numSamples.toDouble
  val least10AvgAngle = least10Stats.map(_._3).sum / numSamples.toDouble
  println("Average length difference: " + least10AvgLengthDiff)
  println("Average angle: " + least10AvgAngle)
}

object VectorInspectorSpec extends App {
  println(VectorInspector.calculateLengthsAndAngle(new DenseTensor1(Array(3.0, 0.0)), new DenseTensor1(Array(5.0, 5.0)))._3)
  println(VectorInspector.calculateLengthsAndAngle(new DenseTensor1(Array(3.0, 4.0)), new DenseTensor1(Array(-8.0, 6.0)))._3)
  println(VectorInspector.calculateLengthsAndAngle(new DenseTensor1(Array(5.0, 6.0)), new DenseTensor1(Array(-1.0, 4.0)))._3)
  println(VectorInspector.calculateLengthsAndAngle(new DenseTensor1(Array(3.0, 5.0)), new DenseTensor1(Array(-1.0, 6.0)))._3)
}