# Gatling documentation

## Prerequisites

You need [Go](https://golang.org/doc/install) and [Hugo](https://gohugo.io/getting-started/installing/) installed.

Make sure to install the **extended** version of Hugo.

## Installation

To install or update the theme:

```
hugo mod get -u
hugo mod npm pack
npm install
hugo server
```

## Troubleshooting

### Invalid version: unknown revision

In case of issue such as:

```
go: github.com/gatling/gatling.io-doc-theme@v0.0.0-20240222160400-c0fbf7866574: invalid version: unknown revision c0fbf7866574
```

In the file `go.mod`, in the last line (with the `require` keyword), replace the hash with `main`:

```diff
 module github.com/gatling/gatling

 go 1.21

-require github.com/gatling/gatling.io-doc-theme v0.0.0-20240220083005-6f637476df1d // indirect
+require github.com/gatling/gatling.io-doc-theme main // indirect
```

Then, run `hugo mod get -u`.

### Template for shortcode "img" not found

In case of issue such as:

```
failed to extract shortcode: template for shortcode "img" not found
```

Run `hugo mod clean`.

### Update Hugo

If you continue to encounter errors, check your installation of Hugo is up-to-date and that you are using the
**extended** edition.

Check the [official documentation](https://gohugo.io/installation/) for all the details.
