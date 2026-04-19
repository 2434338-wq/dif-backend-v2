#!/usr/bin/env sh
##############################################################################
# Gradle start up script for UN*X
##############################################################################
set -e
APP_HOME=$(dirname "$(readlink -f "$0" 2>/dev/null || echo "$0")")
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
JAVA_EXE="${JAVA_HOME:-}/bin/java"
if [ ! -x "$JAVA_EXE" ]; then
    JAVA_EXE="java"
fi
exec "$JAVA_EXE" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
