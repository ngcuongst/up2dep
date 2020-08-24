#download gradle-5.1.1

zip_file=gradle-5.1.1-bin.zip
folder_name=gradle-5.1.1

if ! [ -f $zip_file ]; then     
	echo "Downloading Gradle ..."
	curl https://downloads.gradle.org/distributions/$zip_file --output $zip_file
fi

#unzip
if ! [ -d $folder_name ]; then
	echo "Unzip Gradle ..."
	unzip $zip_file
fi

#clean and build
gradle-5.1.1/bin/gradle build

exit