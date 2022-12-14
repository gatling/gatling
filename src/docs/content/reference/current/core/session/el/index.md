---
title: "Expression Language"
description: "How to use Gatling Expression Language to compute dynamic parameters based on Session data using a simple text templating engine"
lead: "Use the Gatling Expression Language to generate dynamic parameters"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
weight: 2030502
---

Most Gatling DSL methods can be passed **Gatling Expression Language (EL)** Strings.

This is a very convenient feature to pass dynamic parameters.

{{< alert warning >}}
Only Gatling DSL methods will interpolate Gatling EL Strings.
You can't use Gatling EL in your own methods or functions.

For example `queryParam("latitude", session -> "#{latitude}")` wouldn't work because the parameter is not a String, but a function that returns a String.

Also, `queryParam("latitude", Integer.parseInt("#{latitude}"))` wouldn't work either because `Integer.parseInt` would be called on the `"#{latitude}"` String before trying to pass the result to the `queryParam` method.

The solution here would be to pass a function:

`queryParam("latitude", session -> session.getInt("latitude"))`.
{{< /alert >}}

## Syntax

Gatling EL uses a `#{attributeName}` syntax to define placeholders to be replaced with the value of the matching *attributeName* attribute's value stored in the virtual user's Session, eg: `request("page").get("/foo?param=#{bar}")`.

```java
// direct attribute access
"#{foo}"

// access by index
// supports arrays, Java List, Scala Seq and Product
// n can be negative to count backward from the end
"#{foo(n)}"

// access by key
// supports Java Map, Java POJO, Java records, Scala Map and Scala case class
"#{foo.bar}"
```

{{< alert warning >}}
The previous `${}` syntax is deprecated because it was clashing with Scala and Kotlin String interpolation. It will be dropped in a future release.

Please make sure to use the `#{}` syntax from now on.
{{< /alert >}}

## Built-in Functions

Gatling EL provide the following built-in functions:

```java
// collection size
// supports arrays, Java List, Scala Seq and Product
"#{foo.size()}"

// collection size
// supports arrays, Java Collection, Java Map, Scala Iterable and Product
  
// collection random element
// supports arrays, Java Collection, Java List, Scala Seq and Product
"#{foo.random()}"
  
// true if the session contains a `foo` attribute
"#{foo.exists()}"
  
// true if the session doesn't contains a `foo` attribute
"#{foo.isUndefined()}"
  
// properly formats into a JSON value (wrap Strings with double quotes, deal with null)
"#{foo.jsonStringify()}"
  
// System.currentTimeMillis
"#{currentTimeMillis()}"

// ZonedDateTime.now() formatted with a java.time.format.DateTimeFormatter pattern
"#{currentDate(<pattern>)}"
  
// unescape an HTML String (entities decoded)
"#{foo.htmlUnescape()}"

// generate a random UUID (fast but cryptographically insecure)
"#{randomUuid()}"

// generate a random UUID (standard java.util.UUID#randomUUID: cryptographically secure, but slower)
"#{randomSecureUuid()}"

// generate a random Int with full range
"#{randomInt()}"

// generate a random Int, where the number generated is >= 5 and < 10 (the right bound of 10 is excluded)
"#{randomInt(5,10)}"

// generate a random Long with full range
"#{randomLong()}"

// generate a random Long, where the number generated is >= 2147483648 and < 2147483658 (the right bound is excluded)
"#{randomLong(2147483648,2147483658)}"

// The following functions generate a random Double value in a given range
// To use these functions you must adhere to the following formats and pay attention to the gotchas
// * a valid double string is of the format number.number, ex 0.34 or -12.34, while these are INVALID .34 or 2. or +0.34
//   must be of the format  -?\d+.\d+ (can start with a - then has digit(s) then a . then has digit(s))
// * when the double is converted to a string in the payload,
//   you may end up seeing doubles represented in scientific notation.
//   This can happen when you choose very small or very big numbers or when requesting many decimal places

// generate a random Double, where the number generated is >= -42.42 and < 42.42 (the right bound of 42.42 is excluded)
"#{randomDouble(-42.42,42.42)}"

// generate a random Double, similar to above, but limit to max of 3 decimal places
"#{randomDouble(-42.42,42.42,3)}"

// generate a random alphanumeric with length
"#{randomAlphanumeric(10)}"
```

You can combine different Gatling EL builtin functions, eg:

```java
// return first element of the first list in `foo`
"#{foo(0)(0)}"

// return a random element from the List associated with key `list` in the Map `foo`
"#{foo.list.random()}"
```

## Escaping

To prevent `"#{"` from being interpreted by the EL compiler, add a `\\` before it. `"\\#{foo}"` will be turned into `"#{foo}"`.

If you want a `$` before the placeholder, add another `$`.
Assuming the session attribute `foo` holds `"FOO"`, `"$$${foo}"` will be turned into `"$FOO"`.

This can go on and on. In general, if there are 2n-1 `$` characters before `${` -- an even number of `$` characters totally --
there will be n `$` before `{` in the final string;
if there are 2n `$` before `${` -- an odd number totally -- there will be n `$` before the placeholder.
