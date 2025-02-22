#!/bin/bash
CLASSES_DIR="bin"
LIB_DIR="lib"
JAR_FILE="sprint.jar"
TARGET_DIR="/home/yoannah/Documents/ITU/framework/sprint-test/lib"

# Create the JAR file
jar cvf "$JAR_FILE" -C "$CLASSES_DIR" .

# Check if the JAR creation was successful
if [ $? -ne 0 ]; then
  echo "Failed to create JAR file."
  exit 1
fi

# Copy the JAR file to the lib directory
cp -v "$JAR_FILE" "$LIB_DIR"
if [ $? -ne 0 ]; then
  echo "Failed to copy JAR to lib directory."
  exit 1
fi

# Copy all JAR files from the lib directory to the target lib directory
cp -v "$LIB_DIR"/*.jar "$TARGET_DIR"
if [ $? -ne 0 ]; then
  echo "Failed to copy JAR files to target directory."
  exit 1
fi

# Remove the JAR file from the local lib directory after copying
rm -v "$JAR_FILE"
if [ $? -ne 0 ]; then
  echo "Failed to remove JAR file."
  exit 1
fi

echo "JAR file created, copied, and cleaned up successfully!"
