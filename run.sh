#!/bin/bash
# Script para ejecutar DarkNote Desktop

cd /home/dark/Project/darknote
export JAVA_HOME=/opt/android-studio/jbr
export PATH=$JAVA_HOME/bin:$PATH

./gradlew :apps:desktop:run