val v = Stream.from(0) zip (Stream.from(1))
v take 5 foreach println

Stream.from(1) take 5 foreach println
