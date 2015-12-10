package fpinscala.localeffects

import org.scalatest.{FlatSpec, Matchers}

class STMapSpec extends FlatSpec with Matchers {

  behavior of "STMap"
  it should "create empty map" in {
    val r = ST.runST(new RunnableST[Int] {
      def apply[S] = for {
        map <- STMap.empty[S,String,Int]
        size <- map.size
      } yield size
    })
    r should be (0)
  }
  it should "put entries" in {
    val r = ST.runST(new RunnableST[Int] {
      def apply[S] = for {
        map <- STMap.empty[S,String,Int]
        _ <- map.put("foo",1)
        size <- map.size
      } yield size
    })
    r should be (1)
  }
  it should "get entries" in {
    val r = ST.runST(new RunnableST[Option[Int]] {
      def apply[S] = for {
        map <- STMap.empty[S,String,Int]
        _ <- map.put("foo",1)
        v <- map.get("foo")
      } yield v 
    })
    r should be (Some(1)) 
  }
  it should "get entries using apply" in {
    val r = ST.runST(new RunnableST[Int] {
      def apply[S] = for {
        map <- STMap.empty[S,String,Int]
        _ <- map.put("foo",1)
        v <- map("foo")
      } yield v 
    })
    r should be (1) 
  }
  it should "remove entries" in {
    val r = ST.runST(new RunnableST[Int] {
      def apply[S] = for {
        map <- STMap.empty[S,String,Int]
        _ <- map.put("foo",1)
        _ <- map.remove("foo")
        size <- map.size
      } yield size 
    })
    r should be (0) 
  }
}
