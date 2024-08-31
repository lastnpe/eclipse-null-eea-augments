# Versions & Backwards Compatibility

This project "versions" external nullness annotations for public & protected API surfaces of common Java libraries.

In some cases we maintain both older & newer versions; e.g. for the JDK, or the Servlet API in `javax` and `jakarta` packages, etc.

In other cases in our next major version release we may remove old versions of libararies which we believe to longer be used much.

Of course, you can always use already stable past versions released to Maven central, even if we've cleaned them out at source.

Contributions for additional versions which you need in your projects are always welcome.
