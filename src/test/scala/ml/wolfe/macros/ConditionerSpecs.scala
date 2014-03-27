package ml.wolfe.macros

import ml.wolfe.Wolfe
import Wolfe._


/**
 * @author Sebastian Riedel
 */
class ConditionerSpecs extends StructureIsomorphisms {

  "A conditioner" should {
    "condition an atomic sample space" in {
      val space = Seq(1, 2, 3, 4)
      val structure = Conditioner.conditioned(space)(x => x == 1)
      structure mustBeIsomorphicTo (space filter (_ == 1))
    }
    "condition an atomic boolean sample space" in {
      val space = Seq(true, false)
      val structure = Conditioner.conditioned(space)(x => x)
      structure mustBeIsomorphicTo (space filter (x => x))
    }
    "condition a complex sample space " in {
      implicit val persons = Seq("Sameer", "Vivek")
      case class Data(smokes: Pred[String])
      val space = Wolfe.all(Data)
      val structure = Conditioner.conditioned(space)(x => x.smokes("Sameer"))
      structure mustBeIsomorphicTo (space filter (_.smokes("Sameer")))
    }
    "condition a complex sample space with a conjunction" in {
      implicit val ints = Range(0, 4)
      val space = Wolfe.Pred[Int]
      val structure = Conditioner.conditioned(space)(x => x(0) && x(2) && x(3))
      structure mustBeIsomorphicTo (space filter (x => x(0) && x(2) && x(3)))
    }
    "condition by specifying hidden variables in flat case class" in {
      case class Data(x: Boolean, y: Boolean, z: Boolean)
      val space = Wolfe.all(Data)
      def observed(d: Data) = d.copy(y = hide[Boolean])
      val instance = Data(true, true, false)
      val expected = space filter (observed(_) == observed(instance))
      val actual = Conditioner.conditioned(space)(observed(_) == observed(instance))
      actual mustBeIsomorphicTo expected
    }
    "condition by specifying hidden variables in nested case class" in {
      case class Nested(a: Boolean, b: Boolean)
      case class Data(x: Nested, y: Boolean, z: Boolean)
      implicit val nested = Wolfe.all(Nested)
      val space = Wolfe.all(Data)
      val instance = Data(Nested(false, false), true, false)
      def observed(d: Data) = d.copy(x = d.x.copy(a = hide[Boolean]))
      val expected = space filter (observed(_) == observed(instance))
      val actual = Conditioner.conditioned(space)(observed(_) == observed(instance))
      actual mustBeIsomorphicTo expected
    }
    "condition by specifying hidden variables in case classes nested in a sequence" in {
      case class Element(a: Boolean, b: Boolean)
      val elements = Wolfe.all(Element)
      val space = seqs(elements,10)
      val instance = Seq.fill(3)(Element(true,false))
      def observed(s:Seq[Element]) = s.map(_.copy(b = hide[Boolean]))
//      val expected = space filter (observed(_) == observed(instance))
//      val actual = Conditioner.conditioned(space)(observed(_) == observed(instance))
//      println(expected.mkString("\n"))

    }


  }

}
