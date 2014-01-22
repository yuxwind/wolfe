package scalapplcodefest.newExamples

import scalapplcodefest.sbt._
import scalapplcodefest.{Wolfe, Util}
import scala.io.Source
import scala.util.Random

/**
* @author Sebastian Riedel
*/
@Compile class Iris extends (() => Unit) {

  import Wolfe._
  import Iris._

  def apply() {
    val random = new Random(0l)
    val dataset = random.shuffle(Iris.loadIris())
    val (train, test) = dataset.splitAt(dataset.size / 2)

    def sampleSpace = all(Data)(c(all(Observed)(c(doubles, doubles, doubles, doubles)), classes))

    //    def features(data: Data) = {
    //      import data._
    //      import data.x._
    //      oneHot('sl -> y, sepalLength) + oneHot('sw -> y, sepalWidth) + oneHot('pl -> y, petalLength) + oneHot('pw -> y, petalWidth)
    //    }

    def features(data: Data) = oneHot('sl -> data.y, data.x.sepalLength)

    def model(weights: Vector)(data: Data) = features(data) dot weights

    def loss(weights: Vector)(data: Data) = max2(sampleSpace)(_.x == data.x)(model(weights)) - model(weights)(data)

    val learned = argmin2(vectors)(_ => true)(w => sum2(train)(_ => true)({data => loss(w)(data)}))

    def predict(data: Data) = argmax2(sampleSpace)(_.x == data.x)(model(learned))

    println(learned)

    val predicted = test.map(predict)

    println(test.head)
    println(predicted.head)

  }


}
@Analyze object Iris {

  type IrisClass = String
  val classes = Set("Iris-setosa", "Iris-versicolor", "Iris-virginica")

  case class Observed(sepalLength: Double, sepalWidth: Double, petalLength: Double, petalWidth: Double)

  case class Data(x: Observed, y: IrisClass)

  def loadIris() = {
    val stream = Util.getStreamFromClassPathOrFile("scalapplcodefest/datasets/iris/iris.data")
    val data = for (line <- Source.fromInputStream(stream).getLines().toBuffer if line.trim != "") yield {
      val Array(sl, sw, pl, pw, ic) = line.split(",")
      Data(Observed(sl.toDouble, sw.toDouble, pl.toDouble, pw.toDouble), ic)
    }
    stream.close()
    data
  }

  def main(args: Array[String]) {
    GenerateSources.generate(
      sourcePath = "src/main/scala/scalapplcodefest/newExamples/Iris.scala",
      replacers = List(
        env => new ArgminByFactorieTrainerReplacer(env) with SimpleDifferentiator))

  }

}
//
