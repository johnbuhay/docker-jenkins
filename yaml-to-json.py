#!/usr/bin/env python

import os
import simplejson
import sys
import yaml

if sys.argv:
    input = open(sys.argv[1], 'r')
else:
    input = str(sys.stdin.read())

print simplejson.dumps(yaml.load(input), sort_keys=True, indent=4 * ' ')
#python -c 'import simplejson,sys,yaml;print simplejson.dumps(yaml.load(str(sys.stdin.read())), sort_keys=True)'
