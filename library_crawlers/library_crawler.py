#!/usr/bin/python
#
# Scraper for libraries hosted at mvn central
# Retrieves jar|aar files along with some meta data
# @author erik derr [derr@cs.uni-saarland.de]
#

import json
from urllib.request import urlopen
from urllib.error import URLError, HTTPError
import urllib.error
import datetime
import os
import os.path
import errno
import zipfile
import traceback
from logger import logger
from xml.etree import ElementTree
import time
import sys
import shutil
import multiprocessing as mp

SLEEP_BETWEEN_TRIES = 5 # seconds
SLEEP_WHEN_SERVER_INAVAILABLE = 30
downloaded_since_reset = 0

JCENTER_REPO_URL = "http://jcenter.bintray.com"
MAVEN_REPO_URL = "http://search.maven.org/remotecontent?filepath="
GOOGLE_REPO = "https://dl.google.com/dl/android/maven2/"
libDescriptorFileName = "library.xml"




## functions ##

def unix2Date(unixTime):
    unixTime = int(str(unixTime)[:-3])
    return datetime.datetime.fromtimestamp(unixTime).strftime('%d.%m.%Y')


def make_sure_path_exists(path):
    try:
        os.makedirs(path)
    except OSError as exception:
        if exception.errno != errno.EEXIST:
            raise


def write_library_description(fileName, libName, category, version, date, comment, order):
    make_sure_path_exists(os.path.dirname(fileName))

    # write lib description in xml format
    with open(fileName, "w") as desc:
        desc.write("<?xml version=\"1.0\"?>\n")
        desc.write("<library>\n")
        desc.write("    <!-- library name -->\n")
        desc.write("    <name>{}</name>\n".format(libName))
        desc.write("\n")
        desc.write("    <!-- Advertising, Analytics, Android, SocialMedia, Cloud, Utilities -->\n")
        desc.write("    <category>{}</category>\n".format(category))
        desc.write("\n")
        desc.write("    <!-- optional: version string -->\n")
        desc.write("    <version>{}</version>\n".format(version))
        desc.write("\n")
        desc.write("    <!-- optional: date (format: dd.MM.yyyy  example: 21.05.2017) -->\n")
        desc.write("    <releasedate>{}</releasedate>\n".format(date))
        desc.write("\n")
        desc.write("    <!-- optional: order (to compare different versions) -->\n")
        desc.write("    <order>{}</order>\n".format(order))
        desc.write("\n")
        desc.write("    <!-- optional: comment -->\n")
        desc.write("    <comment>{}</comment>\n".format(comment))
        desc.write("</library>\n")


def downloadFileFromBuiltinRepo(targetDir, repoURL, groupid, artifactId, version, filetype):
    make_sure_path_exists(os.path.dirname(targetDir + "/"))

    # assemble download URL
    fileName = artifactId + "-" + version + "." + filetype
    URL = repoURL + "/" + groupid.replace(".","/") + "/" + artifactId.replace(".","/") + "/" + version + "/" + fileName

    # retrieve and save file
    targetFile = targetDir + "/" + fileName

    try:
        libFile = urlopen(URL)
        with open(targetFile,'wb') as output:
            output.write(libFile.read())
        logger.info("Downloaded {}".format(targetFile))
        return 0
    except HTTPError as e:
        if filetype != 'aar':
            logger.error('    !! HTTP Error while retrieving ' + filetype + ' file:  ' + str(e.code))
        return 1
    except URLError as e:
        logger.error('    !! URL Error while retrieving ' + filetype + ' file: ' + str(e.reason))
        return 1
    except Exception as excp:
        logger.error('    !! Download failed: ' + str(excp))
        return 1
def get_version_from_property(version_text, property_values):
    if version_text and version_text.startswith('${') and version_text.endswith('}'):
        version  = version_text[2:-1]
        if version in property_values:
            return property_values[version]
        else:
            return version_text
    else:
        return version_text

