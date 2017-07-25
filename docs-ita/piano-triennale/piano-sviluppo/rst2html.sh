#!/bin/bash

if [ -d "./docs" ]; then
  rm -rf ./docs
fi

mkdir ./docs

sphinx-build ./rst ./docs
