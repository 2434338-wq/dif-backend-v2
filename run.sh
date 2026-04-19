#!/bin/sh
JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home"
GRADLE="$HOME/Downloads/gradle-8.10/bin/gradle"
echo "Iniciando DIF Backend..."
JAVA_HOME="$JAVA_HOME" "$GRADLE" clean run
