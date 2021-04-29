---
title: "Validation"
description: ""
lead: ""
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

## Concept

`Validation` is an abstraction for describing something that can either be a valid result, or an error message.
Scalaz has a great implementation, but Gatling has its own, less powerful but much more simple.

The benefit of using this abstraction is that it's composable, so one can chain operations that consume and produce validations without having to determine on every operation if it's actually dealing with a succeeding operation or not.

`Validation[T]` has a type parameter `T` that is the type of the value in case of a success.

It has 2 implementations:

* `Success[T](value: T)` that wraps a value in case of a success
* `Failure(message: String)` that wraps a String error message

The goal of such an abstraction is to deal with "unexpected results" in a composable and cheap way instead of using Exceptions.

## Usage

### Creating instances

First, import the `validation` package:

```scala
||< include-static "ValidationSample.scala#import" >||
```

Then, you can either directly create new instance of the case classes:

```scala
||< include-static "ValidationSample.scala#with-classes" >||
```

or use the helpers:

```scala
||< include-static "ValidationSample.scala#with-helpers" >||
```

### Manipulating

`Validation` can be used with pattern matching:

```scala
||< include-static "ValidationSample.scala#pattern-matching" >||
```

`Validation` has the standard Scala "monadic" methods such as:

* `map`:expects a function that takes the value if it's a success and return a value.
* `flatMap`: expects a function that takes the value if it's a success and return a new `Validation`

Basically, `map` is used to **chain with an operation that can't fail**, hence return a raw value:

```scala
||< include-static "ValidationSample.scala#map" >||
```

`flatMap` is used to **chain with an operation that can fail**, hence return a `Validation`:

```scala
||< include-static "ValidationSample.scala#flatMap" >||
```

In both case, the chained function is not called if the original `Validation` was a `Failure`:

```scala
||< include-static "ValidationSample.scala#map-failure" >||
```

You can also use Scala *"for comprehension"* syntactic sugar.

For the impatient, just consider it's like a super loop that can iterate other multiple objects of the same kind (like embedded loops) and can iterate over other things that collections, such as `Validation`s or `Option`s.

Here's what the above example would look like using a *"for comprehension"*:

```scala
||< include-static "ValidationSample.scala#for-comp" >||
```
