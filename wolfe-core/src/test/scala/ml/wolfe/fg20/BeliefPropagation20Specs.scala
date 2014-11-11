package ml.wolfe.fg20

import ml.wolfe._
import org.scalautils.Equality

/**
 * @author Sebastian Riedel
 */
class BeliefPropagation20Specs extends WolfeSpec {


  val potFunction: Array[Int] => Double = {
    case Array(0, 0) => 1
    case Array(0, 1) => 2
    case Array(1, 0) => -3
    case Array(1, 1) => 0
  }
  val v1 = new DiscVar(Seq(false,true))
  val v2 = new DiscVar(Seq(false,true))
  val fixedTable = TablePotential.table(Array(2, 2), potFunction)
  val tablePot = new TablePotential(Array(v1,v2),fixedTable)
  val simpleProblem = Problem(Seq(tablePot),Seq(v1,v2))

  val xorFun = (arr:Array[Int]) => if ((arr(0)==1) ^ (arr(1)==1)) 0 else Double.NegativeInfinity
  val xorPot = new TablePotential(Array(v1,v2),TablePotential.table(Array(2,2),xorFun))
  val xorProblem = Problem(Seq(xorPot),Seq(v1,v2))

    def chainProblem(length: Int) = {
      val vars = for (i <- 0 until length) yield new DiscVar(Seq(false,true), "v" + i)
      val pots = for ((v1, v2) <- vars.dropRight(1) zip vars.drop(1)) yield new TablePotential(Array(v1,v2), fixedTable)
      Problem(pots,vars)
    }

  def sameVector(v1: FactorieVector, v2: FactorieVector, eps: Double = 0.00001) = {
    v1.activeDomain.forall(i => math.abs(v1(i) - v2(i)) < eps) &&
    v2.activeDomain.forall(i => math.abs(v1(i) - v2(i)) < eps)
  }

  "A BrufeForce algorithm" should {
    "return the exact log partition function" in {
      import scala.math._
      val bf = new BruteForce(simpleProblem)
      val result = bf.inferMarginals()
      val logZ = log(fixedTable.scores.map(exp).sum)
      result.logZ should be (logZ)
    }
    "return the exact MAP value" in {
      val problem = Problem(Seq(tablePot),Seq(v1,v2))
      val bf = new BruteForce(problem)
      val result = bf.inferMAP()
      val max = fixedTable.scores.max
      result.score should be (max)
    }

  }

    "A Max Product algorithm" should {
      "return the exact max-marginals when given a single table potential" in {
        val fg_mp = new MaxProduct(simpleProblem)
        val fg_bf = new BruteForce(simpleProblem)

        val mpResult = fg_mp.inferMAP(1)
        val bfResult = fg_bf.inferMAP()

        mpResult.maxMarginals should equal (bfResult.maxMarginals)
      }
      "choose a valid global max from a factor graph with multiple solutions" ignore {
        val mp = new MaxProduct(xorProblem)
        val result = mp.inferMAP()
        result.state(v1) should not be result.state(v2)
      }

      "return the exact max-marginals given a chain" in {
        val chain = chainProblem(5)

        val mp = new MaxProduct(chain).inferMAP()
        val bf = new BruteForce(chain).inferMAP()

        mp.maxMarginals should equal (bf.maxMarginals)

      }

      "return feature vectors of argmax state" in {
        val chain = chainProblem(5)

        val mp = new MaxProduct(chain).inferMAP()
        val bf = new BruteForce(chain).inferMAP()

        sameVector(mp.gradient, bf.gradient) should be(true)
      }
    }

    "A Sum Product algorithm" should {
      "return the exact marginals when given a single table potential" in {
        val fg_mp = new SumProduct(simpleProblem)
        val fg_bf = new BruteForce(simpleProblem)

        val mpResult = fg_mp.inferMarginals(1)
        val bfResult = fg_bf.inferMarginals()

        mpResult.marginals should equal (bfResult.marginals)
      }

      "return the exact marginals given a chain" in {
        val chain = chainProblem(5)

        val mp = new SumProduct(chain).inferMarginals()
        val bf = new BruteForce(chain).inferMarginals()

        mp.marginals should equal (bf.marginals)

      }



    }

