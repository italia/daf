#ckan + ckanext-harvest + ckanext-dcat + ckanext-dcatapit
FROM ubuntu:16.04
MAINTAINER Open

# Vars
ENV CKAN_HOME /usr/lib/ckan/default
ENV CKAN_CONFIG /etc/ckan/default
ENV CKAN_STORAGE_PATH /var/lib/ckan
ENV CKAN_SITE_URL http://localhost:5000

# Install CKAN required packages
RUN apt-get -q -y update && apt-get -q -y upgrade && DEBIAN_FRONTEND=noninteractive apt-get -q -y install \
	python-dev \
        python-pip \
        python-virtualenv \
        libpq-dev \
        git-core \
	sudo \
	&& apt-get -q clean


# Ensure that we always use UTF-8 and with US English locale
RUN apt-get -qq update && \
    apt-get -q -y upgrade && \
    apt-get install -y sudo curl wget locales && \
    rm -rf /var/lib/apt/lists/*

# Ensure that we always use UTF-8 and with Canadian English locale
RUN locale-gen en_US.UTF-8
COPY ./default_locale /etc/default/locale
RUN chmod 0755 /etc/default/locale

ENV LC_ALL=en_US.UTF-8
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US.UTF-8

# Upgrade python pip
RUN pip install --upgrade pip

# SetUp ckan Directory
RUN mkdir -p $CKAN_HOME
RUN mkdir -p $CKAN_CONFIG 
RUN mkdir -p $CKAN_STORAGE_PATH

# Create virtualenv
RUN virtualenv --no-site-packages $CKAN_HOME

# Install ckan
RUN pip install setuptools==20.4
RUN mkdir -p $CKAN_HOME/src/ckan/ 
ADD ./ckan/ $CKAN_HOME/src/ckan/ 
RUN . /usr/lib/ckan/default/bin/activate 
RUN pip install -e $CKAN_HOME/src/ckan/ 
RUN pip install -r $CKAN_HOME/src/ckan/requirements.txt 

# TMP-BUGFIX https://github.com/ckan/ckan/issues/3594 
RUN pip install --upgrade urllib3
RUN . /usr/lib/ckan/default/bin/activate 

# Add ckan.ini configuration file
ADD ./ckan.ini $CKAN_CONFIG
RUN chmod 777 $CKAN_CONFIG/ckan.ini

# Link to who.ini
RUN ln -s $CKAN_HOME/src/ckan/ckan/config/who.ini $CKAN_CONFIG/who.ini

# Install ckanext-harvest
RUN mkdir $CKAN_HOME/src/ckanext-harvest/
ADD ./ckanext-harvest/ $CKAN_HOME/src/ckanext-harvest/
RUN . /usr/lib/ckan/default/bin/activate  
RUN pip install -e $CKAN_HOME/src/ckanext-harvest/ 
RUN pip install -r $CKAN_HOME/src/ckanext-harvest/pip-requirements.txt 
RUN pip install -r $CKAN_HOME/src/ckanext-harvest/dev-requirements.txt 

# Install ckanext-dcat
RUN mkdir $CKAN_HOME/src/ckanext-dcat/
ADD ./ckanext-dcat/ $CKAN_HOME/src/ckanext-dcat/
RUN . /usr/lib/ckan/default/bin/activate 
RUN pip install -e $CKAN_HOME/src/ckanext-dcat/ 
RUN pip install -r $CKAN_HOME/src/ckanext-dcat/requirements.txt

# Install ckanext-dcatapit
RUN mkdir $CKAN_HOME/src/ckanext-dcatapit/
ADD ./ckanext-dcatapit/ $CKAN_HOME/src/ckanext-dcatapit/
RUN . /usr/lib/ckan/default/bin/activate 
RUN pip install -e $CKAN_HOME/src/ckanext-dcatapit/ 

#RUN pip install -r $CKAN_HOME/src/ckanext-dcatapit/dev-requirements.txt
RUN . /usr/lib/ckan/default/bin/activate

CMD ["paster","serve","/etc/ckan/default/ckan.ini"]

# SetUp EntryPoint 
COPY ./ckan-entrypoint.sh /
RUN chmod +x /ckan-entrypoint.sh
ENTRYPOINT ["/ckan-entrypoint.sh"]

#TEST BASH
ADD ./ckan-init.sh /
RUN chmod +x /ckan-init.sh

# Volumes 
VOLUME ["/etc/ckan/default"]
VOLUME ["/var/lib/ckan"]
EXPOSE 5000
CMD ["paster","serve","/etc/ckan/default/ckan.ini"]