def summarize_pom_file(full_path):
    root = None
    try:
        root = ElementTree.parse(full_path).getroot()
    except Exception as error:
        logger.exception("file :{}".format(full_path))

    if root == None:
        return
    tagStr = root.tag
    namespace = ''
    if '}' in tagStr:
        namespace = tagStr[0:tagStr.index('}') + 1]

    property_values = {}

    project_version = root.find(namespace + 'version')

    if project_version is not None:
        property_values['project.version'] = project_version.text
    else:
        project_version = root.find(namespace + 'parent/'+namespace +'version')
        if project_version is not None:
            property_values['project.version'] = project_version.text

    properties = root.find(namespace + 'properties')

    # if 'com.google.truth__truth/0.41' in full_path:
    #   import pdb; pdb.set_trace()

    if properties is not None:
        properties_children = properties.getchildren()
        for _property in properties:
            property_name = _property.tag.replace(namespace,'')
            property_value = _property.text
            property_values[property_name] = property_value

    dependencies = root.findall(namespace + 'dependencies/' + namespace + 'dependency')
    pom_data = {'libs': [], 'deps': []}
    if len(dependencies) > 0:
        groupElement = root.find(namespace +'groupId')
        if groupElement == None:
            groupIdElement = root.find(namespace + 'parent/'+ namespace + 'groupId')
            if groupIdElement != None:
                group_id1 = groupIdElement.text
            else:
            #     # logger.error("group id 1 not found: {}".format(full_path))
                return
        else:
            group_id1 = groupElement.text


        versionElement = root.find(namespace + 'version')
        if versionElement == None:
            versionIdElement = root.find(namespace + 'parent/'+ namespace +'version')
            if versionIdElement != None:
                version1 = versionIdElement.text
            else:
                # logger.error("version 1 not found: {}".format(full_path))
                return

        else:
            version1 = versionElement.text
        version1 = get_version_from_property(version1, property_values)
        if '${' in version1:
            # logger.error("can not extract version1 :{} in {}".format(version1,full_path))
            return
        artifactElement = root.find(namespace +'artifactId')
        if artifactElement == None:
            artifactIdElement = root.find(namespace +'parent/' + namespace +'artifactId')
            if artifactIdElement != None:
                artifact_id1 = artifactIdElement.text
            else:
                # logger.error("artifact id 1 id not found: {}".format(full_path))
                return 
        else:
            artifact_id1 = artifactElement.text

        if '{' in group_id1 or '{' in artifact_id1:
            # logger.error(" group_id or artifact id contain $ {}".format(artifact_id1))
            return

        nameElement = root.find(namespace +'name')
        lib_name1 = None


        if nameElement != None:
            lib_name1 = nameElement.text
        key = group_id1 +"__" + artifact_id1
        if key in pom_data['libs']:            
            pom_data['libs'].append(key)        
        else:
            pom_data['libs']= [key]

        for dependency in dependencies:
            groupIdElement = dependency.find(namespace +'groupId')
            if groupIdElement == None:
                # logger.error("Group id not found: {}".format(full_path))
                continue
            else:
                groupId2 = groupIdElement.text
                artifactIdELement = dependency.find(namespace +'artifactId')
                if artifactIdELement != None:
                    artifactId2 = artifactIdELement.text
                else:
                    # logger.error("artifactId2 not found: {}".format(full_path))
                    continue

                versionElement = dependency.find(namespace +'version')
                version2 = None
                if '{' in groupId2 or '{' in artifactId2:
                    # logger.error("Group id or artifact_id contain $ {}".format(artifactId2))
                    continue

                if versionElement != None:
                    version2 = dependency.find(namespace +'version').text

                    key2 = groupId2 +"__" + artifactId2
                    key2 = groupId2 +"__" + artifactId2
                    if key2 in pom_data['libs']:            
                        pom_data['libs'].append(key2)        
                    else:
                        pom_data['libs']= [key2]        
                # else:
                    # logger.error("version 2 not found: {}".format(full_path))
                # # scope = dependency.find(namespace +'scope').text
                if version2:
                    version2 = get_version_from_property(version2, property_values)
                    if '${' in version2:
                        # logger.error("can not extract version2 :{} in {}".format(version2,full_path))
                        continue
                    lib1 = key +"__" + version1
                    lib2 = key2 +"__" + version2
                    dep_key = "{}>{}".format(lib1, lib2)
                    if dep_key in pom_data['deps']:
                        pom_data['deps'] = [dep_key]
                    else:
                        pom_data['deps'].append(dep_key)
                    
                            
    lib_summary_file = full_path.replace('.pom', '.json')
    with open(lib_summary_file, 'w') as json_summary:
        json.dump(pom_data, json_summary)
    logger.info("write dom summary to : {}".format(lib_summary_file))
