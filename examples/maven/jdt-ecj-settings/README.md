The `org.eclipse.jdt.core.prefs` file in this folder enables stricter checks of java code, especially in the area of null analysis.

It should either be manually placed in the `.settings` directory of each eclipse project for which these checks should be applied,
or you can use the https://github.com/lastnpe/eclipse-external-annotations-m2e-plugin which can do this automatically for you,
if appropriately configured to do so (the example projects in .. are so configured).