  //
//  val pot: Array[Int] => Double = {
//    case Array(0, 0) => 1
//    case Array(0, 1) => 2
//    case Array(1, 0) => -3
//    case Array(1, 1) => 0
//  }
//
//  val fixedTable = TablePotential.table(Array(2, 2), pot)
//
//  val xorPot: Array[Int] => Double = arr => if ((arr(0)==1) ^ (arr(1)==1)) 0 else Double.NegativeInfinity
//  val xorTable = TablePotential.table(Array(2, 2), xorPot)
//
//  val fixedStats = LinearPotential.stats(Array(2, 2), {
//    case Array(i, j) => LinearPotential.singleton(2 * i + j, 1.0)
//  })
//
//
//  def tablePotential(fg: FactorGraph, n1: Node, n2: Node, table: Table) = {
//    val f1 = fg.addFactor()
//    val e1 = fg.addEdge(f1, n1)
//    val e2 = fg.addEdge(f1, n2)
//    f1.potential = TablePotential(Array(e1, e2), table)
//    f1
//  }
//
//  def linearPotential(fg: FactorGraph, n1: Node, n2: Node, stats: Stats) = {
//    val f1 = fg.addFactor()
//    val e1 = fg.addEdge(f1, n1)
//    val e2 = fg.addEdge(f1, n2)
//    f1.potential = new LinearPotential(Array(e1, e2), stats, fg)
//    f1
//  }
//
//  def oneFactorFG() = {
//    val fg = new FactorGraph
//    val n1 = fg.addDiscreteNode(2)
//    val n2 = fg.addDiscreteNode(2)
//    tablePotential(fg, n1, n2, fixedTable)
//    fg.build()
//    fg
//  }
//
//  def xorFG() = {
//    val fg = new FactorGraph
//    val n1 = fg.addDiscreteNode(2)
//    val n2 = fg.addDiscreteNode(2)
//    tablePotential(fg, n1, n2, xorTable)
//    fg.build()
//    fg
//  }
//
//  def chainFG(length: Int) = {
//    val fg = new FactorGraph
//    val nodes = for (i <- 0 until length) yield fg.addDiscreteNode(2)
//    for ((n1, n2) <- nodes.dropRight(1) zip nodes.drop(1)) tablePotential(fg, n1, n2, fixedTable)
//    fg.build()
//    fg
//  }
//
//  def chainFGWithFeatures(length: Int) = {
//    val fg = new FactorGraph
//    val nodes = for (i <- 0 until length) yield fg.addDiscreteNode(2)
//    for ((n1, n2) <- nodes.dropRight(1) zip nodes.drop(1)) linearPotential(fg, n1, n2, fixedStats)
//    fg.weights = LinearPotential.dense(4, 0 -> 1.0, 1 -> 2.0, 2 -> -3, 3 -> 0)
//    fg.build()
//    fg
//  }
//
//
//  def sameBeliefs(fg1: FactorGraph, fg2: FactorGraph) = {
//    def sameBeliefs(n1: List[FactorGraph.Node], n2: List[FactorGraph.Node]): Boolean = (n1, n2) match {
//      case (Nil, Nil) => true
//      //todo: this should be approx. equal on array
//      case (h1 :: t1, h2 :: t2) =>
//        MoreArrayOps.approxEqual(h1.variable.asDiscrete.b, h2.variable.asDiscrete.b) && sameBeliefs(t1, t2)
//      case _ => false
//    }
//    sameBeliefs(fg1.nodes.toList, fg2.nodes.toList)
//  }
//
//  def sameVector(v1: FactorieVector, v2: FactorieVector, eps: Double = 0.00001) = {
//    v1.activeDomain.forall(i => math.abs(v1(i) - v2(i)) < eps) &&
//    v2.activeDomain.forall(i => math.abs(v1(i) - v2(i)) < eps)
//  }
//
//  "A Max Product algorithm" should {
//    "return the exact max-marginals when given a single table potential" in {
//      val fg_mp = oneFactorFG()
//      val fg_bf = oneFactorFG()
//
//      BeliefPropagation(fg_mp, 1)
//      BruteForce.maxMarginals(fg_bf)
//
//      sameBeliefs(fg_mp, fg_bf) should be(true)
//      fg_mp.value should be(fg_bf.value)
//
//    }
//    "choose a valid global max from a factor graph with multiple solutions" in {
//      val fg = xorFG()
//      BeliefPropagation(fg, 1)
//      val v0 = fg.nodes(0).variable.asDiscrete
//      val v1 = fg.nodes(1).variable.asDiscrete
//      v0.setting should not be(v1.setting)
//    }
//    "return the exact marginals given a chain" in {
//      val fg_mp = chainFG(5)
//      val fg_bf = chainFG(5)
//
//      BeliefPropagation(fg_mp, 1)
//      BruteForce.maxMarginals(fg_bf)
//
//      sameBeliefs(fg_mp, fg_bf) should be(true)
//      fg_mp.value should be(fg_bf.value)
//
//    }
//    "return feature vectors of argmax state" in {
//      val fg_mp = chainFGWithFeatures(5)
//      val fg_bf = chainFGWithFeatures(5)
//
//      BeliefPropagation(fg_mp, 1)
//      BruteForce.maxMarginals(fg_bf)
//
//      sameBeliefs(fg_mp, fg_bf) should be(true)
//      sameVector(fg_mp.gradient, fg_bf.gradient) should be (true)
//      fg_mp.value should be(fg_bf.value)
//    }
//
//  }
//
//  "A BrufeForce algorithm" should {
//    "return the exact log partition function" in {
//      val fg_bf = oneFactorFG()
//      BruteForce.marginalize(fg_bf)
//      val logZ = log(fixedTable.scores.map(exp).sum)
//      fg_bf.value should be(logZ)
//    }
//  }
//
//  "A Sum Product algorithm" should {
//    "return the exact marginals when given a single table potential" in {
//      val fg_bp = oneFactorFG()
//      val fg_bf = oneFactorFG()
//
//      BeliefPropagation.sumProduct(1)(fg_bp)
//      BruteForce.marginalize(fg_bf)
//
//      fg_bp.value should be(fg_bf.value)
//      sameBeliefs(fg_bp, fg_bf) should be(true)
//
//
//    }
//    "return the exact marginals given a chain" in {
//      val fg_bp = chainFG(5)
//      val fg_bf = chainFG(5)
//
//      BeliefPropagation.sumProduct(1)(fg_bp)
//      BruteForce.marginalize(fg_bf)
//
//      sameBeliefs(fg_bp, fg_bf) should be(true)
//      fg_bp.value should be(fg_bf.value +- 0.001)
//
//    }
//    "return feature vectors of argmax state" in {
//      val fg_bp = chainFGWithFeatures(5)
//      val fg_bf = chainFGWithFeatures(5)
//
//      BeliefPropagation.sumProduct(1)(fg_bp)
//      BruteForce.marginalize(fg_bf)
//
//      sameBeliefs(fg_bp, fg_bf) should be(true)
//      sameVector(fg_bp.gradient, fg_bf.gradient) should be (true)
//      fg_bp.value should be(fg_bf.value +- 0.001)
//
//
//    }
//
//  }


}
