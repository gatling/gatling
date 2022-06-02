---
title: "Expression Language"
description: "Expression Language is a simple templating engine used to write dynamic values by fetching values from the Session"
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
  
// new java.util.Date() formatted with a java.text.SimpleDateFormat pattern
"#{currentDate(<pattern>)}"
  
// unescape an HTML String (entities decoded)
"#{foo.htmlUnescape()}"
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