def downloadFile(targetDir, groupid, artifactId, version, filetype):    
    result = -1
    global downloaded_since_reset
    make_sure_path_exists(os.path.dirname(targetDir + "/"))

    # assemble download URL
    
    artifactid_r = artifactId.replace(".","/")
    groupid_r = groupid.replace(".","/")
    URL = MAVEN_REPO_URL + groupid_r + "/" + artifactid_r + "/"

#    # sometimes it just returns the type "bundle", we then access the jar file
#    if filetype == "bundle":
#        filetype = "jar"
#    if filetype == "apklib":
#        filetype = "aar"

    fileName = artifactid_r + "-" + version + "." + filetype
    LIB_URL = URL + version + "/" + fileName  
    targetFile = targetDir + "/" + fileName
    # retrieve and save file
    pomFileName = artifactid_r + "-" + version + "." + "pom"
    pomURL = URL + version + "/" + pomFileName    
    targetPomFile = targetDir + "/" + pomFileName
    isPomExist  =  os.path.isfile(targetPomFile)

    if not isPomExist:        
        try:
            
            pomFile = urlopen(pomURL)        
            with open(targetPomFile,'wb') as pomOutput:
                pomOutput.write(pomFile.read())
            result = 0
            logger.info("Downloaded: {}".format(targetPomFile))
            downloaded_since_reset += 1
            summarize_pom_file(targetPomFile)

            
        except HTTPError as e:
            if filetype != 'pom':
                logger.error(pomURL)
                logger.error('!! HTTP Error while retrieving Pom file:  {}'.format(e.code))
                if e.code == 503:                    
                    logger.info("server is not available, wait for {} seconds".format(SLEEP_WHEN_SERVER_INAVAILABLE))
                    time.sleep(SLEEP_WHEN_SERVER_INAVAILABLE) #TODO may be keep trying with sleep
        except URLError as e:
            logger.error(pomURL)
            logger.info('Pom URL Error while retrieving pom file: {}\t{}'.format(pomURL, e.reason))
        except Exception as excp:        
            logger.error('Download failed: {}'.format(str(excp)))

    isLibFileExist = os.path.isfile(targetFile)

    if isLibFileExist:
        logger.info("Lib file already exists: {}".format(fileName))
    else:
        try:
            libFile = urlopen(LIB_URL)
            with open(targetFile,'wb') as output:
                output.write(libFile.read())
            logger.info("Downloaded: {}".format(targetFile))
            downloaded_since_reset += 1
            result = 0        
        except HTTPError as e:
            if filetype != 'aar':
                logger.error("HTTP Error while retrieving  filetype: {}\t error code: {}".format(filetype, e.code))
            if e.code == 503:
                logger.info("server is not available, wait for {} seconds".format(SLEEP_WHEN_SERVER_INAVAILABLE))
                time.sleep(SLEEP_WHEN_SERVER_INAVAILABLE) #TODO may be keep trying with sleep
            result = 1
        except URLError as e:
            logger.error("URL Error while retrieving filetype: {}\t error code: {}".format(filetype, e.reason))

            if e.code == 503:
                logger.info("server is not available, wait for {} seconds".format(SLEEP_WHEN_SERVER_INAVAILABLE))
                time.sleep(SLEEP_WHEN_SERVER_INAVAILABLE) #TODO may be keep trying with sleep
            result = 1
        except Exception as excp:
            logger.error("Download failed: {}".format(excp))
            result = 1
    if downloaded_since_reset > 20:
        logger.info("Sleep for 3 seconds after 20 downloads")
        time.sleep(3)
        downloaded_since_reset = 0
    return result


def deleteOldFormat(rootDir, libName):
    oldName = libName.replace("__", "::")
    oldPath = rootDir +"/" + oldName
    
    if os.path.isdir(oldPath):
        shutil.rmtree(oldPath)
        print("deleted: {}".format(oldPath))

