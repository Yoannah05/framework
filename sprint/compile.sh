#!/bin/bash
CLASSES_DIR="bin"
JAR_FILE="sprint.jar"

# Create the JAR file
jar cvf "$JAR_FILE" -C "$CLASSES_DIR" .

# Copy the JAR file to the target directory
cp -v "$JAR_FILE" "/home/yoannah/Documents/ITU/framework/sprint-test/lib"

# Remove the JAR file
rm "$JAR_FILE"
