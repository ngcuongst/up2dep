# Up2Dep

Up2Dep facilitates the task of keeping your project's library dependencies up-to-date.<br>
In comparing to Lint and/or other tools, Up2Dep is better at the following points:<br>
  - Up2Dep not only provides you information about the latest version of a dependency but also gives more insight on the compatible version if the latest version is incompatible with your code.<br>
  - With Up2Dep you can easily navigate to your API dependency and  fix it with our API change prediction.<br>
  - Up2Dep respect your privacy, it DOES NOT track you

Please refer to our project's [website](https://project.cispa.io/up2dep/) for more information



# Build
To build Up2Dep you need to have [Gradle](https://gradle.org/) installed on your system. Gradle also requires Java JDK or JRE version 8 or higher.

To build Up2Dep, type command: gradle build
After successfully build Up2Dep, you will find the compiled file at: build/distributions/up2dep-2.3.0.zip

# Run
We have only tested Up2Dep with Android Studio version 3.5.1. As the API of Intellij IDEA (Androi Studio) that Up2Dep have to use changes frequently. We cannot guarantee that Up2Dep is compatible with newer versions of Android Studio.

To run Up2Dep you have 2 options:
- You can either install it from Android Studio: Android Studio -> Settings -> Plugins -> Install from disk (the little gear icon) -> browse to the file up2dep-2.3.0.zip at build/distributions
- You can directly run Up2Dep with an instance of Android Studio:
  - Provide the path to Android Studio app in the build.gradle file for example: alternativeIdePath '/Applications/Android Studio.app'
  - From Up2Dep source code folder, type command: gradle runIde