def check_against_a_repo(rootDir, libName, category, comment, groupId, artifactId, repoURL):
    
    # Assemble base URL and retrieve meta data
    mvnURL = repoURL + "/" + groupId.replace(".","/") + "/" + artifactId.replace(".","/")
    metaURL = mvnURL + "/maven-metadata.xml"
    logger.info("{}__{} \tchecking specified repo:\n{}".format(groupId, artifactId, metaURL))    
    success = True
    try:
        

        response = urlopen(metaURL)
        data = response.read()
        response.close()
    except URLError as e:
        print('URLError = ' + str(e.reason))
        success = False
        return
    except Exception as excp:
        print('Could not retrieve meta data for ' + libName + '  [SKIP]  (' + str(excp) + ')')
        success = False
        return

    # retrieve available versions
    versions = []
    root = ElementTree.fromstring(data)
    for vg in root.find('versioning'):
        for v in vg.iter('version'):
            # if not skipAlphaBeta or (skipAlphaBeta and not '-alpha' in v.text and not '-beta' in v.text and not '-rc' in v.text and not '-dev' in v.text): 
            versions.append(v.text)

    numberOfVersions = len(versions)
    numberofDownloads = 0
    numberOfUpdates = 0
    if numberOfVersions > 0:
        for version in versions:
            # skip lib version if already existing
            libDir = rootDir + "/" + libName
            numberOfUpdates += 1
            order = numberOfUpdates
            if not os.path.isfile(libDir +  "/" + version + "/" + libDescriptorFileName):
                targetDir = libDir + "/" + version
                result = downloadFileFromBuiltinRepo(targetDir, repoURL, groupId, artifactId, version, "aar")

                if result == 1:
                    result = downloadFileFromBuiltinRepo(targetDir, repoURL, groupId, artifactId, version, "jar")

                if result == 0:
                    # write lib description
                    fileName = targetDir + "/" + "library.xml"
                    write_library_description(fileName, libName, category, version, "", comment, order)
                    numberofDownloads += 1

        if numberOfUpdates == 0:
            logger.info("all versions up-to-date")



        sleep_time =  float(numberofDownloads)/7
        logger.info("sleep for {}".format(sleep_time))
    else:
        success = False


def checkMavenCentral(rootDir, libName, category, comment,  groupId, artifactId):
    
    success = True
    if groupId == "" and artefactId == "":
        import pdb; pdb.set_trace()
    # Assemble mvn central search URL and retrieve meta data
    try:        
        mvnSearchURL = "http://search.maven.org/solrsearch/select?q=g:%22" + groupId + "%22+AND+a:%22" + artifactId + "%22&rows=100&core=gav"
        logger.info("{}__{}\tchecking maven central\n{}".format(groupId, artifactId,mvnSearchURL))
        response = urlopen(mvnSearchURL)
        data = json.loads(response.read().decode('utf-8'))
    except URLError as e:
        success = False
        logger.error('URLError = {}'.format(e.reason))
        return
    except Exception as excp:
        success = False
        logger.error('Could not retrieve meta data for {}, reason: {}\tskip!'.format(libName, excp))
        return

    # DEBUG: pretty print json
    #print json.dumps(data, indent=4, sort_keys=True)
    #print

    numberOfVersions = data["response"]["numFound"]    
    
    if numberOfVersions > 0:
        logger.info("retrieved meta data for {} versions".format(numberOfVersions))

        numberOfUpdates = 0
        numberofDownloads = 0
        for version in data["response"]["docs"]:
            order = numberOfVersions - numberOfUpdates
            numberOfUpdates += 1
            # skip lib version if already existing
            libDir = rootDir + "/" + libName
            if not os.path.isfile(libDir + "/" + version["v"] + "/" + libDescriptorFileName):
                date = unix2Date(version["timestamp"])
                targetDir = libDir + "/" + version["v"]
                logger.info("update version: {}   type: {}  date: {}  target-dir: {}".format(version["v"], version["p"], date, targetDir))

                # result = downloadFile(targetDir, groupId, artefactId, version["v"], version["p"])
                result = downloadFile(targetDir, groupId, artifactId, version["v"], "aar")                                
                if result == 1:
                    result = downloadFile(targetDir, groupId, artifactId, version["v"], "jar")

                if result == 0:
                    # write lib description
                    fileName = targetDir + "/" + "library.xml"
                    write_library_description(fileName, libName, category, version["v"], date, comment, order)
                    numberofDownloads += 1

        if numberofDownloads == 0:
            logger.info("all versions up-to-date")
        sleep_time =  float(numberofDownloads)/7
        logger.info("sleep for {}".format(sleep_time))
        time.sleep(sleep_time)
    else:
        #look in jcenter
        success = False
        
    return success

