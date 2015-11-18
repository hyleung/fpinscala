---
layout: post
title:  "Chapter 13: External Effects and IO"
date:   2015-11-01 20:32:40 -0700
categories: fpinscala chapter_notes
---

# External Effects and IO

The goal here is to provide a…*way of embedding imperative programming with I/O effects in a pure program while **preserving referential transparency**.*

We separate the code that has *external effects*, from code that does not.

## Factoring effects

{% highlight scala %}
case class Player(name:String, score:Int)

def contest(p1: Player, p2: Player):Unit =
  if (p1.score > p2.score)
    println(s"${p1.name} is the winner!")
  else if (p2.score > p1.score)
    println(s"${p2.name} is the winner!")
  else
    println("It's a draw!")    
{% endhighlight %}

In the preceding example, we have side effects - that is `println`, which is I/O - mixed in with the pure logic.

We can move things in the right direction by factoring out the logic (i.e. who wins) into a separate function:

{% highlight scala %}
def winner(p1: Player, p2: Player):Option[Player] {
  if (p1.score > p2.score) Some(p1)
  else if (p2.score > p1.score) Some(p2)
  else None
}

def contest(p1: Player, p2: Player):Unit = winner(p1, p2) match {
  case Some(p) => println(s"${p.name} is the winner!")
  case None => println("It's a draw!")
}
{% endhighlight %}

This is *a bit* better. But `contest` still has two responsibilities here: computing the message to be displayed and *actually* displaying the message (printing it to console). We can refactor this further:

{% highlight scala %}
def computeMessage(p:Option[Player]):String = p map {
  case Player(name,_) => s"$name is the winner!"
} getOrElse "It's a draw!"

def contest(p1: Player, p2: Player):Unit =>
  println(computeMessage(winner(p1, p2)))
{% endhighlight %}
