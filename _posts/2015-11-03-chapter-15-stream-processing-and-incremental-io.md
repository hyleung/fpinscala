---
layout: post
title:  "Chapter 15: Stream Processing and Incremental IO" 
date:   2015-11-03  20:32:40 -0700
categories: fpinscala chapter_notes
---

# Stream Processing and Incremental IO

So far we have seen types like `IO` and `ST` which work by embedding an imperative style in our functional Scala
program. What we lose, however, is the ability to use composition to build our programs.

## A motivating example: counting line numbers in a file

Check whether a file contains greater than 40,000 lines.

Using an imperative style, might look something like this:

{%highlight scala%}
def linesGt40k(fileName:String):IO[Boolean] = IO {
    val src = io.Source.fromFile(filename)
    try {
        var count = 0
        val lines = Iterator[String] = src.getLines
        while (count <= 40000 && lines.hasNext) {
            lines.next
            count += 1
        }
        count > 40000
   finally src.close
}
{%endhighlight%}

On the plus side, this code doesn't load the entire file into memory. It also terminates as soon as it has an answer.

But...
- we have to remember to close the file after we're done. What we *want* is to build a library that ensures resource
safety 
- it has a mix of concerns: file IO, iteration, "actual" logic. This limits reuse/composibility

Other variations:
- Check whether the number of *non-empty* lines is > 40,000
- Find a line index before 40,000 where the first letters on consecutive lines spell out a particular word.

Compare this to how we'd solve this with a `Stream[String]`:

{%highlight scala%}
lines.zipWithIndex.exists(_.2 + 1 >= 40000)

lines.filter(!_.trim.isEmpty)
    .zipWithIndex
    .exists(_.2 + 1 >= 40000)

lines.filter(!_.trim.isEmpty)
    .take(40000)
    .map(_.head)
    .indexOfSlice("abracadabra".toList)
{%endhighlight%}

But, we don't have a `Stream[String]`, but a file...

A cheat using *lazy I/O*:
{%highlight scala%}
def lines(filename:String}:IO[Stream[String]] = IO {
    val src = io.Source.fromFile(filename)
    src.getLines.toStream append {src.close; Stream.empty;}
{%endhighlight%}

This is a cheat because the value inside (the `Stream`) isn't a pure value - it executes the side-effect of readng the
file. Also, the file resource is **only** closed when we read to the end of the stream - problematic for cases where we
wish to terminate early.

There's also nothing to prevent traversal of the stream more than once. Since we are executing some side-effecting IO,
we may run into problems when there are multiple threads processig the same stream.

In order to process the `Stream` safely, we'd need to have a good idea of where it comes from, where/how it's being
used, etc. 

> This is bad for composition, where we shouldn't have to know anything about the value other than its type