''' 1. check updatability attached repo (if available)
    2. if no attached repo available -> search from maven central 
    3. if not found in maven central -> search from jcenter
'''
def updateLibrary(rootDir, libName, category, comment,  groupId, artifactId, repo=None):
    # replace all blanks with dash
    libName = libName.strip().replace(" ", "_")
    libName = libName.strip().replace(":", "_")
    # logger.info("check library {} [{}] (g: {} a: {})".format(libName , category, groupId, artifactId))
    name_to_store = groupId + "__" + artifactId
    baseDirName = rootDir  + "/" + name_to_store + "/"
    deleteOldFormat(rootDir, name_to_store)
    dir = os.path.dirname(baseDirName)
    make_sure_path_exists(dir);
    ## 1. if repo is given, check updatability from the given repo
    if repo is not None and repo.strip() != "":    
        check_against_a_repo(rootDir, libName, category, comment, groupId, artifactId, repo)
    else:
        ## 2. if repo is not specified, search on maven central
        success = checkMavenCentral(rootDir, libName, category, comment, groupId, artifactId)
        if not success:
            ## 3. if the lib is not found in mvn central, search on jcenter
            repo = JCENTER_REPO_URL
            success = check_against_a_repo(rootDir, libName, category, comment, groupId, artifactId, repo)
            ## 4. if the lib is not found in jcenter, search on google maven
            if not success:
                repo = GOOGLE_REPO
                success = check_against_a_repo(rootDir, libName, category, comment, groupId, artifactId, repo)
                if not success:
                    print("Library {} is not found".format(libName))
            

def remove_empty_folders(rootDir, inputFile):
    with open(inputFile) as ifile:
        data = json.load(ifile)
    # remove empty library folders
    empty_libs = []
    for lib in os.listdir(rootDir):
        joinLibPath = os.path.join(rootDir, lib)
        if not os.listdir(joinLibPath):
            print("Remove empty folder {}".format(joinLibPath))
            os.rmdir(joinLibPath)
            empty_libs.append(lib)
    # remove library with no data from all-libraries.json
    for empty_lib in empty_libs:
        for lib in data["libraries"]:
            if "::" in lib["name"]:
                lib["name"] = lib["name"].replace("::", "__")
            if lib["name"] == empty_lib:
                data["libraries"].remove(lib)
    # insert back into all-libraries.json
    with open(inputFile, 'w') as ofile:
        json.dump(data, ofile)

def crawl(inputFile, rootDir):
    # setup the process pool for downloading multiple libraries in parallel
    # number_CPUs = mp.cpu_count()
    number_CPUs = 3
    pool = mp.Pool(processes=number_CPUs)
    jobs = []

    with open(inputFile) as ifile:
        data = json.load(ifile)

        # update each lib
        count_lib = 0
        total_lib = len(data["libraries"])
        for lib in data["libraries"]:
            count_lib += 1
            logger.info('check {}/{}'.format(count_lib, total_lib))

            groupId = lib["groupid"]
            artifactId = lib["artefactid"]

            category = "Undefined"
            if 'category' in lib and lib['category'] != "":
                category = lib['category']

            comment = ""
            if 'comment' in lib:
                comment = lib["comment"]
            name = groupId + "__"+ artifactId
            try:
                # instantiate process with list of download links for a specific plugin
                # create jobs
                jobs.append(pool.apply_async(updateLibrary, (rootDir, name, category, comment, groupId, artifactId,)))
            except Exception as e:
                logger.error('Library %s with error:\n %s' % (name, e.message))
                pass
            # updateLibrary(rootDir, name, category, comment, groupId, artifactId)
    # wait for all jobs to finish
    for job in jobs:
        job.get()

    # clean up
    pool.close()
    pool.join()

    #remove all empty folders
    remove_empty_folders(rootDir, inputFile)
    logger.info("Crawling time: {}".format(datetime.datetime.now() - current_time))
##  Main functionality ##
def main():
    current_time = datetime.datetime.now()
    # Requires one argument (path to json file with library descriptions)
    args = len(sys.argv)
    rootDir = None
    if args != 3:
        logger.error("Usage: " + sys.argv[0] + "  <libraries.json> <libs-folder>")
        sys.exit(1)
    else:
        inputFile = sys.argv[1]
        rootDir = sys.argv[2]
        if rootDir.endswith('/'):
            rootDir = rootDir[0:len(rootDir)-1]
        logger.info("Load libraries from {} and save new libs to {}".format(inputFile, rootDir))
    
    crawl(inputFile, rootDir)

if __name__ == "__main__":
    main()

