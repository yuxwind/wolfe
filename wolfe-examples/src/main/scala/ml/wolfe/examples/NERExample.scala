package ml.wolfe.examples

import java.io.{InputStream, BufferedInputStream, FileNotFoundException}
import ml.wolfe.util._
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import ml.wolfe.{MaxProduct, Wolfe}
import cc.factorie.optimize.{AveragedPerceptron, OnlineTrainer}
import ml.wolfe.macros.{Library, OptimizedOperators}

/**
 * Created by rockt on 07/04/2014.
 *
 * Linear-chain example for BioNLP 2004 NER corpus.
 * See http://www.nactem.ac.uk/tsujii/GENIA/ERtask/report.html
 */
object NERExample {
  import Wolfe._
  import OptimizedOperators._
  import NLP.{Sentence, Chunk, Tag, Token, groupLines}
  //import NLP._
  import Library._

  implicit val defaultChunks = Seq("?").map(Chunk)
  implicit val labels = Seq("O", "B-protein", "I-protein", "B-cell_type", "I-cell_type", "B-DNA", "I-DNA",
    "B-cell_line", "I-cell_line", "B-RNA", "I-RNA").map(Tag)

  def toFeatureVector(token: Token): Wolfe.Vector = {
    oneHot('word -> token.word.toLowerCase -> token.tag) +
    oneHot('firstCap -> token.tag, I(token.word.head.isUpper)) +
    oneHot('allCap -> token.tag, I(token.word.matches("[A-Z]+"))) +
    oneHot('realNumber -> token.tag, I(token.word.matches("[-0-9]+[.,]+[0-9.,]+"))) +
    oneHot('isDash -> token.tag, I(token.word.matches("[-–—−]"))) +
    oneHot('isQuote -> token.tag, I(token.word.matches("[„“””‘’\"']"))) +
    oneHot('isSlash -> token.tag, I(token.word.matches("[/\\\\]"))) +
    oneHot('prefix2 -> token.tag -> token.word.take(2)) +
    oneHot('suffix2 -> token.tag -> token.word.takeRight(2))
  }

  def Sentences = Wolfe.all(Sentence)(seqs(all(Token)))

  def observed(s: Sentence) = s.copy(tokens = s.tokens.map(_.copy(tag = hidden)))
  
  def features(s: Sentence): Wolfe.Vector = {
    sum { over(0 until s.tokens.size) of (i => toFeatureVector(s.tokens(i))) } +
    sum { over(0 until s.tokens.size - 1) of (i => oneHot('transition -> s.tokens(i).tag -> s.tokens(i + 1).tag)) }
  }

  @OptimizeByInference(MaxProduct(_, 1))
  def model(w: Vector)(s: Sentence) = w dot features(s)
  def predictor(w: Vector)(s: Sentence) = argmax { over(Sentences) of model(w) st evidence(observed)(s) }

  //100: epochs, -1: report interval
  @OptimizeByLearning(new OnlineTrainer(_, new AveragedPerceptron, 100, -1))
  def loss(data: Iterable[Sentence])(w: Vector) = sum { over(data) of (s => model(w)(predictor(w)(s)) - model(w)(s)) }
  def learn(data:Iterable[Sentence]) = argmin { over[Vector] of loss(data) }

  def main(args: Array[String]) {
    import scala.sys.process._

    def loadGenia(path: String): InputStream = Util.getStreamFromClassPathOrFile(path)

    val trainPath = "ml/wolfe/datasets/genia/Genia4ERtraining.tar.gz"
    val testPath = "ml/wolfe/datasets/genia/Genia4ERtest.tar.gz"

    //if genia corpus is not present, download it
    val (trainSource, testSource) =
      try {
        (loadGenia(trainPath), loadGenia(testPath))
      } catch {
        case f: FileNotFoundException =>
          "wget http://www.nactem.ac.uk/tsujii/GENIA/ERtask/Genia4ERtraining.tar.gz -P wolfe-examples/src/main/resources/ml/wolfe/datasets/genia/".!!
          "wget http://www.nactem.ac.uk/tsujii/GENIA/ERtask/Genia4ERtest.tar.gz -P wolfe-examples/src/main/resources/ml/wolfe/datasets/genia/".!!
          val prefix = "wolfe-examples/src/main/resources/"
          (loadGenia(prefix + trainPath), loadGenia(prefix + testPath))
      }

    val train = IOBToWolfe(groupLines(loadIOB(trainSource).toIterator, "###MEDLINE:")).flatten
    val test = IOBToWolfe(groupLines(loadIOB(testSource).toIterator, "###MEDLINE:")).flatten

    val w = learn(train)

    def evaluate(corpus: Seq[Sentence]) = {
      val predicted = map { over(corpus) using predictor(w) }
      val evaluated = MentionEvaluator.evaluate(corpus, predicted)
      println(evaluated)
    }

    println("Train:")
    evaluate(train)
    println("Test:")
    evaluate(test)
  }

  def loadIOB(stream: InputStream) = {
    val in = new BufferedInputStream(stream)
    val gzIn = new GzipCompressorInputStream(in)
    val tarIn = new TarArchiveInputStream(gzIn)
    var entry = tarIn.getNextEntry

    var lines = Array[String]()

    while (entry != null) {
      if (entry.getName.endsWith("2.iob2")) {
        val content = new Array[Byte](entry.getSize.toInt)
        tarIn.read(content, 0, entry.getSize.toInt)
        val text = new String(content)
        lines = text.split("\n")
      }
      entry = tarIn.getNextEntry
    }

    lines
  }

  def IOBToWolfe(docs: Seq[Seq[String]]): Seq[Seq[Sentence]] = {
    for (doc <- docs) yield {
      (for (sentence <- doc.mkString("\n").split("\n\n")) yield {
        Sentence(
          for {
            line <- sentence.split("\n")
            if !line.startsWith("###MEDLINE:")
          } yield {
            val Array(word, label) = line.split("\t")
            //FIXME: chunk ("?") shouldn't be needed here, but toString of default value throws Exception
            Token(word, label, "?")
          }
        )
      }).toSeq
    }
  }
}