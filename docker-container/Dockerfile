FROM ubuntu:20.04
ARG root_passwd=up2dep
ARG zip_file=gradle-5.1.1-bin.zip
ARG gradle_url=https://downloads.gradle.org/distributions/${zip_file}

ARG username=up2dep_dev
ARG userhome=/home/${username}
ARG userid=1000
RUN useradd --create-home --shell /bin/bash --uid ${userid} ${username} && \
        mkdir --mode 755 ${userhome}/.ssh && \
        chown ${username}:${username} ${userhome}/.ssh

RUN apt-get update && apt-get install -y --no-install-recommends \
        coreutils \
        git \
        wget \
        ca-certificates

RUN mkdir --mode 775 ${userhome}/downloads 
# download gradle
RUN wget --directory-prefix ${userhome}/downloads/  ${gradle_url} 

# install unzip
RUN apt-get install zip unzip

# unzip gradle
RUN unzip ${userhome}/downloads/${zip_file} -d ${userhome}/downloads/

# install java
RUN apt-get update && \    
    apt-get -y install openjdk-11-jdk-headless && \
    apt-get clean

# clone up2dep source code
RUN cd ${userhome} && \
        git clone https://github.com/ngcuongst/up2dep.git

# build up2dep using gradle build
RUN cd ${userhome}/up2dep && \
        ../downloads/gradle-5.1.1/bin/gradle build

