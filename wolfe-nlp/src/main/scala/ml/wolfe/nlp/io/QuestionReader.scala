package ml.wolfe.nlp.io

/**
 * @author narad
 * @author mbosnjak
 */
class MCTestReader(tsvFilename: String, ansFilename: String) extends Iterable[MultiQuestion] {
  private val MC_LABELS = Array("A", "B", "C", "D")

  def iterator: Iterator[MultiQuestion] = {
    val tsvReader = io.Source.fromFile(tsvFilename).getLines()
    val ansReader = io.Source.fromFile(ansFilename).getLines()
    val zippedReaders = tsvReader.zip(ansReader)

    zippedReaders.map{ x =>
      val l = x._1
      val ans = x._2.split("\\s")
      val fields = l.split("\t")
      val id = fields(0)
      val author = fields(1)
      val passage = fields(2).replaceAll("\\\\newline", " ")
      val questions = fields.slice(3, fields.size).grouped(5).zip(ans.toIterator).map { case(g, t) =>
        val gi = g.head.split(": ")
        val (qt, q) = (gi(0), gi(1))
        println(qt)
        val as = g.tail.zipWithIndex.map { case(a, i) =>
          AnswerChoice(MC_LABELS(i), a, t == MC_LABELS(i))
        }
        new MultipleChoiceQuestion(q, as, qt)
      }.toIterable
      MultiQuestion(id, author, passage, questions)
    }
  }
}

object MCTestReader {

  def main(args: Array[String]) {
    val tsvFilename = args.lift(0).getOrElse("../mctest/data/MCTest/mc160.dev.tsv")
    val ansFilename = args.lift(1).getOrElse("../mctest/data/MCTest/mc160.dev.ans")
    for (q <- new MCTestReader(tsvFilename, ansFilename)) {
      println(q + "\n")
    }
  }
}

case class MultiQuestion(id: String, author: String, passage: String, questions: Iterable[MultipleChoiceQuestion]) {

  override def toString = {
    passage + "\n" + questions.mkString("\n")
  }
}


abstract case class Question(text: String) {

  def isCorrect(str: String): Boolean
}

class MultipleChoiceQuestion(text: String, choices: Seq[AnswerChoice], typ: String) extends Question(text) {

  def isCorrect(label: String): Boolean = {
    choices.exists{ c => c.label == label && c.isCorrect}
  }

  override def toString = {
    (Array(text) ++ choices.map { a =>
      " (%s) [%s]\t%s".format(a.label, if (a.isCorrect) "X" else " ", a.text)
    }).mkString("\n")
  }
}

case class AnswerChoice(label: String, text: String, isCorrect: Boolean)