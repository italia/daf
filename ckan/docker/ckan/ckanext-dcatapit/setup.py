from setuptools import setup, find_packages  # Always prefer setuptools over distutils
from codecs import open  # To use a consistent encoding
from os import path

here = path.abspath(path.dirname(__file__))

# Get the long description from the relevant file
with open(path.join(here, 'README.md'), encoding='utf-8') as f:
    long_description = f.read()

setup(
    name='''ckanext-dcatapit''',

    # Versions should comply with PEP440.  For a discussion on single-sourcing
    # the version across setup.py and the project code, see
    # http://packaging.python.org/en/latest/tutorial.html#version
    version='1.1.0a',

    description='''CKAN extension for the Italian Open Data Portals (DCAT_AP-IT).''',
    long_description=long_description,

    # The project's main homepage.
    url='https://github.com/geosolutions/ckanext-dcatapit',

    # Author details
    author='''Tobia Di Pisa''',
    author_email='''tobia.dipisa@geo-solutions.it''',

    # Choose your license
    license='AGPL',

    # See https://pypi.python.org/pypi?%3Aaction=list_classifiers
    classifiers=[
        # How mature is this project? Common values are
        # 3 - Alpha
        # 4 - Beta
        # 5 - Production/Stable
        'Development Status :: 3 - Alpha',

        # Pick your license as you wish (should match "license" above)
        'License :: OSI Approved :: GNU Affero General Public License v3 or later (AGPLv3+)',

        # Specify the Python versions you support here. In particular, ensure
        # that you indicate whether you support Python 2, Python 3 or both.
        'Programming Language :: Python :: 2.6',
        'Programming Language :: Python :: 2.7',
    ],

    # What does your project relate to?
    keywords='''CKAN DCAT DCAT_AP-IT RDF''',

    # You can just specify the packages manually here if your project is
    # simple. Or you can use find_packages().
    packages=find_packages(exclude=['contrib', 'docs', 'tests*']),
	
	namespace_packages=['ckanext', 'ckanext.dcatapit'],

    # List run-time dependencies here.  These will be installed by pip when your
    # project is installed. For an analysis of "install_requires" vs pip's
    # requirements files see:
    # https://packaging.python.org/en/latest/technical.html#install-requires-vs-requirements-files
    install_requires=[],

    # If there are data files included in your packages that need to be
    # installed, specify them here.  If using Python 2.6 or less, then these
    # have to be included in MANIFEST.in as well.
    include_package_data=True,
    package_data={
    },

    # Although 'package_data' is the preferred approach, in some case you may
    # need to place data files outside of your packages.
    # see http://docs.python.org/3.4/distutils/setupscript.html#installing-additional-files
    # In this case, 'data_file' will be installed into '<sys.prefix>/my_data'
    data_files=[],

    # To provide executable scripts, use entry points in preference to the
    # "scripts" keyword. Entry points provide cross-platform support and allow
    # pip to create the appropriate form of executable for the target platform.
    entry_points='''
        [ckan.plugins]
        dcatapit_pkg=ckanext.dcatapit.plugin:DCATAPITPackagePlugin
        dcatapit_org=ckanext.dcatapit.plugin:DCATAPITOrganizationPlugin        
        dcatapit_config=ckanext.dcatapit.plugin:DCATAPITConfigurerPlugin
        dcatapit_harvester=ckanext.dcatapit.dcat.harvester:DCATAPITHarvesterPlugin
        dcatapit_csw_harvester=ckanext.dcatapit.harvesters.csw_harvester:DCATAPITCSWHarvester
		
        [ckan.rdf.profiles]
        it_dcat_ap=ckanext.dcatapit.dcat.profiles:ItalianDCATAPProfile

        [paste.paster_command]
        vocabulary=ckanext.dcatapit.commands.dcatapit:DCATAPITCommands

        [babel.extractors]
        ckan = ckan.lib.extract:extract_ckan
    ''',

    # DCAT-AP_IT Translations
    message_extractors={
        'ckanext': [
            ('**.py', 'python', None),
            ('**.js', 'javascript', None),
            ('**/templates/**.html', 'ckan', None),
        ],
    }

)
