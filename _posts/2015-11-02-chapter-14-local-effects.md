---
layout: post
title:  "Chapter 14: Local Effects and Mutable State"
date:   2015-11-02  20:32:40 -0700
categories: fpinscala chapter_notes
---

# Local Effects and Mutable State

Some definitions:
    
**Referential Transparency**:

*"An expression `e` is referentially transparent if for all programs `p` all occurences of
`e` can be replaced by the result of evaluating `e` without affecting the meaning of `p`"*

**Pure functions**: 

*"A function `f` is pure if the expression `f(x)` is referentially transparent for all referentially
transparent `x`*

...but notice neither definition says anything about mutable state.


