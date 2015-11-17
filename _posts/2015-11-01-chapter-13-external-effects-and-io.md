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
