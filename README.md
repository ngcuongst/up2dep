# Up2Dep

Up2Dep is Android Studio plugin that facilitates the task of keeping your project's library dependencies up-to-date.<br>
Up2Dep applies [LibScout](https://github.com/reddr/LibScout) to analyze the API changes across all versions of a library, and the [CryptoAnalysis](https://github.com/CROSSINGTUD/CryptoAnalysis) component of CogniCrypt to identify cryptographic API misuse in a library. The results are integrated inside Up2Dep and delivered to developers inside Android Studio.

If you have any questions, please contact me at:

Duc Cuong Nguyen

duc.nguyen[at]cispa.saarland


# Build

This process will take around 30-45 minutes depending on your network connection because the following components will be downloaded and built:

- IntelliJ-Idea version 2019.1.4
- Gradle 
- Zip 

There are two options to build Up2Dep, we highly recommend you using the first one (container):

###	Using the provided docker container:

The [container](https://github.com/ngcuongst/up2dep/tree/master/docker-container) is based on the Ubuntu 20.04 image. Docker is required to build up2dep container. Once the container is successfully built. You can run the image, and find the compiled version of Up2Dep at /home/up2dep_dev/up2dep/build/distributions/up2dep-2.3.0.zip. You can copy this file and install Up2Dep as a plugin into your Android Studio (see `Run` Section) 

###	Manually build (tested on Mac OS Catalina, and Ubuntu 20.04):

  If you would like to build Up2Dep on your own computer, we provided the script build.sh to build Up2Dep. The following dependencies are needed:
  o	Java JDK or JRE version 8 or higher. 
  o	cURL
  
  To build Up2Dep, please run the [build.sh](https://github.com/ngcuongst/up2dep/tree/master/build.sh) script 
  
  Once, the source code is successfully built, the compiled file will be located at build/distributions/up2dep-2.3.0.zip. You can copy this file and install Up2Dep as a plugin into your Android Studio (see Section 3)


# Run
Up2Dep should work on all major OSes (Mac OS, Ubuntu, Windows). Yet, we highly recommend you to test it with Mac OS. 

There are 2 options to run Up2Dep:

### Directly install from Android Studio: 
Android Studio -> Settings/Preferences -> Plugins -> Install from disk (the little gear icon) -> browse to the file up2dep-2.3.0.zip at build/distributions

###	Run Up2Dep with gradle: 
You can run an instance of Android Studio with Up2Dep installed by specifying the path to Android Studio in build.gradle. 
For example:
alternativeIdePath '/Applications/Android Studio.app'

And run the following command in Up2Dep directory: 
```console
gradle runIde
```

#### Exemplary project
To see how Up2Dep works with Android projects, you can import the project [Up2DepExemplary](https://github.com/ngcuongst/up2dep/tree/master/sample_projects). Once the project is imported, you can open the build.gradle file of the "app" module where warnings of outdated libraries are shown (with quick-fixes options). To enable a quick-fix you can click on the bubble icon, or simply use the [alt] + [Enter] shortcut (e.g., on Mac OS).

# For those who'd like to dig deeper 
## Library database
Currently, when you run Up2Dep, it will download the library database from our web server. However, we also provide this database in [sqlite_data](https://github.com/ngcuongst/up2dep/tree/master/sqlite_data) folder. Please bear in mind that, both (database from our web service and the data inside sqlite_data folder) are a snapshot or our database in January 2019 which we used for the study/evaluation in our paper, and are not the newest version. Up2Dep uses this database internally, however, if you would like to check the database, you should have sqlite installed on your machine.

In the database, you will find 1852 sqlite files, of which the main.sql file contains general information about all libraries, and the remaining 1851 files contain detailed information of 1851 libraries (updability, API availability, vulnerability, cryptographic API misuse).

## Library crawlers
We also provide here the crawlers, so you can also download the libraries (and their versions) which we had used [LibScout](https://github.com/reddr/LibScout) and [CryptoAnalysis](https://github.com/CROSSINGTUD/CryptoAnalysis) component of Cognicrypt to analyze and to build the aforementioned database.

# Scientific Publication
For technical details and evaluation results, please refer to our publication:
[Up2Dep: Android Tool Support to Fix Insecure Code Dependencies](https://cispa.de/en/research/publications/3324-up2dep-android-tool-support-to-fix-insecure-code-dependencies)

If you use Up2Dep in a scientific publication, we would appreciate citations using the Bibtex entry [up2dep.bib](https://github.com/ngcuongst/up2dep/tree/master/publication/up2dep.bib)
