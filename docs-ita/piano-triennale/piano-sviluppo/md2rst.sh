#!/bin/bash

function stripMdExtension {
   echo $0
   newfile=$(echo "$1" | sed "s/.md//g")
   mv "$1" $newfile
   return 0
}

export -f stripMdExtension

if [ -d "./rst" ]; then
  rm -rf ./rst
fi

cp -R markdown/ rst

cd rst
find . -name \*.md -print |xargs -I@ pandoc @ -s -o @.rst
find . -name \*.md -print | xargs -I@ rm -f @

find . -name \*.md.rst -print | xargs -n 1 -P 10 -I {} bash -c 'stripMdExtension "$@"' _ {}
