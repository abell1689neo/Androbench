#!/bin/sh
# AUTO-GENERATED FILE, DO NOT EDIT!
if [ -f $1.org ]; then
  sed -e 's!^C:/cygwin64/lib!/usr/lib!ig;s! C:/cygwin64/lib! /usr/lib!ig;s!^C:/cygwin64/bin!/usr/bin!ig;s! C:/cygwin64/bin! /usr/bin!ig;s!^C:/cygwin64/!/!ig;s! C:/cygwin64/! /!ig;s!^Z:!/cygdrive/z!ig;s! Z:! /cygdrive/z!ig;s!^Y:!/cygdrive/y!ig;s! Y:! /cygdrive/y!ig;s!^X:!/cygdrive/x!ig;s! X:! /cygdrive/x!ig;s!^W:!/cygdrive/w!ig;s! W:! /cygdrive/w!ig;s!^V:!/cygdrive/v!ig;s! V:! /cygdrive/v!ig;s!^U:!/cygdrive/u!ig;s! U:! /cygdrive/u!ig;s!^T:!/cygdrive/t!ig;s! T:! /cygdrive/t!ig;s!^E:!/cygdrive/e!ig;s! E:! /cygdrive/e!ig;s!^D:!/cygdrive/d!ig;s! D:! /cygdrive/d!ig;s!^C:!/cygdrive/c!ig;s! C:! /cygdrive/c!ig;' $1.org > $1 && rm -f $1.org
fi
