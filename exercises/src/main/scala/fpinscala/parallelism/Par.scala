package fpinscala.parallelism

import java.util.concurrent._

object Par {
  type Par[A] = ExecutorService => Future[A]
  
  def run[A](s: ExecutorService)(a: Par[A]): Future[A] = a(s)

  def unit[A](a: A): Par[A] = (es: ExecutorService) => UnitFuture(a) // `unit` is represented as a function that returns a `UnitFuture`, which is a simple implementation of `Future` that just wraps a constant value. It doesn't use the `ExecutorService` at all. It's always done and can't be cancelled. Its `get` method simply returns the value that we gave it.

  def lazyUnit[A](a: => A):Par[A] = fork(unit(a))

  private case class UnitFuture[A](get: A) extends Future[A] {
    def isDone = true 
    def get(timeout: Long, units: TimeUnit) = get 
    def isCancelled = false 
    def cancel(evenIfRunning: Boolean): Boolean = false 
  }
  
  def map2[A,B,C](a: Par[A], b: Par[B])(f: (A,B) => C): Par[C] = // `map2` doesn't evaluate the call to `f` in a separate logical thread, in accord with our design choice of having `fork` be the sole function in the API for controlling parallelism. We can always do `fork(map2(a,b)(f))` if we want the evaluation of `f` to occur in a separate thread.
    (es: ExecutorService) => {
      val af = a(es) 
      val bf = b(es)
      UnitFuture(f(af.get, bf.get)) // This implementation of `map2` does _not_ respect timeouts, and eagerly waits for the returned futures. This means that even if you have passed in "forked" arguments, using this map2 on them will make them wait. It simply passes the `ExecutorService` on to both `Par` values, waits for the results of the Futures `af` and `bf`, applies `f` to them, and wraps them in a `UnitFuture`. In order to respect timeouts, we'd need a new `Future` implementation that records the amount of time spent evaluating `af`, then subtracts that time from the available time allocated for evaluating `bf`.
    }

  def map3[A,B,C,D](pa: Par[A], pb: Par[B], pc:Par[C])(f: (A,B,C) => D): Par[D] =
    map2(pc, map2(pa, pb){(a,b) => (a, b)}){( c, ab) =>  f(ab._1, ab._2, c)}


  def fork[A](a: => Par[A]): Par[A] = // This is the simplest and most natural implementation of `fork`, but there are some problems with it--for one, the outer `Callable` will block waiting for the "inner" task to complete. Since this blocking occupies a thread in our thread pool, or whatever resource backs the `ExecutorService`, this implies that we're losing out on some potential parallelism. Essentially, we're using two threads when one should suffice. This is a symptom of a more serious problem with the implementation, and we will discuss this later in the chapter.
    es => es.submit(new Callable[A] { 
      def call = a(es).get
    })

  def map[A,B](pa: Par[A])(f: A => B): Par[B] = 
    map2(pa, unit(()))((a,_) => f(a))

  def sortPar(parList: Par[List[Int]]) = map(parList)(_.sorted)

  def equal[A](e: ExecutorService)(p: Par[A], p2: Par[A]): Boolean = 
    p(e).get == p2(e).get

  def delay[A](fa: => Par[A]): Par[A] = 
    es => fa(es)

  def choice[A](cond: Par[Boolean])(t: Par[A], f: Par[A]): Par[A] =
    es => 
      if (run(es)(cond).get) t(es) // Notice we are blocking on the result of `cond`.
      else f(es)

  def asyncF[A,B](f: A => B): A => Par[B] = a => lazyUnit(f(a))

  def sequence[A](l: List[Par[A]]):Par[List[A]] = l.foldLeft(unit(List[A]()))((pacc,pa) => map2(pacc,pa)((a,acc) => a :+ acc))

  def parMap[A,B](l: List[A])(f: A => B):Par[List[B]] = fork {
    val fbs: List[Par[B]] = l.map(asyncF(f))
    sequence(fbs)
  }

  def parFilter[A](l: List[A])(f: A => Boolean): Par[List[A]] =  {
    val pars: List[Par[List[A]]] =
      l.map(asyncF((a:A) => if (f(a)) List(a) else List()))
    val seq = sequence(pars) //convert to Par[List[List[A]]]
    seq.map( sl => sl.flatten ) // convert each List[List[A]] into List[A]
  }

  def choiceN[A](a: Par[Int])(choices: List[Par[A]]): Par[A] =
    es => {
      val i = a(es).get()
      choices(i)(es)
    }

  def choiceMap[A,B](a: Par[A])(choices: Map[A,Par[B]]): Par[B] =
    es => {
      choices(a(es).get())(es)
    }
  def flatMap[A,B](pa:Par[A])(choices: A => Par[B]):Par[B]  =
    es => {
      choices(pa(es).get())(es)
    }

  def _choice[A](cond: Par[Boolean])(t: Par[A], f: Par[A]): Par[A] =
    choiceN(map(cond)(b => if (b) 0 else 1))(List(t, f))

  def join[A](ppa: Par[Par[A]]):Par[A] =
    ppa.flatMap(pa => pa)

  /* Gives us infix syntax for `Par`. */
  implicit def toParOps[A](p: Par[A]): ParOps[A] = new ParOps(p)

  class ParOps[A](p: Par[A]) {
    def map[B](f: A => B):Par[B] = Par.map(p)(f)
    def map2[B,C](pb:Par[B])(f:(A,B) => C):Par[C] = Par.map2(p,pb)(f)
    def flatMap[B](f: A => Par[B]):Par[B] = Par.flatMap(p)(f)
    def equal(pa:Par[A])(e:ExecutorService):Boolean = Par.equal(e)(p, pa)
    def fork:Par[A] = Par.fork(p)
    def run(executor:ExecutorService):Future[A] = Par.run(executor)(p)
  }
}

object Examples {
  import Par._
  def sum(ints: IndexedSeq[Int]): Int = // `IndexedSeq` is a superclass of random-access sequences like `Vector` in the standard library. Unlike lists, these sequences provide an efficient `splitAt` method for dividing them into two parts at a particular index.
    if (ints.size <= 1)
      ints.headOption getOrElse 0 // `headOption` is a method defined on all collections in Scala. We saw this function in chapter 3.
    else { 
      val (l,r) = ints.splitAt(ints.length/2) // Divide the sequence in half using the `splitAt` function.
      sum(l) + sum(r) // Recursively sum both halves and add the results together.
    }
  def sumPar(ints: IndexedSeq[Int]):Par[Int]  =
    if (ints.size <= 1)
        unit(ints.headOption getOrElse 0)
    else {
      val (l,r) = ints.splitAt(ints.length/2)
      map2(fork(sumPar(l)), fork(sumPar(r)))(_ + _)
    }
}
