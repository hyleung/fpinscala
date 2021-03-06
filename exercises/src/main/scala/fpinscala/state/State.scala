package fpinscala.state


trait RNG {
  def nextInt: (Int, RNG) // Should generate a random `Int`. We'll later define other functions in terms of `nextInt`.
}

object RNG {
  // NB - this was called SimpleRNG in the book text

  case class Simple(seed: Long) extends RNG {
    def nextInt: (Int, RNG) = {
      val newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL // `&` is bitwise AND. We use the current seed to generate a new seed.
      val nextRNG = Simple(newSeed) // The next state, which is an `RNG` instance created from the new seed.
      val n = (newSeed >>> 16).toInt // `>>>` is right binary shift with zero fill. The value `n` is our new pseudo-random integer.
      (n, nextRNG) // The return value is a tuple containing both a pseudo-random integer and the next `RNG` state.
    }
  }

  type Rand[+A] = RNG => (A, RNG)

  val int: Rand[Int] = _.nextInt

  def unit[A](a: A): Rand[A] =
    rng => (a, rng)

  def map[A,B](s: Rand[A])(f: A => B): Rand[B] =
    rng => {
      val (a, rng2) = s(rng)
      (f(a), rng2)
    }

  def _map[A,B](s: Rand[A])(f: A => B): Rand[B] =
    flatMap(s){a => unit(f(a))}

  def nonNegativeInt(rng: RNG): (Int, RNG) = rng.nextInt match {
    case (next,nextState) if next < 0  => (-(next + 1), nextState)
    case p => p
  }

  def nonNegativeLessThan(n:Int):Rand[Int] =
      flatMap(nonNegativeInt){ i =>
          val mod = i % n
          if (i + (n - 1) - mod >= 0) unit(mod)
          else nonNegativeLessThan(n)
      }


  def double(rng: RNG): (Double, RNG) = {
    val (i, nextState) = nonNegativeInt(rng)
    (i / Int.MaxValue.toDouble, nextState)
  }

  def doubleWithMap:Rand[Double] = map(nonNegativeInt)(i => i/Int.MaxValue.toDouble)

  def intDouble(rng: RNG): ((Int,Double), RNG) = {
    val (i, s1) = rng.nextInt
    val (d, s2) = double(s1)
    ((i, d), s2)
  }

  def doubleInt(rng: RNG): ((Double,Int), RNG) = {
    val (d, s1) = double(rng)
    val (i, s2) = s1.nextInt
    ((d,i), s2)
  }
  def double3(rng: RNG): ((Double,Double,Double), RNG) = {
    val (a, s1) = double(rng)
    val (b, s2) = double(s1)
    val (c, s3) = double(s2)
    ((a,b,c), s3)
  }

  def ints(count: Int)(rng: RNG): (List[Int], RNG) = Range(0,count).foldLeft((List.empty[Int],rng)){
    case ((list,next), x) => {
      val (i, next) = rng.nextInt
      (list :+ i, next)
    }
  }

  def map2[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    rng => {
      val (a,rng0) = ra(rng)
      val (b,rng1) = rb(rng0)
      (f(a,b),rng1)
    }

  def _map2[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    flatMap(ra)(a =>
      flatMap(rb)(b =>
        unit(f(a,b))
      )
    )

  def flatMap[A,B](f: Rand[A])(g: A => Rand[B]): Rand[B] = rng => {
    val (x, next) = f(rng)
    g(x)(next)
  }

  def sequence[A](fs: List[Rand[A]]): Rand[List[A]] =
    fs.foldRight(unit(List.empty[A])){ (f,acc) =>
      map2(f,acc)( (x,xs) => x :: xs)
    }

}

case class State[S,+A](run: S => (A, S)) {
  def map[B](f: A => B): State[S, B] = flatMap(a => State.unit(f(a)))

  def map2[B,C](sb: State[S, B])(f: (A, B) => C): State[S, C] =
    flatMap{a => sb.map { b => f(a, b) }}

  def flatMap[B](f: A => State[S, B]): State[S, B] =
    State{ (s:S) =>
      //get the value of type A and next state B
      val (a, s1) = run(s)
      /* f(a) returns a function A => State[S,B]
         so we need to evaluate the function to
         get an actual State[S,B]
       */
      f(a).run(s1)
    }
}

sealed trait Input
case object Coin extends Input
case object Turn extends Input

case class Machine(locked: Boolean, candies: Int, coins: Int)

object State {
  def unit[S, A](a: A): State[S, A] = State{s:S => (a,s)}
  def sequence[S, A](sas: List[State[S, A]]): State[S, List[A]] =
    sas.foldRight(unit[S,List[A]](List.empty))((s,acc) => s.map2(acc)((a,b) => a :: b) )

  type Rand[A] = State[RNG, A]
  def simulateMachine(inputs: List[Input]): State[Machine, (Int, Int)] =
    State{ s:Machine =>
        val r = inputs.foldLeft(s){
          case (m, _) if m.candies == 0 => m
          case (m, Turn) if m.locked => m
          case (Machine(true, candies, coins), Coin) => Machine(false, candies, coins + 1)
          case (Machine(false, candies, coins), Coin) => Machine(false, candies, coins + 1)
          case (Machine(false, candies, coins), Turn) => Machine(true, candies - 1, coins)
          case _ => ???
        }
      ((r.candies, r.coins), r)
    }
}
