```
hugo mod get -u
hugo mod npm pack
npm install
hugo server
```

In case of issue such as `failed to extract shortcode: template for shortcode "img" not found`, run `hugo mod clean`
