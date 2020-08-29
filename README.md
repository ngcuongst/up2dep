# Up2Dep

Up2Dep is Android Studio plugin that facilitates the task of keeping your project's library dependencies up-to-date.<br>

If you have any questions, please contact me at:

Duc Cuong Nguyen

duc.nguyen[at]cispa.saarland


# Build
We use [Gradle](https://gradle.org/) to build Up2Dep source code. Gradle also requires Java JDK or JRE version 8 or higher. Please make sure you have the required Java JDK/JRE version.

To build Up2Dep, run the script: ./build.sh

After successfully build Up2Dep, you will find the compiled file at: build/distributions/up2dep-2.3.0.zip

# Run
We have only tested Up2Dep with Android Studio version 3.5.1. As the set of API of Intellij IDEA (Androi Studio) that Up2Dep depends on changes frequently. We cannot guarantee that Up2Dep is compatible with newer versions of Android Studio.

To run Up2Dep you have 2 options:
- You can either install it from Android Studio: Android Studio -> Settings -> Plugins -> Install from disk (the little gear icon) -> browse to the file up2dep-2.3.0.zip at build/distributions
- You can directly run Up2Dep with an instance of Android Studio:
  - Provide the path to Android Studio app in the build.gradle file for example: alternativeIdePath '/Applications/Android Studio.app'
  - From Up2Dep source code folder, type command: gradle runIde


# Library database
Currently, when you run Up2Dep, it will download the library database from our web server. However, we also provide this data base in sqlite_data folder. Please bear in mind that, both (database from our web service and the data inside sqlite_data folder) are a snapshot or our database in January 2019 which we used for the study/evaluation in our paper, and are not the newest version. 

In the database, you will find 1852 sqlite files, of which the main.sql file contains general information about all libraries, and the remaining 1851 files contain detailed information of 1851 libraries (updability, API availability, vulnerability, cryptographic API misuse).

